package com.example.classic.basic

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait Result
case object Ok extends Result
case object Error extends Result

case object Tell
case class Ask(expectedResult: Result)

class AskActor extends Actor with ActorLogging {
  import context.dispatcher

  override def receive: Receive = {
    case Ok =>
      log.debug(s"AskActor receives Ok")
      // pipe(Future.successful("create something one")) to sender()
      Future.successful("create something one") pipeTo sender()
    case Error =>
      log.debug(s"AskActor receives Error")
      Future.failed(new RuntimeException("error")) pipeTo sender()
  }
}

class TellActor(askActor: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher

  override def receive: Receive = {
    case Tell =>
      log.debug("TellActor receives Tell")
    case Ask(expectedResult) =>
      log.debug("TellActor receives Ask")
      val result: Future[Any] = (askActor ? expectedResult)(3.seconds)
      //pipe(result) to sender()
      result pipeTo sender()
  }
}

object ClassicActorMain extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("ClassicActorSystem")
  import system.dispatcher

  val askActor = system.actorOf(Props[AskActor], "askActor")
  val tellActor = system.actorOf(Props(classOf[TellActor], askActor), "tellActor")

  log.debug("tell")
  tellActor ! Tell

  log.debug("ask and return success")
  (tellActor ? Ask(Ok))(3.seconds).map { respose =>
    log.debug(s"receive response successfully. respose=${respose}")
  }

  log.debug("ask and return failure")
  (tellActor ? Ask(Error))(3.seconds).onComplete {
    case Success(s) => log.debug(s"receive response successfully. respose=${s}")
    case Failure(f) => log.debug(s"receive error. respose=${f.getMessage}")
  }
}