package linkscore.persistence

import java.net.{URL => JUrl}
import java.time.LocalDateTime

import linkscore.domain.{Entry, Report, Score}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates.set

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class LinkscoreRepo(dbUrl: String)
                   (implicit val timestamper: () => LocalDateTime, implicit val ex: ExecutionContext) {
  private case class MongoEntry(id: ObjectId, url: String, domain: String, score: Int,
                                timestamp: LocalDateTime, deleted: Boolean = false)
  private val dbName = "scores"
  private object MongoEntry {
    def apply(entry: Entry): Try[MongoEntry] = {
      Try(new JUrl(entry.url.value)) map { value =>
        MongoEntry(new ObjectId(), entry.url.toString, value.getHost, entry.score.value, timestamper())
      }
    }
  }

  private val codecRegistry = fromRegistries(fromProviders(classOf[MongoEntry]), DEFAULT_CODEC_REGISTRY )
  private val mongoClient: MongoClient = MongoClient(dbUrl)
  private val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  private val links: MongoCollection[MongoEntry] = database.getCollection("links")

  def insert(item: Entry): Future[Boolean] = for {
    mongoEntry <- Future.fromTry(MongoEntry(item))
    _ <- links.insertOne(mongoEntry).toFuture()
  } yield {
    true
  }

  def delete(key: String): Future[Long] =
    links.updateMany(
      equal("url", s"URL(${key.trim})"),
      set("deleted", true))
      .toFuture().map(_.getModifiedCount)

  def reportScoresByDomain: Future[Seq[Report]] =
    database.getCollection("links")
      .aggregate(Seq(filter(equal("deleted", false)), group("$domain", sum("id", 1), sum("totalScore", "$score"))))
      .toFuture()
      .map { results =>
        results.map(doc =>
          Report(doc.getString("_id"), doc.getInteger("id").toInt, Score(doc.getInteger("totalScore").toInt)))
      }

}
