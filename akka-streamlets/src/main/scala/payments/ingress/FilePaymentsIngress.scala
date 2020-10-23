package payments.ingress


import java.nio.file.{FileSystem, FileSystems}

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import cloudflow.akkastream._
import cloudflow.streamlets.avro.AvroOutlet
import cloudflow.streamlets._

import scala.collection.immutable
import scala.util.matching.Regex
import payments.datamodel._

class FilePaymentsIngress extends AkkaServerStreamlet {
  val out: AvroOutlet[PaymentString] = AvroOutlet[PaymentString]("out")

  def shape: StreamletShape = StreamletShape.withOutlets(out)

  val directoryConf: StringConfigParameter = StringConfigParameter("catalog")
  val maskConf: StringConfigParameter = StringConfigParameter("mask")
  val delimiterConf: StringConfigParameter = StringConfigParameter("delimiter")
  val maximumFrameLengthConf: IntegerConfigParameter = IntegerConfigParameter("maximum-frame-length")

  override def configParameters: immutable.IndexedSeq[ConfigParameter] =
    Vector(directoryConf, maskConf, delimiterConf, maximumFrameLengthConf)

  override protected def createLogic(): AkkaStreamletLogic = new AkkaStreamletLogic() {
    val directory: String = directoryConf.value
    val mask: Regex = maskConf.value.r
    val delimiter: String = delimiterConf.value
    val maxFrameLength: Int = maximumFrameLengthConf.value

    val fs: FileSystem = FileSystems.getDefault

    override def run(): Unit = {
      val readPayments: Source[PaymentString, NotUsed] =
        Directory.ls(fs.getPath(directory))
          .filter(path => mask.pattern.matcher(path.getFileName.toString).matches())
          .flatMapConcat(FileIO.fromPath(_))
          .via(Framing.delimiter(ByteString(delimiter), maximumFrameLength = maxFrameLength))
          .map(msg => PaymentString(msg.utf8String))
      readPayments.to(plainSink(out)).run()
    }
  }
}
