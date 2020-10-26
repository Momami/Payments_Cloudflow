package payments.checker


import cloudflow.flink.{FlinkStreamlet, FlinkStreamletLogic}
import cloudflow.streamlets.{ConfigParameter, StreamletShape, StringConfigParameter}
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import org.apache.flink.streaming.api.scala.{DataStream, createTypeInformation}
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.util.Collector
import payments.datamodel._

import scala.collection.immutable
import scala.util.matching.Regex

class PaymentCheckingStreamlet extends FlinkStreamlet {
  @transient val in                    = AvroInlet[PaymentString]("in")
  @transient val out                   = AvroOutlet[PaymentObject]("out")
  @transient val shape: StreamletShape = StreamletShape(in, out)

  val maskConf: StringConfigParameter = StringConfigParameter("mask")

  override def configParameters: immutable.IndexedSeq[ConfigParameter] = Vector(maskConf)

  override protected def createLogic(): FlinkStreamletLogic = new FlinkStreamletLogic() {
    override def buildExecutionGraph(): Unit = {
      val maskForFilter: Regex = maskConf.value.r

      val objectStream: DataStream[PaymentObject] =
        readStream(in)
          .flatMap {
            new FlatMapFunction[PaymentString, PaymentObject]() {
              @throws[Exception]
              def flatMap(value: PaymentString, out: Collector[PaymentObject]): Unit = {
                if (maskForFilter.pattern.matcher(value.payment).matches()) {
                  out.collect(ParserUtils.parsePaymentString(value.payment, maskForFilter))
                }
              }
            }
          }

      writeStream(out, objectStream)
    }
  }

}
