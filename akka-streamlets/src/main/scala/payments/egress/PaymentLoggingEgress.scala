package payments.egress

import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.RunnableGraph
import cloudflow.akkastream.scaladsl.{ FlowWithCommittableContext, RunnableGraphStreamletLogic }
import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import payments.datamodel._

class PaymentLoggingEgress extends AkkaStreamlet {
  val in: AvroInlet[LogMessage]        = AvroInlet[LogMessage]("in")
  override def shape(): StreamletShape = StreamletShape.withInlets(in)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {

    def log(logMessage: LogMessage): Unit = logMessage match {
      case LogMessage(message, "INFO") => system.log.info(message)
      case LogMessage(message, "WARN") => system.log.warning(message)
      case _                           => system.log.error("Unknown operation.")
    }

    def flow: FlowWithCommittableContext[LogMessage, LogMessage]#Repr[LogMessage, ConsumerMessage.Committable] =
      FlowWithCommittableContext[LogMessage].map { logMessage: LogMessage ⇒
        log(logMessage)
        logMessage
      }

    def runnableGraph: RunnableGraph[_] =
      sourceWithCommittableContext(in).via(flow).to(committableSink)
  }
}
