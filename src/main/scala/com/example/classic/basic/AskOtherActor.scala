package com.example.classic.basic

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.example.classic.basic.AskOtherActor.One
import org.slf4j.LoggerFactory

object AskOtherActor {
  case class One(str: String, refTo: ActorRef)
}

class AskOtherActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case One(str, refTo) =>
      println(s"msg : ${str}")
      refTo ! str
    case _ =>
      println("Unxepected Error")
  }
}

class DummyActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case str =>
      println(s"Receive str : ${str}")
  }
}

object AskOtherActorApp extends App {
  val log = LoggerFactory.getLogger(this.getClass)
  val system = ActorSystem("ClassicActorSystem")
  import system.dispatcher

  val dummyActor = system.actorOf(Props[DummyActor], "DummyActor")
  val askOtherActor = system.actorOf(Props[AskOtherActor], "AskOtherActor")

  dummyActor ! "Start"
  askOtherActor ! One("#1", dummyActor)
}