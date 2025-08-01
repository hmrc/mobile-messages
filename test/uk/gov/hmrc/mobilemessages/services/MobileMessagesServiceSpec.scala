/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.mobilemessages.connector.model.ResourceActionLocation
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader}
import uk.gov.hmrc.mobilemessages.utils.Setup
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class MobileMessagesServiceSpec extends Setup {

  val appName: String = "mobile-messages"

  def mockAuditAndConnector(response: Seq[MessageHeader]) = {
    when(auditConnector.sendEvent(any())(any(), any()))
      .thenReturn(Future.successful(Success))
    when(mockMessageConnector.messages(any())(any(), any())).thenReturn(Future.successful(response))
  }

  val auditConnector: AuditConnector = mock[AuditConnector]
  val auditService:   AuditService   = new AuditService(auditConnector, "mobile-messages")

  implicit val defaultTimeout: FiniteDuration = 5.seconds
  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val service: MobileMessagesService =
    new MobileMessagesService("mobile-messages", mockMessageConnector, auditConnector, configuration, auditService)

  "readAndUnreadMessages()" should {
    "return an empty seq of messages" in {

      mockAuditAndConnector(Seq.empty)
      await(service.readAndUnreadMessages(Some("en"))) mustBe Seq.empty
    }

    "return a populated seq of messages" in {

      mockAuditAndConnector(messageServiceHeadersResponse)
      await(service.readAndUnreadMessages(Some("en"))) mustBe messageServiceHeadersResponse
    }

    "readMessageContent(messageId: MessageId)" should {

      val updatedMessage = message.convertedFrom(
        message.bodyWith(id = messageId.value, markAsReadUrl = Some(ResourceActionLocation("sa-message-renderer", "url")))
      )

      def mockAuditAndConnector(response1: Message) = {
        stubAuditReadMessageContent()(auditConnector)
        when(mockMessageConnector.getMessageBy(any(), any())(any(), any())).thenReturn(Future.successful(response1))
        when(mockMessageConnector.render(any(), any(), any())(any())).thenReturn(Future.successful(html))
      }

      "return an html page with headers and mark an unread message as read" in {

        mockAuditAndConnector(updatedMessage)
        when(mockMessageConnector.markAsRead(any())(any(), any())).thenReturn(ReadSuccessEmptyResult)
        await(service.readMessageContent(messageId, Some("en"))) mustBe RenderedMessage(html)
      }

          "return an html page with headers when receiving read messages" in {

            val updatedMessage = message.convertedFrom(message.bodyWith(id = messageId.value))

            mockAuditAndConnector(updatedMessage)
            await(service.readMessageContent(messageId, Some("en"))) mustBe RenderedMessage(
              html
            )
          }

    }
  }
}
