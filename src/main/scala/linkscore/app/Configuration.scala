package linkscore.app

import java.net.ServerSocket
import java.time.LocalDateTime

import com.typesafe.config.ConfigFactory
import linkscore.persistence.{DbStarter, LinkscoreRepo}
import net.ceedubs.ficus.Ficus._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object Configuration {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val conf = ConfigFactory.load()
  private val parallelism =
    conf.as[Option[Int]]("persistence.executor.parallelism").getOrElse(5)

  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
    new java.util.concurrent.ForkJoinPool(parallelism))
  private implicit val ts: () => LocalDateTime = () => LocalDateTime.now

  val dbPort: Int = new ServerSocket(0).getLocalPort
  private val maybeMongoUrl = conf
    .as[Option[String]]("mongodb.database.url")
    .orElse(DbStarter.start)

  val mongoUrl: String =  maybeMongoUrl match {
    case Some(value) => value
    case None => throw new AssertionError("MongodDb must be resolved")
  }

  logger.info(s"creating repo attached to $mongoUrl")
  val scoreRepo = new LinkscoreRepo(mongoUrl)
  val commands = new Machine(scoreRepo)

  def shutdown(): Unit = DbStarter.stop()
}
