package payments.checker

import cloudflow.flink.{ FlinkStreamlet, FlinkStreamletLogic }
import cloudflow.streamlets.{ ConfigParameter, StreamletShape, StringConfigParameter }
import cloudflow.streamlets.avro.{ AvroInlet, AvroOutlet }
import org.apache.flink.streaming.api.scala.{ createTypeInformation, DataStream, OutputTag }
import payments.datamodel._

import scala.collection.immutable
import scala.util.matching.Regex

class PaymentCheckingStreamlet extends FlinkStreamlet {
  @transient val in: AvroInlet[PaymentString]      = AvroInlet[PaymentString]("in")
  @transient val out: AvroOutlet[PaymentObject]    = AvroOutlet[PaymentObject]("out")
  @transient val outLogger: AvroOutlet[LogMessage] = AvroOutlet[LogMessage]("out-logger")
  @transient val shape: StreamletShape             = StreamletShape.withInlets(in).withOutlets(out, outLogger)
  @transient val maskConf: StringConfigParameter   = StringConfigParameter("mask")

  override def configParameters: immutable.IndexedSeq[ConfigParameter] = Vector(maskConf)

  override protected def createLogic(): FlinkStreamletLogic = new FlinkStreamletLogic() {
    override def buildExecutionGraph(): Unit = {
      val maskForFilter = maskConf.value.r
      val outputLogger  = OutputTag[LogMessage](outLogger.name)
      val objectStream  = readStream(in).process(new MaskCheckingFunction(maskForFilter, outputLogger))

      writeStream(out, objectStream)
      writeStream(outLogger, objectStream.getSideOutput(outputLogger))
    }
  }
}
