package com.example.quartz

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import org.slf4j.LoggerFactory

case object Tick
class ReceiveActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case Tick =>
      log.info("Tick")
  }
}

object SimpleQuartzApp extends App {
  val log = LoggerFactory.getLogger(this.getClass)
  val system = ActorSystem("ClassicActorSystem")
  val receiveActor = system.actorOf(Props[ReceiveActor], "ReceiveActor")

  val scheduler = QuartzSchedulerExtension(system)
  QuartzSchedulerExtension(system).schedule("Every30Seconds", receiveActor, Tick)

}
