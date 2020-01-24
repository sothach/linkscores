package linkscore.persistence

import linkscore.domain.{Entry, Report}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.DeleteResult

import scala.concurrent.{ExecutionContext, Future}

class LinkscoreRepo(dbUrl: String, dbName: String)(implicit val ex: ExecutionContext) {
  private case class MongoEntry(id: ObjectId, url: String, domain: String, score: Int)
  private object MongoEntry {
    def apply(entry: Entry): MongoEntry = {
      MongoEntry(new ObjectId(), entry.url.toString, entry.domain, entry.score)
    }
  }

  private val codecRegistry = fromRegistries(fromProviders(classOf[MongoEntry]), DEFAULT_CODEC_REGISTRY )
  private val mongoClient: MongoClient = MongoClient(dbUrl)
  private val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  private val links: MongoCollection[MongoEntry] = database.getCollection("links")

  def insert(item: Entry): Future[Completed] = links.insertOne(MongoEntry(item))
    .toFuture()

  def delete(key: String): Future[DeleteResult] =
    links.deleteMany(equal("url", s"URL($key)")).toFuture()

  def reportScoresByDomain: Future[Seq[Report]] = {
    val reports: MongoCollection[Document] = database.getCollection("links")
    reports.aggregate(Seq(group("$domain", sum("id", 1), sum("totalScore", "$score"))))
      .toFuture()
      .map { results =>
        results.map(doc =>
          Report(doc.getString("_id"), doc.getInteger("id").toInt, doc.getInteger("totalScore").toInt))
      }
    }

}
