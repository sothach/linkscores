package linkscore.domain

import scala.util.{Failure, Try}
import java.net.{URL => JUrl}

case class URL(value: String) {
  require(Option(value).exists(_.nonEmpty), "url must not be empty")
}

sealed trait PositiveInt {
  def value: Int
  require(value > 0, "score must be positive")
}
case class Score(value: Int) extends PositiveInt

case class Entry(url: URL, score: Score)
object Entry {
  def apply(request: String): Try[Entry] = {
    val linkPattern = """(.+)\s+0*([1-9]\d*)""".r
    request.trim match {
      case linkPattern(url, score) =>
        Try(new JUrl(url)) map { value =>
          Entry(URL(value.toString), Score(score.toInt))
        }
      case _ =>
        Failure(new Throwable(s"bad url/score: [$request]"))
    }
  }
}
case class Report(domain: String, entries: Int, totalScore: Score) {
  def record: String = s"$domain;$entries;${totalScore.value}"
}