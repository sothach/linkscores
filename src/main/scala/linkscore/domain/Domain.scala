package linkscore.domain

import scala.util.{Failure, Try}
import java.net.{URL => JUrl}

case class URL(value: String)

case class Entry(url: URL, domain: String, score: Int)
object Entry {
  def apply(request: String): Try[Entry] = {
    val linkPattern = """(.+)\s+(\d+)""".r

    request.trim match {
      case linkPattern(url, score) =>
        Try(new JUrl(url)) map { value =>
          Entry(URL(value.toString), value.getHost, score.toInt)
        }
      case _ =>
        Failure(new Throwable(s"Bad url/score: [$request]"))
    }
  }
}
case class Report(domain: String, entries: Int, totalScore: Int) {
  def record: String = s"$domain;$entries:$totalScore"
}