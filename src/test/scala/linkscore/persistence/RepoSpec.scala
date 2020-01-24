package linkscore.persistence

import linkscore.app.Configuration
import linkscore.domain.{Entry, Report}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Success

class RepoSpec extends FlatSpec with ScalaFutures with Matchers with BeforeAndAfterAll {
  private implicit val ec: ExecutionContext = ExecutionContext.global
  import Configuration.{dbHost, dbPort, scoreRepo}

  override def beforeAll(): Unit = {
    val links = Seq(
      "https://www.rte.ie/news/politics/2018/1004/1001034-cso/ 20",
      "http://www.bbc.com/news/world-europe-45746837 10",
      "https://www.rte.ie/news/ulster/2018/1004/1000952-moanghan-mine/ 30",
      "http://www.bbc.com/news/world-europe-45746837 3")
      .map (Entry(_)).collect {
        case Success(entry) =>
          scoreRepo.insert(entry)
      }
    Await.ready(Future.sequence(links), 30 seconds)
  }

  override def afterAll(): Unit = Configuration.shutdown()

  "A collection of documents" should "be reduced correctly" in {
    val result = scoreRepo.reportScoresByDomain.futureValue
    result.size should be(2)
    result should contain(Report("www.bbc.com",2,13))
    result should contain(Report("www.rte.ie",2,50))
  }

  "Deleting a link" should "be reflected in the report" in {
    val deletion = scoreRepo.delete("http://www.bbc.com/news/world-europe-45746837")
    Await.ready(deletion, 10 seconds)

    val result = scoreRepo.reportScoresByDomain.futureValue

    result.size should be(1)
    result.filter(_.domain == "www.bbc.com") should be(empty)
    result should contain(Report("www.rte.ie",2,50))
  }

  "Attempting to start an already started DB" should "nave no effect" in {
    DbStarter.start(dbHost, dbPort) should be(false)
  }

}
