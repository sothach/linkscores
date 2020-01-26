package linkscore.app

import scala.annotation.tailrec

class LinkscoreRepl(console: ConsoleInOut, stateMachine: Machine) {
  import stateMachine._

  @tailrec
  private def inputLoop(next: State): State = next match {
      case Finished => Finished
      case state => inputLoop(state.action())
    }

  def run(): Unit = inputLoop(Reader(console))
}

object StdLinkscoreRepl extends LinkscoreRepl(new StdInputOutput, Configuration.commands) {
  def main(args: Array[String]): Unit = run()
}