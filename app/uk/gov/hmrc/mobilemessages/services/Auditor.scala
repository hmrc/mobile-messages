package uk.gov.hmrc.customerprofile.services

import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait Auditor extends AppName {

  val auditConnector : AuditConnector

  def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier) = {
    def auditResponse(): Unit = {
      auditConnector.sendEvent(
        DataEvent(appName, "ServiceResponseSent",
          tags = Map("transactionName" -> service),
          detail = details))
    }
  }

  def withAudit[T](service: String, details: Map[String, String])(func: Future[T])(implicit hc: HeaderCarrier) = {
    audit(service, details) // No need to wait!
    func
  }
}
