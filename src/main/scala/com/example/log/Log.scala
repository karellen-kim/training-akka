package com.example.log

import org.slf4j.{Logger, LoggerFactory}

object Log {
  def apply[A](clazz: Class[A]) = new SimpleLog() // new Log(LoggerFactory.getLogger(clazz))
}

class Log(logger: Logger) {

  def title(msg: String) = {
    logger.debug(s"\n_______________________________\n ${msg}\n-------------------------------")
  }
  def debug(msg: String) = logger.debug(msg)
}

class SimpleLog {

  def title(msg: String) = {
    println(s"\n_______________________________\n ${msg}\n-------------------------------")
  }
  def debug(msg: String) = println(msg)
}