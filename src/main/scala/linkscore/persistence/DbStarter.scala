package linkscore.persistence

import java.io.{File, FileOutputStream}
import java.util.logging.{Level, Logger}

import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net, RuntimeConfigBuilder}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{Command, MongodExecutable, MongodStarter}
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.io.{IStreamProcessor, Processors}
import de.flapdoodle.embed.process.runtime.Network
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Try

object DbStarter {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val mongoExecutable: mutable.Buffer[MongodExecutable] = mutable.Buffer.empty

  private val root = Logger.getLogger("")
  private val handlers = root.getHandlers
  handlers foreach(_.setLevel(Level.OFF))

  def start(bindIp: String, port: Int): Boolean =
    if (mongoExecutable.isEmpty) {
      val config = new MongodConfigBuilder()
        .version(Version.Main.PRODUCTION)
        .net(new Net(bindIp, port, Network.localhostIsIPv6()))
        .build();
      Try(buildStarter) map { starter =>
        val mExec = starter.prepare(config)
        mongoExecutable += mExec
        mExec.start()
        logger.info(s"MongodDb started: mongodb://$bindIp:$port")
      }
      true
    } else {
      logger.warn(s"MongodDb already started: mongodb://$bindIp:$port")
      false
    }

  def stop(): Unit = {
    val mExec = mongoExecutable.remove(0)
    mExec.stop()
    logger.info(s"MongodDb stopped}")
  }

  private def buildStarter = {
    class FileStreamProcessor(file: File) extends IStreamProcessor {
      private val outputStream: FileOutputStream = new FileOutputStream(file)
      override def process(block: String): Unit = outputStream.write(block.getBytes());
      override def onProcessed(): Unit = outputStream.close();
    }
    val output = Processors.named("[mongod>]",
      new FileStreamProcessor(File.createTempFile("mongod", "log")));
    val error = new FileStreamProcessor(File.createTempFile("mongod-error", "log"));
    val commandsOutput = Processors.namedConsole("[console>]");

    val runtimeConfig = new RuntimeConfigBuilder()
      .defaults(Command.MongoD)
      .processOutput(new ProcessOutput(output, error, commandsOutput))
      .build();

    MongodStarter.getInstance(runtimeConfig);
  }
}
