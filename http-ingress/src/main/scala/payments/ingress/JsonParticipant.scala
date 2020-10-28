package payments.ingress

import spray.json._
import payments.datamodel._

object JsonParticipant extends DefaultJsonProtocol {

  implicit object ParticipantInfoJson extends RootJsonFormat[ParticipantInfo] {
    override def write(obj: ParticipantInfo): JsValue =
      JsObject("participantId" -> JsString(obj.participantId), "balance" -> JsNumber(obj.balance))

    override def read(json: JsValue): ParticipantInfo =
      json.asJsObject.getFields("participantId", "balance") match {
        case Seq(JsString(id), JsNumber(balance)) => ParticipantInfo(id, balance.toLong)
      }
  }
}
