/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilemessages.services

import org.joda.time.DateTime
import play.twirl.api.Html
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.api.service.Auditor
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mobilemessages.config.MicroserviceAuditConnector
import uk.gov.hmrc.mobilemessages.connector._
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.mobilemessages.sandbox.DomainGenerator._
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

trait MobileMessagesService {
  def readAndUnreadMessages()(implicit hc:HeaderCarrier, ec : ExecutionContext): Future[Seq[MessageHeader]]
  def readMessageContent(readTimeUrl : String)(implicit hc: HeaderCarrier, ec: ExecutionContext, auth: Option[Authority]): Future[Html]
}

trait LiveMobileMessagesService extends MobileMessagesService with Auditor {
  def authConnector: AuthConnector

  def messageConnector : MessageConnector
  def entityResolverConnector : EntityResolverConnector

  override def readAndUnreadMessages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] ={
    withAudit("readAndUnreadMessages", Map.empty) {
      for {
        accounts <- authConnector.accounts()
        msghrs <- messages(accounts.saUtr)
      } yield msghrs
    }
  }

  private def messages(maybeSaUtr : Option[SaUtr])(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Seq[MessageHeader]] =
    maybeSaUtr match {
      case Some(utr) => entityResolverConnector.messages(utr)
      case _ => Future.successful(Seq.empty)
    }

  override def readMessageContent(readTimeUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, auth: Option[Authority]): Future[Html] =
    withAudit("readMessageContent", Map.empty) {
      messageConnector.readMessageContent(readTimeUrl)
    }
}

trait SandboxMobileMessagesService extends MobileMessagesService with FileResource {

  implicit val dateTime:DateTime
  val saUtr : SaUtr

  def readAndUnreadMessages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] =
    Future.successful(Seq(readMessageHeader(saUtr), unreadMessageHeader(saUtr)))

  override def readMessageContent(readTimeUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, auth: Option[Authority]): Future[Html] = {
    val partial = readTimeUrl.contains(readMessageId) match {
      case true => newTaxStatement
      case false => stoppingSA
    }
    Future.successful(partial)
  }

}

object SandboxMobileMessagesService extends SandboxMobileMessagesService {

  import uk.gov.hmrc.mobilemessages.sandbox.DomainGenerator._

  implicit val dateTime: DateTime = DateTimeUtils.now
  val saUtr = nextSaUtr
}

object LiveMobileMessagesService extends LiveMobileMessagesService {
  override val authConnector: AuthConnector = AuthConnector

  override val messageConnector: MessageConnector = MessageConnector

  override val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  val auditConnector: AuditConnector = MicroserviceAuditConnector
}
