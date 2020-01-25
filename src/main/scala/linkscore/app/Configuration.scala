package linkscore.app

import java.net.ServerSocket

import linkscore.persistence.{DbStarter, LinkscoreRepo}
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext

object Configuration {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private def getOption[T](key: String): Option[T] = if (conf.hasPath(key)) {
    Some(conf.getAnyRef(key).asInstanceOf[T])
  } else {
    None
  }

  private val conf = ConfigFactory.load()

  private val parallelism = conf.getInt("persistence.executor.parallelism")
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
    new java.util.concurrent.ForkJoinPool(parallelism))

  private val dbName = conf.getString("mongodb.database.name")
  val dbHost: String = getOption[String]("mongodb.database.host")
    .getOrElse("localhost")
  val dbPort: Int = getOption[Int]("mongodb.database.port")
    .getOrElse(new ServerSocket(0).getLocalPort)

  DbStarter.start(dbHost, dbPort)
  logger.info(s"creating repo attached to mongodb://$dbHost:$dbPort")
  val scoreRepo = new LinkscoreRepo(s"mongodb://$dbHost:$dbPort/", dbName)
  val commands = new Commands(scoreRepo)

  def shutdown(): Unit =  DbStarter.stop()
}
