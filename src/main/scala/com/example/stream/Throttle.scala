package com.example.stream

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.example.stream.ThrottleActor.{Abort, Start}
import com.example.utils.AkkaApp

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ThrottleActor {
  case object Start
  case object Abort
}

class ThrottleActor(sleep: FiniteDuration, blockingDispatcher: ExecutionContext, materializer: Materializer) extends Actor with ActorLogging {
  val THROTTLE = 10

  override def receive: Receive = {
    case Start =>
      log.info("# Start")
      Source(0 to 50)
        .zipWithIndex
        .grouped(3)
        .mapAsync(1) { ids =>
          Future {
            //Thread.sleep(sleep.toMillis)
            log.info(s"Access DB ids=${ids}")
            ids
          }(blockingDispatcher)
        }
        .mapConcat(identity)
        .throttle(20, 5 seconds)
        .runForeach { ids =>
          //throw new RuntimeException("ERROR")
          log.info(s"Send Message ids=${ids}")
        }(materializer)
        .onComplete {
          case Success(v) =>
            log.info("Success")
          case Failure(e) =>
            log.info(s"Failure e=${e.getMessage}")
        }(blockingDispatcher)
    case Abort =>
  }
}

object Throttle extends AkkaApp {

  val blockingDispatcher: ExecutionContext = system.dispatchers.lookup("blocking-thread-pool-dispatcher")
  val actor = system.actorOf(Props(classOf[ThrottleActor], /*sleep : */ 10 seconds, blockingDispatcher, mat), "ThrottleActor")
  actor ! Start
  Thread.sleep(2000)
  actor ! Start
}
