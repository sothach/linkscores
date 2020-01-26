package linkscore.app

import scala.io.AnsiColor.{CYAN, RESET}

object CommandSet {
  sealed abstract class Command(val key: String, val help: String) {
    def describe: String = s"${key.toUpperCase}:\t$CYAN$help$RESET"
    def matches(v: String): Boolean =
      v == key || v.startsWith(key.take(1))
  }

  case object Quit extends Command("quit", "quit program")
  case object Help extends Command("help", "display this help")
  case object Add extends Command("add", "add a URL with an associated social score")
  case object Remove extends Command("remove", "remove a URL from the system")
  case object Export extends Command("export", "export statistics about the URLs stored in the system")

  val commands: Seq[Command] = Seq(Quit,Help,Add,Remove,Export)

  def apply(input: String): Option[(Command, Option[String])] = {
    val params = (v: String) => v.indexOf(" ") match {
      case n if n > -1 => Some(v.drop(n).trim)
      case _ => None
    }
    val request = input.trim.toLowerCase
    commands
      .find(_.matches(request))
      .map((_, params(request)))
  }
}