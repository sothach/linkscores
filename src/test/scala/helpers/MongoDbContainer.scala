package helpers

import org.testcontainers.containers.GenericContainer

class MongoDbContainer extends
    GenericContainer[MongoDbContainer](MongoDbContainer.DEFAULT_IMAGE_AND_TAG) {
  addExposedPort(MongoDbContainer.MONGODB_PORT)
  lazy val host: String = getContainerIpAddress
  lazy val port: Int = getMappedPort(MongoDbContainer.MONGODB_PORT)

  def getUrl: String = s"mongodb://$host:$port"

}

object MongoDbContainer {
  val MONGODB_PORT = 27017
  val DEFAULT_IMAGE_AND_TAG = "mongo:latest"
}

