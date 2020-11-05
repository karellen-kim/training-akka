package com.example.utils

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

trait AkkaApp extends App {
  val system = ActorSystem("ActorSystem")
  implicit val ec = system.dispatcher
  implicit val mat = Materializer(system)
  val log = LoggerFactory.getLogger(this.getClass)

  val timeout = 3.seconds
}
