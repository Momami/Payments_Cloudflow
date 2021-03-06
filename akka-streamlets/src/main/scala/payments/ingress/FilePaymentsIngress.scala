package payments.ingress

import java.nio.file.{ FileSystem, FileSystems }

import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource
import akka.stream.scaladsl.{ FileIO, Framing }
import akka.util.ByteString
import cloudflow.akkastream._
import cloudflow.streamlets.avro.AvroOutlet
import cloudflow.streamlets._

import scala.collection.immutable
import scala.util.matching.Regex
import payments.datamodel._

import scala.concurrent.duration.DurationInt

class FilePaymentsIngress extends AkkaStreamlet {
  val out: AvroOutlet[PaymentString] = AvroOutlet[PaymentString]("out")

  def shape: StreamletShape = StreamletShape.withOutlets(out)

  val directoryConf: StringConfigParameter           = StringConfigParameter("catalog")
  val maskConf: StringConfigParameter                = StringConfigParameter("mask")
  val delimiterConf: StringConfigParameter           = StringConfigParameter("delimiter")
  val maximumFrameLengthConf: IntegerConfigParameter = IntegerConfigParameter("maximum-frame-length")

  override def configParameters: immutable.IndexedSeq[ConfigParameter] =
    Vector(directoryConf, maskConf, delimiterConf, maximumFrameLengthConf)

  override protected def createLogic(): AkkaStreamletLogic = new AkkaStreamletLogic() {
    val directory: String   = directoryConf.value
    val mask: Regex         = maskConf.value.r
    val delimiter: String   = delimiterConf.value
    val maxFrameLength: Int = maximumFrameLengthConf.value

    val fs: FileSystem = FileSystems.getDefault

    override def run(): Unit = {
      val read = DirectoryChangesSource(fs.getPath(directory), pollInterval = 1.second, maxBufferSize = 1000)
        .map(_._1)
        .filter(path => mask.pattern.matcher(path.getFileName.toString).matches())
        .flatMapConcat(FileIO.fromPath(_))
        .via(Framing.delimiter(ByteString(delimiter), maximumFrameLength = maxFrameLength))
        .map(msg => PaymentString(msg.utf8String))
      read.to(plainSink(out)).run()
    }
  }
}
