package com.example.utils

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object CountableThreadPoolHelper {

  def apply(name: String, core: Int, max: Int, counter: AtomicInteger): ExecutionContextExecutor = {
    ExecutionContext.fromExecutor(
      new ThreadPoolExecutor(core, max, 1000 * 60, TimeUnit.SECONDS,
        new ArrayBlockingQueue[Runnable](max), new ThreadFactory() {
          override def newThread(r: Runnable): Thread = {
            new Thread(r, s"thread-${name}-${counter.getAndIncrement()}")
          }
        }))
  }
}
