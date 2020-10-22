package payments.ingress


import java.nio.file.{FileSystem, FileSystems}

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Framing, RunnableGraph, Source}
import akka.util.ByteString
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaServerStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.avro.AvroOutlet
import cloudflow.streamlets.{ConfigParameter, IntegerConfigParameter, StreamletShape, StringConfigParameter}
import org.apache.kafka.clients.producer.RoundRobinPartitioner

import scala.collection.immutable
import scala.util.matching.Regex

class FilePaymentsIngress extends AkkaServerStreamlet{
  val out: AvroOutlet[Any] = AvroOutlet[PaymentsString]("out").withPartitioner(RoundRobinPartitioner)
  def shape: StreamletShape = StreamletShape.withOutlets(out)

  val directoryConf: StringConfigParameter = StringConfigParameter("catalog")
  val maskConf: StringConfigParameter = StringConfigParameter("mask")
  val delimiterConf: StringConfigParameter = StringConfigParameter("delimiter")
  val maximumFrameLengthConf: IntegerConfigParameter = IntegerConfigParameter("maximum-frame-length")

  override def configParameters: immutable.IndexedSeq[ConfigParameter] =
    Vector(directoryConf, maskConf, delimiterConf, maximumFrameLengthConf)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    val directory: String = directoryConf.value
    val mask: Regex = maskConf.value.r
    val delimiter: String = delimiterConf.value
    val maxFrameLength: Int = maximumFrameLengthConf.value

    val fs: FileSystem = FileSystems.getDefault
    val readPayments: Source[String, NotUsed] =
      Directory.ls(fs.getPath(directory))
        .filter(path => mask.pattern.matcher(path.getFileName.toString).matches())
        .flatMapConcat(FileIO.fromPath(_))
        .via(Framing.delimiter(ByteString(delimiter), maximumFrameLength = maxFrameLength))
        .map(msg => msg.utf8String)
    override def runnableGraph(): RunnableGraph[_] =
       readPayments.to(plainSink(out))
  }
}
