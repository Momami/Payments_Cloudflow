package payments.process

import org.apache.flink.api.common.state.{ MapState, MapStateDescriptor }
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction
import org.apache.flink.util.Collector
import payments.datamodel.{ LogMessage, ParticipantInfo, PaymentObject }

class PaymentProcessFunction extends KeyedCoProcessFunction[String, ParticipantInfo, PaymentObject, LogMessage] {
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
    ctx: KeyedCoProcessFunction[String, ParticipantInfo, PaymentObject, LogMessage]#Context,
    out: Collector[LogMessage]
  ): Unit = {
    participantState.put(value.participantId, value)
  }

  override def processElement2(
    value: PaymentObject,
    ctx: KeyedCoProcessFunction[String, ParticipantInfo, PaymentObject, LogMessage]#Context,
    out: Collector[LogMessage]
  ): Unit = {
    value match {
      case PaymentObject(senderId, _, _, currency) if !participantState.contains(senderId) =>
        out.collect(LogMessage(s"Sender $senderId with currency $currency does not exist.", "WARN"))
      case PaymentObject(_, receiverId, _, currency) if !participantState.contains(receiverId) =>
        out.collect(LogMessage(s"Receiver $receiverId with currency $currency does not exist.", "WARN"))
      case PaymentObject(senderId, _, sum, _) if sum > participantState.get(senderId).balance =>
        out.collect(LogMessage(s"The sender $senderId has insufficient funds: $sum.", "WARN"))
      case PaymentObject(senderId, receiverId, sum, currency) =>
        val newSender = participantState.get(senderId)
        newSender.balance -= sum
        val newReceiver = participantState.get(receiverId)
        newReceiver.balance += sum
        participantState.put(senderId, newSender)
        participantState.put(receiverId, newReceiver)
        out.collect(LogMessage(s"Successful translation from $senderId to $receiverId: $sum $currency", "INFO"))
      case _ => out.collect(LogMessage("Unknown command!", "WARN"))
    }
  }
}
