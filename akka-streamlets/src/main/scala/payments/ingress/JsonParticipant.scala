package payments.ingress

import spray.json._

case object JsonParticipant extends DefaultJsonProtocol {
  implicit val crFormat =
    jsonFormat(ParticipantInfo.apply, "participantId", "balance")
}