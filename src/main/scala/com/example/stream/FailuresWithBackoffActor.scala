package com.example.stream

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Source}
import com.example.stream.FailuresWithBackoffActor.{Result, Start}
import com.example.utils.AkkaApp

import scala.concurrent.Future
import scala.concurrent.duration._

object FailuresWithBackoffActor {

  case class Start(name: String,
                   occurError: Boolean,
                   refTo: ActorRef)

  case class Result(name: String,
                    nums: List[Int])

}

class FailuresWithBackoffActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher
  var count = new AtomicInteger(0)

  override def receive: Receive = {
    case Start(name, occurError, refTo) =>
      RestartSource.onFailuresWithBackoff(
        minBackoff = 3.seconds,
        maxBackoff = 10.seconds,
        randomFactor = 0.2,
        maxRestarts = 3
      ) { () =>
        Source.futureSource {
          Future {
            println(s"# Execute Future : ${name}")
            if (occurError) {
              throw new RuntimeException(s"# Error Occured ${count.getAndIncrement()}")
            } else {
              Source.single(1)
            }
          }
        }
      }
        .log("# Final error logging")
        .runFold(List[Int]()) { case (acc, b) => b :: acc  }
        .map { nums =>
          println(s"# Execute map : ${name}")
          refTo ! Result(name, nums)
        }
  }
}

class FailuresWithBackoffReceiverActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher

  override def receive: Receive = {
    case Result(name, nums) =>
      println(s"# Success name : ${name}, nums : ${nums}")
  }
}

object FailuresWithBackoffApp extends AkkaApp {

  val failuresWithBackoffActor = system.actorOf(Props[FailuresWithBackoffActor], "FailuresWithBackoffActor")
  val receiverActor = system.actorOf(Props[FailuresWithBackoffReceiverActor], "FailuresWithBackoffReceiverActor")

  // 실패시에만 retry를 함
  /**
    * # Execute Future : #1
    * # Execute map : #1
    * # Success name : #1, nums : List(1)
    */
  //failuresWithBackoffActor ! Start("#1", occurError = false, receiverActor)
  /**
    * # Execute Future : #2
    * java.lang.RuntimeException: # Error Occured 0
    * # Execute Future : #2
    * java.lang.RuntimeException: # Error Occured 1
    * # Execute Future : #2
    * java.lang.RuntimeException: # Error Occured 2
    * # Execute Future : #2
    * [ERROR] [# Final error logging] Upstream failed.
    * java.lang.RuntimeException: # Error Occured 3
    */
  failuresWithBackoffActor ! Start("#2", occurError = true, receiverActor)
}