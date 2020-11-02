package payments.checker

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.flink.util.Collector
import payments.datamodel.{ LogMessage, PaymentObject, PaymentString }

import scala.util.matching.Regex

class MaskCheckingFunction(maskForFilter: Regex, outLoggerTag: OutputTag[LogMessage])
    extends ProcessFunction[PaymentString, PaymentObject] {
  override def processElement(
    value: PaymentString,
    ctx: ProcessFunction[PaymentString, PaymentObject]#Context,
    out: Collector[PaymentObject]
  ): Unit = {
    if (maskForFilter.pattern.matcher(value.payment).matches()) {
      out.collect(ParserUtils.parsePaymentString(value.payment, maskForFilter))
    } else {
      ctx.output(outLoggerTag, LogMessage(s"Incorrect transfer: ${value.payment}", "WARN"))
    }
  }
}
