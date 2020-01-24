package linkscore.app

import scala.annotation.tailrec

class LinkscoreRepl(console: ConsoleInOut, commands: Commands) {
  import commands._

  @tailrec
  private def inputLoop(next: State): State = next match {
      case Finished => Finished
      case state => inputLoop(state.action())
    }

  def run(): Unit = inputLoop(ReadCommand(console))
}

object StdLinkscoreRepl extends LinkscoreRepl(new StdInputOutput, Configuration.commands) {
  def main(args: Array[String]): Unit = run()
}