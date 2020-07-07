package com.example.classic.stream

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Status.{Success => AkkaSuccess}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.example.classic.stream.StreamActor.Start
import com.example.utils.CountableThreadPoolHelper
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object StreamActor {
  case class Start(str: String, refTo: ActorRef)
  case class ReceivedResult(str: String)
}

class StreamActor extends Actor with ActorLogging {

  implicit val mat = Materializer(context)
  implicit val ec = context.system.dispatcher

  override def receive: Receive = {
    case Start(str, refTo) =>
      log.debug(s"Start : ${str}")
      Source
        .future { Blocking.run(str) }
        .runWith {
          Sink.actorRef(refTo, AkkaSuccess())
        }
  }
}

object Blocking {
  val log = LoggerFactory.getLogger(this.getClass)

  final val commonPoolCounter = new AtomicInteger(1)
  implicit val commonPool = CountableThreadPoolHelper("commonPool", 3, 5, commonPoolCounter)

  def run(str: String)(implicit executor: ExecutionContext) : Future[String] = Future {
    log.debug(s"Run : ${str}")
    Thread.sleep(5000) // 3초
    s"End : ${str}"
  }
}

class StreamNonBlockingReceiver extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ : AkkaSuccess =>
      //log.debug(s"Complete")
    case msg : String =>
      log.debug(s"End : ${msg}")
    case msg =>
      log.debug(s"Unexpected msg : ${msg}")
  }
}

object StreamNonBlockingActorApp extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("ActorSystem")

  val timeout = 30.seconds
  val streamActor = system.actorOf(Props[StreamActor], "StreamActor")
  val receiverActor = system.actorOf(Props[StreamNonBlockingReceiver], "ReceiverActor")

  (streamActor ? Start("#1", receiverActor))(timeout)
  (streamActor ? Start("#2", receiverActor))(timeout)
  (streamActor ? Start("#3", receiverActor))(timeout)
  (streamActor ? Start("#4", receiverActor))(timeout)
}