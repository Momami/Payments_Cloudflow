package payments.checker

import cloudflow.flink.{FlinkStreamlet, FlinkStreamletLogic}
import cloudflow.streamlets.{ConfigParameter, StreamletShape, StringConfigParameter}
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import org.apache.flink.streaming.api.scala.{DataStream, OutputTag, createTypeInformation}
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector
import payments.datamodel._

import scala.collection.immutable
import scala.util.matching.Regex

class PaymentCheckingStreamlet extends FlinkStreamlet {
  @transient val in: AvroInlet[PaymentString]   = AvroInlet[PaymentString]("in")
  @transient val out: AvroOutlet[PaymentObject] = AvroOutlet[PaymentObject]("out")
  @transient val outLogger: AvroOutlet[LogInfo] = AvroOutlet[LogInfo]("out-logger")
  @transient val shape: StreamletShape          = StreamletShape.withInlets(in).withOutlets(out, outLogger)
  val maskConf: StringConfigParameter           = StringConfigParameter("mask")

  override def configParameters: immutable.IndexedSeq[ConfigParameter] = Vector(maskConf)

  override protected def createLogic(): FlinkStreamletLogic = new FlinkStreamletLogic() {
    override def buildExecutionGraph(): Unit = {
      val maskForFilter: Regex = maskConf.value.r
      val outputLogger = OutputTag[LogInfo](outLogger.name)
      val objectStream: DataStream[PaymentObject] =
        readStream(in).process(new MaskCheckingFunction(maskForFilter, outputLogger))

      writeStream(out, objectStream)
      writeStream(outLogger, objectStream.getSideOutput(outputLogger))
    }
  }

  class MaskCheckingFunction(maskForFilter: Regex, outLoggerTag: OutputTag[LogInfo])
      extends ProcessFunction[PaymentString, PaymentObject] {
    override def processElement(
      value: PaymentString,
      ctx: ProcessFunction[PaymentString, PaymentObject]#Context,
      out: Collector[PaymentObject]
    ): Unit = {
      if (maskForFilter.pattern.matcher(value.payment).matches()) {
        out.collect(ParserUtils.parsePaymentString(value.payment, maskForFilter))
      } else {
        ctx.output(outLoggerTag, LogInfo(s"Incorrect transfer: ${value.payment}", "WARN"))
      }
    }
  }
}
