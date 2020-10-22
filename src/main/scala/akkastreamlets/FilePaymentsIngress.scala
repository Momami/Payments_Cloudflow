package akkastreamlets

import cloudflow.akkastream.{AkkaServerStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.{ReadOnlyMany, StreamletShape, VolumeMount}
import cloudflow.streamlets.avro.AvroOutlet

class FilePaymentsIngress extends AkkaServerStreamlet{
  val out: AvroOutlet[Any] = AvroOutlet[PaymentsString]("out")
  def shape: StreamletShape = StreamletShape.withOutlets(out)

  private val sourceData    = VolumeMount("source-data-mount", "/mnt/data", ReadOnlyMany)
  override def volumeMounts = Vector(sourceData)

  override protected def createLogic(): AkkaStreamletLogic = {}
}
