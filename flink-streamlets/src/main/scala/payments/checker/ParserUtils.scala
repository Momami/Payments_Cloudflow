package payments.checker

import scala.util.matching.Regex
import payments.datamodel._

object ParserUtils {
  def parsePaymentString(pay: String, mask: Regex): PaymentObject = {
    val mask(sender, receive, sum) = pay
    PaymentObject(sender, receive, sum.toLong)
  }
}
