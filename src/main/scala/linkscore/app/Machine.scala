package linkscore.app

import linkscore.domain.Entry
import linkscore.persistence.LinkscoreRepo

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.AnsiColor._
import scala.language.postfixOps
import scala.util.{Failure, Success}
import CommandSet._

class Machine(scoreRepo: LinkscoreRepo) {
  private val timeout = 10 seconds

  sealed trait State {
    def action(): State
  }

  object Finished extends State {
    def action(): State = this
  }

  sealed trait InputState extends State {
    def console: ConsoleInOut
    def prompt: String
    def next(input: String): State

    def action(): State = {
      console.print(prompt)
      val input = console.readLine().trim.toLowerCase
      CommandSet(input) match {
        case Some((Quit, _)) =>
          Finished
        case Some((Help, _)) =>
          Helping(this)
        case Some((Export, _)) =>
          Exporting(this)
        case Some((Remove, Some(params))) =>
          Removing(this,params)
        case Some((Add, Some(params))) =>
          Adding(this,params)
        case _ =>
          Complaining(this,input)
      }
    }

    def onError(input: String): Unit =
      console.println(s"\t${RED}input error:${RESET} $input, ${CYAN}try 'HELP' for help${RESET}")
  }

  case class Helping(parent: InputState) extends State {
    def action(): State = {
      commands foreach (c =>
        parent.console.println(c.describe))
      parent.action()
    }
  }

  case class Complaining(parent: InputState, input: String) extends State {
    def action(): State = {
      parent.onError(input)
      parent.action()
    }
  }

  case class Adding(parent: InputState, command: String) extends State {
    def action(): State = {
      Entry.apply(command) match {
        case Success(entry) =>
          Await.ready(scoreRepo.insert(entry), timeout)
          parent.console.println(s"\tsaved ${entry.url} score: ${entry.score.value}")
        case Failure(e) =>
          parent.onError(s"${e.getMessage}")
      }
      parent.action()
    }
  }

  case class Exporting(parent: InputState) extends State {
    def action(): State = {
      val export = Await.result(scoreRepo.reportScoresByDomain, timeout)
      parent.console.println("\tdomain;urls;social_score")
      export foreach ( report =>
        parent.console.println(s"\t${report.record}"))
      parent.action()
    }
  }

  case class Removing(parent: InputState, url: String) extends State {
    def action(): State = {
      val response = Await.result(scoreRepo.delete(url), timeout)
      if(response >= 1) {
        parent.console.println(s"\tremoved $url")
      } else {
        parent.onError(s"failed to remove $url")
      }
      parent.action()
    }
  }

  case class Reader(console: ConsoleInOut) extends InputState {
    val prompt = s"enter command: "
    def next(input: String): State = this
  }
}