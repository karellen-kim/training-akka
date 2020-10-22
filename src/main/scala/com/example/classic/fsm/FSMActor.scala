package com.example.classic.fsm

import akka.actor.{Actor, ActorLogging, FSM}
import com.example.classic.fsm.FSMActor.{Failure, LoadTask, Logging, ProcessTask, State, Success}

object FSMActor {
  sealed trait State

  case object LoadTask extends State
  case object ProcessTask extends State
  case object Success extends State
  case object Failure extends State
  case object Logging extends State
}

case class Data(items: List[Long] = Nil, offset: Int = 0, count: Int = 0)

class FSMActor() extends Actor with FSM[State, Data] with ActorLogging {
  startWith(LoadTask, Data())

  when(LoadTask) {
    case Event(e, data) =>
      log.debug(s"LoadTask data=${data}")
      goto(ProcessTask) using data.copy(items = List(1, 2, 3))
  }

  when(ProcessTask) {
    case Event(e, data) =>
      log.debug(s"ProcessTask data=${data}")
      goto(Success) using data
    case Event(e, data) =>
      log.debug(s"ProcessTask data=${data}")
      goto(Failure) using data
  }

  when(Success) {
    case Event(e, data) =>
      log.debug(s"Success data=${data}")
      goto(Logging) using data
  }

  when(Failure) {
    case Event(e, data) =>
      log.debug(s"Failure data=${data}")
      goto(Logging) using data
  }

  onTransition {
    case LoadTask -> ProcessTask =>
      log.debug(s"LoadTask -> ProcessTask")
    case ProcessTask -> Success =>
      log.debug(s"ProcessTask -> Success")
    case ProcessTask -> Failure =>
      log.debug(s"ProcessTask -> Failure")
  }

  initialize()
}

