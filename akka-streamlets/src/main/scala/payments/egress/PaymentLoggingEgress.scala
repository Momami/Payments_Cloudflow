package payments.egress


import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.RunnableGraph
import cloudflow.akkastream.scaladsl.{FlowWithCommittableContext, RunnableGraphStreamletLogic}
import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import payments.datamodel._

class PaymentLoggingEgress extends AkkaServerStreamlet {
  val in: AvroInlet[LogInfo] = AvroInlet[LogInfo]("in")
  override def shape(): StreamletShape = StreamletShape.withInlets(in)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {

    def log(logInfo: LogInfo): Unit = logInfo match {
      case LogInfo(message, "INFO") => system.log.info(message)
      case LogInfo(message, "WARN") => system.log.warning(message)
      case _ => system.log.error("Unknown operation.")
    }

    def flow: FlowWithCommittableContext[LogInfo, LogInfo]#Repr[LogInfo, ConsumerMessage.Committable] =
      FlowWithCommittableContext[LogInfo]
        .map { logInfo: LogInfo ⇒
          log(logInfo)
          logInfo
        }

    def runnableGraph: RunnableGraph[_] =
      sourceWithCommittableContext(in).via(flow).to(committableSink)
  }
}