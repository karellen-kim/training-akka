package com.example.classic.retry

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import com.example.classic.retry.RetryActor.{Response, Retry}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success, Try}

/**
  * 참고 : https://gist.github.com/codetinkerhack/8206481
  */
trait Result
case object Ok extends Result
case object Error extends Result

object RetryActor {
  case class Response(originalSender: ActorRef, result: Any)
  case class Retry(originalSender: ActorRef, message: Any, times: Int)
}

class RetryActor(forwardTo: ActorRef, maxTries: Int, retryTimeOut: FiniteDuration, retryInterval: FiniteDuration) extends Actor with ActorLogging {
  import context.dispatcher

  def retriable: Receive = {
    case Retry(originalSender, message, triesLeft) =>
      log.debug(s"Retry triesLeft=${triesLeft}")
      (forwardTo ? message)(retryTimeOut).onComplete {
        case Success(result) =>
          // 액터가 중지된 경우를 위해 (이 경우 deadletters)
          self ! Response(originalSender, result)
        case Failure(e) =>
          log.debug(s"onComplete Failure")
          if (triesLeft - 1 == 0) {
            self ! Response(originalSender, Failure(new Exception("Retries exceeded")))
          } else {
            log.debug("error occurred")
          }
      }
      context.system.scheduler.scheduleOnce(retryInterval, self, Retry(originalSender, message, triesLeft - 1))
    case Response(originalSender, result) =>
      log.debug("Done")
      originalSender ! result
      context stop self
  }

  override def receive: Receive = {
    case message @ _ =>
      self ! Retry(sender, message, maxTries)
      // become : 액터의 Receive를 새로운 Receive로 변경한다.
      context.become(retriable, false)
  }

  override def postStop() = {
    log.debug(s"Stop actor : ${self.path.name}")
  }
}

class MockActor extends Actor with ActorLogging {
  import context.dispatcher

  override def receive: Receive = {
    case Ok =>
      Future.successful("create something one") pipeTo sender()
    case Error =>
      Future.failed(new RuntimeException("error")) pipeTo sender()
  }
}

object RetryClassicActorApp extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("RetryActorSystem")
  import system.dispatcher

  val timeout = 5.seconds
  val mockActor = system.actorOf(Props[MockActor], "mockActor")
  def callback(res: Try[Any]) = res match {
    case Success(s) => log.debug(s"receive response successfully. respose=${s}")
    case Failure(f) => log.debug(s"receive error. respose=${f.getMessage}")
  }

/*  val retryActor1 = system.actorOf(Props(classOf[RetryActor], mockActor, 3, timeout, timeout), "retryActor1")
  log.title("success")
  (retryActor1 ? Ok)(timeout).onComplete(callback)*/

  val retryActor2 = system.actorOf(Props(classOf[RetryActor], mockActor, 3, timeout, 0.second), "retryActor2")
  log.debug("error")
  (retryActor2 ? Error)(timeout).onComplete(callback)
}