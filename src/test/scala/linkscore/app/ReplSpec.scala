package linkscore.app

import com.mongodb.client.result.DeleteResult
import helpers.MockInputOutput
import linkscore.domain.{Entry, Report, Score}
import linkscore.persistence.LinkscoreRepo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mongodb.scala.Completed
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.io.AnsiColor._

class ReplSpec extends FlatSpec with Matchers with MockitoSugar {

  private val scoreRepo = mock[LinkscoreRepo]
  when(scoreRepo.insert(any[Entry])) thenAnswer ((_: InvocationOnMock) => {
    Future.successful(Completed)
  })
  when(scoreRepo.reportScoresByDomain) thenAnswer ((_: InvocationOnMock) => {
    Future.successful(Seq(Report("www.rte.ie", 1, Score(4))))
  })

  val commands = new Machine(scoreRepo)

  "The REPL interface" should "work as expected when given valid inputs" in {
    val io = new MockInputOutput(Seq(
      "ADD https://www.rte.ie/news/politics/2018/1004/1001034-cso/ 20",
      "ADD https://www.rte.ie/news/ulster/2018/1004/1000952-moanghan-mine/ 30",
      "ADD http://www.bbc.com/news/world-europe-45746837 10", "quit"))
    val expected = Seq(
      "enter command: ", "\tsaved URL(https://www.rte.ie/news/politics/2018/1004/1001034-cso/) score: 20",
      "enter command: ", "\tsaved URL(https://www.rte.ie/news/ulster/2018/1004/1000952-moanghan-mine/) score: 30",
      "enter command: ", "\tsaved URL(http://www.bbc.com/news/world-europe-45746837) score: 10",
      "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "When an invalid command is entered, the Repl" should "display an error, and re-prompt" in {
    val io = new MockInputOutput(Seq("ADD https://www.rte.ie/news/politics/2018/1004/1001034-cso/ ABC","quit"))
    val expected = Seq(
      "enter command: ",
      s"\t${RED}input error:${RESET} bad url/score: [https://www.rte.ie/news/politics/2018/1004/1001034-cso/ abc], ${CYAN}try 'HELP' for help$RESET",
      "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "When an invalid url is entered, the Repl" should "display an error, and re-prompt" in {
    val io = new MockInputOutput(Seq("ADD this-is-no-kind-of-url 8","quit"))
    val expected = Seq(
      "enter command: ",
      s"\t${RED}input error:${RESET} no protocol: this-is-no-kind-of-url, ${CYAN}try 'HELP' for help$RESET",
      "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "The help command" should "display command options" in {
    val io = new MockInputOutput(Seq("HELP", "quit"))
    val expected = Seq("enter command: ",
      "QUIT:\t\u001b[36mquit program\u001b[0m",
      "HELP:\t\u001b[36mdisplay this help\u001b[0m",
      "ADD:\t\u001b[36madd a URL with an associated social score\u001b[0m",
      "REMOVE:\t\u001b[36mremove a URL from the system\u001b[0m",
      "EXPORT:\t\u001b[36mexport statistics about the URLs stored in the system\u001b[0m",
      "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "The export command" should "report on current links" in {
    val io = new MockInputOutput(Seq("export", "quit"))
    val expected = Seq("enter command: ", "\tdomain;urls;social_score",  "\twww.rte.ie;1;4", "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "The remove command" should "return an error if no URL is provided" in {
    val io = new MockInputOutput(Seq("remove", "quit"))
    val expected = Seq("enter command: ",
      s"\t${RED}input error:${RESET} remove, ${CYAN}try 'HELP' for help${RESET}", "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "The remove command" should "return an error if URL not in db" in {
    when(scoreRepo.delete(any[String])) thenAnswer ((_: InvocationOnMock) => {
      Future.successful(0L)
    })
    val io = new MockInputOutput(Seq("remove http://rtw.ie/some/music", "quit"))
    val expected = Seq("enter command: ",
      s"\t${RED}input error:${RESET} failed to remove http://rtw.ie/some/music, ${CYAN}try 'HELP' for help${RESET}",
      "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

  "The remove command" should "succeed if the repo contains the url" in {
    when(scoreRepo.delete(any[String])) thenAnswer ((_: InvocationOnMock) => {
      Future.successful(1L)
    })
    val io = new MockInputOutput(Seq("remove http://rtw.ie/some/news", "quit"))
    val expected = Seq("enter command: ", "\tremoved http://rtw.ie/some/news", "enter command: ")

    new LinkscoreRepl(io,commands).run()

    io.output shouldBe expected
  }

}