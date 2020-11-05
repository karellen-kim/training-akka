package com.example.stream

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.example.stream.RecoverRetryActor.{Result, Start}
import com.example.utils.AkkaApp

import scala.concurrent.Future

object RecoverRetryActor {

  case class Start(name: String,
                   occurError: Boolean,
                   refTo: ActorRef)

  case class Result(name: String,
                    num: Option[Int])

}

class RecoverRetryActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher
  var count = new AtomicInteger(0)
  var recoverCount = new AtomicInteger(0)

  override def receive: Receive = {
    case Start(name, occurError, refTo) =>
      Source.futureSource {
        Future {
          println(s"# Execute Future : ${name}")
          if (occurError) {
            throw new RuntimeException(s"# Error Occured ${count.getAndIncrement()}")
          } else {
            Source.single(Some(1))
          }
        }
      }
      .recoverWithRetries(attempts = 5, {
        case e : RuntimeException =>
          println(s"# Recover : ${name}")
          Source.single(throw new RuntimeException(s"# Retry Error Occured"))
      })
      .runForeach(num => refTo ! Result(name, num))
  }
}

class RecoverRetryReceiverActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher

  override def receive: Receive = {
    case Result(name, num) =>
      println(s"# Success name : ${name}, num : ${num}")
  }
}

object RetryApp extends AkkaApp {
  val actor = system.actorOf(Props[RecoverRetryActor], "RecoverRetryActor")
  val receiver = system.actorOf(Props[RecoverRetryReceiverActor], "RecoverRetryReceiverActor")

  /**
    * # Execute Future : #1
    * # Success name : #1, num : Some(1)
    */
  actor ! Start("#1", false, receiver)

  /**
    * # Execute Future : #2
    * # Recover : #2
    * # Error Occured 0
    * # Success name : #2, num : None
    */
  actor ! Start("#2", true, receiver)
}