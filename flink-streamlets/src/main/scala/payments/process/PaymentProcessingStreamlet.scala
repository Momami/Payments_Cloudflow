package payments.process

import cloudflow.flink._
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import org.apache.flink.streaming.api.scala._
import payments.datamodel._

class PaymentProcessingStreamlet extends FlinkStreamlet {
  @transient val inParticipant: AvroInlet[ParticipantInfo] = AvroInlet[ParticipantInfo]("in-participant")
  @transient val inPayment: AvroInlet[PaymentObject]       = AvroInlet[PaymentObject]("in-payment")
  @transient val outLogger: AvroOutlet[LogMessage]         = AvroOutlet[LogMessage]("out")
  @transient def shape: StreamletShape =
    StreamletShape
      .withInlets(inParticipant, inPayment)
      .withOutlets(outLogger)

  override def createLogic(): FlinkStreamletLogic = new FlinkStreamletLogic() {
    override def buildExecutionGraph(): Unit = {
      val participants =
        readStream(inParticipant)
          .keyBy(_.currency)

      val payments =
        readStream(inPayment)
          .keyBy(_.currency)

      val LogMessage = participants
        .connect(payments)
        .process(new PaymentProcessFunction)

      writeStream(outLogger, LogMessage)
    }
  }
}
