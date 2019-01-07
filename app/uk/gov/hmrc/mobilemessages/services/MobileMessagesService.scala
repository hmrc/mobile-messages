/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject._
import play.api.Configuration
import play.twirl.api.Html
import uk.gov.hmrc.api.service.Auditor
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.connector._
import uk.gov.hmrc.mobilemessages.controllers.auth.Authority
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId, UnreadMessage}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MobileMessagesService @Inject()(val messageConnector: MessageConnector,
                                      val auditConnector: AuditConnector,
                                      val appNameConfiguration: Configuration) extends Auditor {

  def readAndUnreadMessages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = {
    withAudit("readAndUnreadMessages", Map.empty) {
      messageConnector.messages()
    }
  }

  def readMessageContent(messageId: MessageId)(implicit hc: HeaderCarrier, ec: ExecutionContext, auth: Option[Authority]): Future[Html] =
    withAudit("readMessageContent", Map.empty) {
      messageConnector.getMessageBy(messageId) flatMap {
        message =>
          messageConnector.render(message, hc) map {
            renderedMessage =>
              markAsReadIfUnread.apply(message)
              renderedMessage
          }
      }
    }

  private def markAsReadIfUnread(implicit hc: HeaderCarrier, ec: ExecutionContext): Message => Unit = {
    case unreadMessage@UnreadMessage(_, _, _) => messageConnector.markAsRead(unreadMessage)
    case _ => ()
  }
}