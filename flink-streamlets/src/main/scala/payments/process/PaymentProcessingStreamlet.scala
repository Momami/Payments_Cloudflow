package payments.process

import cloudflow.flink._
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.streaming.api.functions.co.{CoProcessFunction, KeyedCoProcessFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector
import payments.datamodel._

class PaymentProcessingStreamlet extends FlinkStreamlet {
  @transient val inParticipant: AvroInlet[ParticipantInfo] = AvroInlet[ParticipantInfo]("in-participant")
  @transient val inPayment: AvroInlet[PaymentObject]       = AvroInlet[PaymentObject]("in-payment")
  @transient val outLogger: AvroOutlet[LogInfo]            = AvroOutlet[LogInfo]("out")
  @transient def shape: StreamletShape =
    StreamletShape
      .withInlets(inParticipant, inPayment)
      .withOutlets(outLogger)

  override def createLogic(): FlinkStreamletLogic = new FlinkStreamletLogic() {
    override def buildExecutionGraph(): Unit = {
      val participants: DataStream[ParticipantInfo] =
        readStream(inParticipant)
          .keyBy(_ => "default")

      val payments: DataStream[PaymentObject] =
        readStream(inPayment)
          .keyBy(_ => "default")

      val logInfo: DataStream[LogInfo] = participants
        .connect(payments)
        .process(new MyProcessFunction)

      writeStream(outLogger, logInfo)
    }
  }

  class MyProcessFunction extends KeyedCoProcessFunction[String, ParticipantInfo, PaymentObject, LogInfo] {
    @transient lazy val participantState: MapState[String, ParticipantInfo] =
      getRuntimeContext
        .getMapState(
          new MapStateDescriptor[String, ParticipantInfo](
            "participant",
            classOf[String],
            classOf[ParticipantInfo]
          )
        )

    override def processElement1(
      value: ParticipantInfo,
      ctx: KeyedCoProcessFunction[String, ParticipantInfo, PaymentObject, LogInfo]#Context,
      out: Collector[LogInfo]
    ): Unit = {
      participantState.put(value.participantId, value)
    }

    override def processElement2(
      value: PaymentObject,
      ctx: KeyedCoProcessFunction[String, ParticipantInfo, PaymentObject, LogInfo]#Context,
      out: Collector[LogInfo]
    ): Unit = {
      value match {
        case PaymentObject(senderId, _, _) if !participantState.contains(senderId) =>
          out.collect(LogInfo("Sender does not exist.", "WARN"))
        case PaymentObject(_, receiverId, _) if !participantState.contains(receiverId) =>
          out.collect(LogInfo("Receiver does not exist.", "WARN"))
        case PaymentObject(senderId, _, sum) if sum > participantState.get(senderId).balance =>
          out.collect(LogInfo("The sender has insufficient funds.", "WARN"))
        case PaymentObject(senderId, receiverId, sum) =>
          val newSender = participantState.get(senderId)
          newSender.balance -= sum
          val newReceiver = participantState.get(receiverId)
          newReceiver.balance += sum
          participantState.put(senderId, newSender)
          participantState.put(receiverId, newReceiver)
          out.collect(LogInfo(s"Successful translation from $senderId to $receiverId: $sum", "INFO"))
        case _ => out.collect(LogInfo("Unknown command!", "WARN"))
      }
    }
  }
}
