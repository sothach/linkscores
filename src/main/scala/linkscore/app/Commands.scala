package linkscore.app

import linkscore.domain.Entry
import linkscore.persistence.LinkscoreRepo

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.AnsiColor._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class Commands(scoreRepo: LinkscoreRepo) {
  private val timeout = 10 seconds

  sealed trait State {
    def action(): State
  }

  object Finished extends State {
    def action() = this
  }

  sealed trait InputState extends State {
    val exit = "quit"
    val help = "help"

    def commandHelp: Seq[(String, String)] = Seq(
      ("QUIT", "quit program"),
      ("HELP", "display this help"),
      ("ADD", "add a URL with an associated social score"),
      ("REMOVE", "remove a URL from the system"),
      ("EXPORT", "export statistics about the URLs stored in the system")
    )

    def console: ConsoleInOut

    def prompt: String

    def next(input: String): State

    def action() = {
      console.print(prompt)
      console.readLine().trim.toLowerCase match {
        case v if v == exit =>
          Finished
        case v if v == help =>
          DisplayHelp(this)
        case v =>
          next(v)
      }
    }

    def onError(input: String): Unit =
      console.println(s"\t${RED}input error:${RESET} $input, ${CYAN}try 'HELP' for help${RESET}")
  }

  case class DisplayHelp(parent: InputState) extends State {
    def action() = {
      parent.commandHelp foreach { case (command, purpose) =>
        parent.console.println(s"$command:\t$CYAN$purpose$RESET")
      }
      parent.action()
    }
  }

  case class ReadCommand(console: ConsoleInOut) extends InputState {
    val prompt = s"enter command: "

    def next(input: String): State = input match {
      case command if command.startsWith("add ") =>
        Entry.apply(command.drop(4)) match {
          case Success(entry) =>
            Await.ready(scoreRepo.insert(entry), timeout)
            console.println(s"\tsaved ${entry.url} domain: ${entry.domain} score: ${entry.score}")
          case Failure(e) =>
            onError(s"${e.getMessage}")
        }
        this
      case command if command.startsWith("remove ") =>
        val url = command.drop(7).trim
        Await.ready(scoreRepo.delete(url), timeout)
        console.println(s"\tdeleted $url")
        this
      case "export" =>
        val export = Await.result(scoreRepo.reportScoresByDomain, timeout)
        console.println("\tdomain;urls;social_score")
        export foreach ( report =>
          console.println(s"\t${report.record}"))
        this
      case error =>
        onError(s"[$error]")
        this
    }
  }

}
