package payments.ingress

import spray.json._
import payments.datamodel._

case object JsonParticipant extends DefaultJsonProtocol {
  implicit val crFormat =
    jsonFormat(ParticipantInfo.apply, "participantId", "balance")
}