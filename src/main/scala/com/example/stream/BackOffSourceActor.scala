package com.example.stream

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Source}
import com.example.stream.BackOffActor.{Result, Start}

import scala.concurrent.Future
import scala.concurrent.duration._

object BackOffActor {
  case class Start(name: String, occurError: Boolean, refTo: ActorRef)
  case class Result(name: String, nums: List[Int])
}

class BackOffActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher
  var count = new AtomicInteger(0)

  /*
   * duration이 최소 minBackoff ~ maxBackoff이 될거라는 의미.
   * val calculatedDuration : FiniteDuration = Try(maxBackoff.min(minBackoff * math.pow(2, restartCount)) * rnd).getOrElse(maxBackoff)
   */
  override def receive: Receive = {
    case Start(name, occurError, refTo) =>
      RestartSource.withBackoff(
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
        .runFold(List[Int]()) { case (acc, b) => b :: acc  }
        .map { nums =>
          println(s"# Execute map : ${name}")
          refTo ! Result(name, nums)
        }
  }
}

class BackOffReceiverActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher

  override def receive: Receive = {
    case Result(name, nums) =>
      println(s"# Success name : ${name}, nums : ${nums}")
  }
}

object BackOffSourceApp extends App {
  val system = ActorSystem("ActorSystem")
  implicit val ec = system.dispatcher
  implicit val mat = Materializer(system)

  val backOffActor = system.actorOf(Props[BackOffActor], "BackOffActor")
  val backOffReceiverActor = system.actorOf(Props[BackOffReceiverActor], "BackOffReceiverActor")
  val timeout = 3.seconds

  // 실패, 완료 둘 다 retry를 함
  /**
    * # Execute Future : #1
    * # Execute Future : #1
    * # Execute Future : #1
    * # Execute Future : #1
    * # Execute map : #1
    * # Success name : #1, nums : List(1, 1, 1, 1)
    */
  backOffActor ! Start("#1", occurError = false, backOffReceiverActor)
  /**
    * # Execute Future : #2
    * # java.lang.RuntimeException: # Error Occured 0
    * # Execute Future : #2
    * # java.lang.RuntimeException: # Error Occured 1
    * # Execute Future : #2
    * # java.lang.RuntimeException: # Error Occured 2
    * # Execute Future : #2
    */
  backOffActor ! Start("#2", occurError = true, backOffReceiverActor)
}