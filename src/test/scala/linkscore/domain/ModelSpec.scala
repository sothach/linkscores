package linkscore.domain

import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class ModelSpec extends FlatSpec with Matchers with MockitoSugar {

  "An invalid entry object" should "not be instantiated" in {
    val expected = "requirement failed: score must be positive"
    val createEntry = () => Entry(URL("https://strava.com"),Score(0))
    the [IllegalArgumentException] thrownBy createEntry() should have message expected
  }

  "Attempt to construct a URL with an empty string" should "fail" in {
    val expected = "requirement failed: url must not be empty"
    val createUrl = () => URL("")
    the [IllegalArgumentException] thrownBy createUrl() should have message expected
  }

}