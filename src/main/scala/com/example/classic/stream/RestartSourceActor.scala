package com.example.classic.stream

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Source}
import com.example.classic.stream.RestartSourceActor.Start

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

object RestartSourceActor {
  case object Start
}

class RestartSourceActor extends Actor with ActorLogging {
  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher
  var count = new AtomicInteger(0)

  /*
   * duration이 최소 minBackoff ~ maxBackoff이 될거라는 의미.
   * val calculatedDuration : FiniteDuration = Try(maxBackoff.min(minBackoff * math.pow(2, restartCount)) * rnd).getOrElse(maxBackoff)
   */
  override def receive: Receive = {
    case Start =>
      val restartSource = RestartSource.withBackoff(
        minBackoff = 3.seconds,
        maxBackoff = 10.seconds,
        randomFactor = 0.2,
        maxRestarts = 3
      ) { () =>
        Source.futureSource {
          Future {
            throw new RuntimeException(s"# Error Occured ${count.getAndIncrement()}")
          }
        }
      }
      restartSource
        .runFold(List[Int]()) { case (acc, b) => b :: acc  }
        .map(nums => println(nums))
  }
}

object RestartSourceActorApp extends App {
  val system = ActorSystem("ActorSystem")
  implicit val ec = system.dispatcher
  implicit val mat = Materializer(system)

  val restartSourceActor = system.actorOf(Props[RestartSourceActor], "RestartSourceActor")

  restartSourceActor ! Start
}