/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.Named
import play.api.Configuration
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.connector._
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageCountResponse, MessageHeader, MessageId, UnreadMessage}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MobileMessagesService @Inject() (
  @Named("appName") val appName: String,
  val messageConnector:          MessageConnector,
  val auditConnector:            AuditConnector,
  val appNameConfiguration:      Configuration,
  auditService:                  AuditService) {

  def readAndUnreadMessages(
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Seq[MessageHeader]] =
    auditService.withAudit("readAndUnreadMessages", Map.empty) {
      messageConnector.messages()
    }

  def countOnlyMessages(
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[MessageCountResponse] =
    messageConnector.messageCount()

  def readMessageContent(
    messageId:   MessageId
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[RenderedMessage] =
    auditService.withAudit("readMessageContent", Map.empty) {
      messageConnector.getMessageBy(messageId) flatMap { message =>
        messageConnector.render(message, hc) map { renderedMessage =>
          markAsReadIfUnread.apply(message)
          RenderedMessage(renderedMessage)
        }
      }
    }

  private def markAsReadIfUnread(
    implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Message => Unit = {
    case unreadMessage @ UnreadMessage(_, _, _) => messageConnector.markAsRead(unreadMessage)
    case _                                      => ()
  }
}

case class RenderedMessage(html: Html)

object RenderedMessage
