package com.example.classic.nonblocking

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import com.example.utils.CountableThreadPoolHelper
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

case class Message(msg: String)
case class AnotherMessage(msg: String)

class NonBlockingActor extends Actor with ActorLogging {
  import context.dispatcher

  override def receive: Receive = {
    case Message(msg) =>
      log.debug(s"Start : ${msg}")
      Blocking.run(msg) pipeTo sender()
    case AnotherMessage(msg) =>
      log.debug(s"Start : ${msg}")
      Blocking.run(msg) pipeTo sender()
  }
}

object Blocking {
  val log = LoggerFactory.getLogger(this.getClass)
  final val blockingPoolCounter = new AtomicInteger(1)
  implicit val blockingPool = CountableThreadPoolHelper("blockingPool", 3, 5, blockingPoolCounter)

  def run(str: String): Future[String] = Future {
    log.debug(s"Run : ${str}")
    Thread.sleep(5000) // 5초
    s"End : ${str}"
  }
}

object NonBlockingActorApp extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("ClassicActorSystem")
  final val commonPoolCounter = new AtomicInteger(1)
  implicit val commonPool = CountableThreadPoolHelper("commonPool", 3, 5, commonPoolCounter)

  val nonBlockingActor = system.actorOf(Props[NonBlockingActor], "NonBlockingActor")

  (nonBlockingActor ? Message("#1"))(10.seconds).map { res =>
    log.debug(res.toString)
  }
  (nonBlockingActor ? Message("#2"))(10.seconds).map { res =>
    log.debug(res.toString)
  }
  (nonBlockingActor ? AnotherMessage("#3"))(10.seconds).map { res =>
    log.debug(res.toString)
  }
  (nonBlockingActor ? AnotherMessage("#4"))(10.seconds).map { res =>
    log.debug(res.toString)
  }
}