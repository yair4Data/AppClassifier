package Util

import java.util.concurrent.{ExecutorService, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import com.google.common.util.concurrent.{MoreExecutors, ThreadFactoryBuilder}
import com.startapp.spring.Logging
import com.twitter.util._
/**
  * Created by yairf on 13/02/2018.
  */


object AsyncExecutableFactory {
  def initThreadPool(poolSize: Int, name: String): ExecutorService = {
    MoreExecutors.getExitingExecutorService(newThreadPoolExecutor(poolSize, name))
  }

  private def newThreadPoolExecutor(poolSize: Int, name: String): ThreadPoolExecutor = {
    new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue[Runnable],
      new ThreadFactoryBuilder().setNameFormat(s"$name-%s").build)
  }
}

//using twitter futurepool - https://medium.com/@sveinnfannar/thread-pooling-in-scala-using-twitter-s-futurepool-8f38c6448fa2
trait AsyncExecutable extends  Serializable with Logging {
  private lazy val pool: FuturePool = FuturePool(AsyncExecutableFactory.initThreadPool(poolSize, poolName))
  var poolSize: Int = 1
  var poolName: String = "AsyncPool"

  def async[A](f: => A): Future[A] = {
    pool {
      try {
        logger.debug(s"running async func")
        f
      } finally {
      }
    }
  }


  def asyncWithTimer[F](f: => F, timer: Timer, timeout: Duration, errorFunc: Throwable => String): F = {
    val promise = Promise[F]

    async(f).within(timeout)(timer)
      .onSuccess(
        response => promise.update(Return(response)))
      .onFailure { ex =>
        logger.error(errorFunc(ex))
        promise.setException(ex)
      }

    Await.result(promise) //case e: TimeoutException =>
  }

}

