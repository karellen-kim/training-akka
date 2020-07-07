package com.example.classic.basic

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import com.example.classic.basic.ClassicActorMain.{log, system}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

class AskTypeCheckActor extends Actor with ActorLogging {
  import context.dispatcher

  override def receive: Receive = {
    case "first" => Future(1) pipeTo sender()
    case "second" => Future(List("a", "b", "c")) pipeTo sender()
    case "third" => Future(Map("a" -> 1, "b" -> 2, "c" -> 3)) pipeTo sender()
    case "fourth" => Future(Some(Map("a" -> 1, "b" -> 2, "c" -> 3))) pipeTo sender()
  }
}

object AskTypeCheckActorApp extends App {
  val log = LoggerFactory.getLogger(this.getClass)
  val system = ActorSystem("ClassicActorSystem")
  import system.dispatcher

  val askTypeCheckActor = system.actorOf(Props[AskTypeCheckActor], "AskTypeCheckActor")

  def get[A](tpe: String): Future[A] = {
    (askTypeCheckActor ? tpe) (3.seconds).map {
      case a : A =>
        a
      case _ : Any =>
        log.debug("try to match Any")
        throw new RuntimeException("Unexpected Error")
    }
  }

  def getOption[A](tpe: String): Future[Option[A]] = {
    (askTypeCheckActor ? tpe) (3.seconds).map {
      case a : Option[A] =>
        a
      case _ =>
        log.debug("try to match Any")
        throw new RuntimeException("Unexpected Error")
    }
  }

  get[Int]("first").map { response: Int =>
      log.debug(s"first : ${response}")
  }
  get[List[String]]("second").map { response : List[String]=>
    log.debug(s"second : ${response}")
  }
  get[Map[String, Int]]("third").map { response: Map[String, Int] =>
    log.debug(s"third : ${response}")
  }
  get[Option[Map[String, Int]]]("fourth").map { response: Option[Map[String, Int]] =>
    log.debug(s"fourth : ${response}")
  }

  get[Int]("fourth")
    .map { response: Int => log.debug(s"fourth : ${response}") }
    .recoverWith { e =>
      log.debug(e.getMessage)
      Future.successful()
    }

  getOption[Map[String, Int]]("fourth")
    .map { response: Option[Map[String, Int]] => log.debug(s"#getOption fourth : ${response}") }
    .recoverWith { e =>
      log.debug(e.getMessage)
      Future.successful()
    }

  getOption[String]("fourth")
    .map { response: Option[String] =>
      log.debug(s"#getOption fourth Error : ${response.get.indexOf('a')}")
    }
    .recoverWith { e =>
      log.debug("Type Error : " + e.getMessage)
      Future.successful()
    }
}