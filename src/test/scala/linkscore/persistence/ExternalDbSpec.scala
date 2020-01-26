package linkscore.persistence

import java.time.LocalDateTime

import helpers.MongoDbContainer
import linkscore.domain.{Entry, Report, Score}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class ExternalDbSpec extends FlatSpec with ScalaFutures with Matchers with BeforeAndAfterAll {
  private implicit val defaultPatience: PatienceConfig = PatienceConfig(1 minute)
  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val ts: () => LocalDateTime = () => LocalDateTime.parse("2020-01-01T12:30:00")
  private val db = new MongoDbContainer
  lazy val scoreRepo = new LinkscoreRepo(db.getUrl)

  override def beforeAll(): Unit = {
    db.start()
    val links = streamDataFile("linkscores.txt")
      .map(Entry(_)).collect {
      case Success(entry) => entry
    }
    links foreach { link =>
      Await.ready(scoreRepo.insert(link), 1 second)
    }
  }

  override def afterAll(): Unit = db.stop()

  "A large collection of links" should "be reduced correctly" in {
    val result = scoreRepo.reportScoresByDomain.futureValue
      .map ( item => item.domain -> item.totalScore.value).toMap

    result.size should be(426)
    result("oakley.com") should be(4)
    result("reuters.com") should be(4)
    result("t-online.de") should be(3)
    result("usda.gov") should be(1)
  }

  private def streamDataFile(resource: String): Iterator[String] = {
    val stream = getClass.getResourceAsStream(s"/$resource")
    scala.io.Source.fromInputStream(stream).getLines()
  }

}
