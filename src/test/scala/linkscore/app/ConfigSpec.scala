package linkscore.app

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ConfigSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "The configuration object" should "configure and build the repo" in {
    val commands = Configuration.commands
    commands should not be(null)
  }

}
