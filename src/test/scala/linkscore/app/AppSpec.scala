package linkscore.app

import java.io.InputStream

import org.scalatest.{FlatSpec, Matchers}

class AppSpec extends FlatSpec with Matchers {

  "When the Repl is launched, the Std IO channels" should "be connected" in {
    import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
    val stdIn = System.in
    val stdOut = System.out
    val in: InputStream = new ByteArrayInputStream("help\nquit\n".getBytes())
    System.setIn(in)
    val out = new ByteArrayOutputStream
    System.setOut(new PrintStream(out))
    StdLinkscoreRepl.main(Array.empty)
    out.close()
    System.setIn(stdIn)
    System.setOut(stdOut)
    succeed
  }

}