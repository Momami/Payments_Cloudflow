package payments.ingress


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import cloudflow.akkastream.util.scaladsl.HttpServerLogic


class ParticipantInitializeIngress extends AkkaServerStreamlet{
  val out: AvroOutlet[ParticipantInfo] = AvroOutlet[ParticipantInfo]("out")
  override def shape: StreamletShape = StreamletShape.withOutlets(out)

  override protected def createLogic = HttpServerLogic.default(this, out)
}
