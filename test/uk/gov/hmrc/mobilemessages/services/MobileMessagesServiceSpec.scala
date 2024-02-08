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

import org.scalamock.handlers.CallHandler3
import org.scalamock.matchers.MatcherBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.connector.model.ResourceActionLocation
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageId, UnreadMessage}
import uk.gov.hmrc.mobilemessages.mocks.AuditStub
import uk.gov.hmrc.mobilemessages.utils.Setup
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MobileMessagesServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with Setup
    with AuditStub {

  val appName: String = "mobile-messages"
  def mockAudit(
                 transactionName: String,
                 detail: Map[String, String] = Map.empty
               ): CallHandler3[DataEvent, HeaderCarrier, ExecutionContext, Future[
    AuditResult
  ]] = {
    def dataEventWith(
                       auditSource: String,
                       auditType: String,
                       tags: Map[String, String]
                     ): MatcherBase =
      argThat { (dataEvent: DataEvent) =>
        dataEvent.auditSource.equals(auditSource) &&
          dataEvent.auditType.equals(auditType) &&
          dataEvent.tags.equals(tags) &&
          dataEvent.detail.equals(detail)
      }

    (auditConnector
      .sendEvent(_: DataEvent)(_: HeaderCarrier, _: ExecutionContext))
      .expects(
        dataEventWith(
          appName,
          auditType = "ServiceResponseSent",
          tags = Map("transactionName" -> transactionName)
        ),
        *,
        *
      )
      .returns(Future successful Success)
  }

  val auditConnector: AuditConnector = mock[AuditConnector]
  val auditService: AuditService = new AuditService(auditConnector, "mobile-messages")

  val service: MobileMessagesService =
    new MobileMessagesService("mobile-messages", mockMessageConnector, auditConnector, configuration, auditService)

  "readAndUnreadMessages()" should {
    "return an empty seq of messages" in {

      (mockMessageConnector
        .messages()(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *)
        .returns(Future successful Seq.empty)
      mockAudit("readAndUnreadMessages")

      await(service.readAndUnreadMessages()) shouldBe Seq.empty
    }

    "return a populated seq of messages" in {


      (mockMessageConnector
        .messages()(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *)
        .returns(Future successful messageServiceHeadersResponse)
      mockAudit("readAndUnreadMessages")
      await(service.readAndUnreadMessages()) shouldBe messageServiceHeadersResponse
    }
  }

  "readMessageContent(messageId: MessageId)" should {
    "return an html page with headers and mark an unread message as read" in {

      (mockMessageConnector
        .getMessageBy(_: MessageId)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *)
        .returns(
          Future successful message.convertedFrom(
            message.bodyWith(id            = messageId.value,
                             markAsReadUrl = Some(ResourceActionLocation("sa-message-renderer", "url")))
          )
        )
      stubAuditReadMessageContent()(auditConnector)
      (mockMessageConnector
        .render(_: Message, _: HeaderCarrier)(_: ExecutionContext))
        .expects(*, *, *)
        .returns(Future successful html)
      (mockMessageConnector
        .markAsRead(_: UnreadMessage)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *)
        .returns(ReadSuccessEmptyResult)

      await(service.readMessageContent(messageId)) shouldBe RenderedMessage(
        html
      )
    }

    "return an html page with headers when receiving read messages" in {
      mockAudit("readMessageContent")
      (mockMessageConnector
        .getMessageBy(_: MessageId)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *)
        .returns(Future successful message.convertedFrom(message.bodyWith(id = messageId.value)))
      (mockMessageConnector
        .render(_: Message, _: HeaderCarrier)(_: ExecutionContext))
        .expects(*, *, *)
        .returns(Future successful html)

      await(service.readMessageContent(messageId)) shouldBe RenderedMessage(
        html
      )
    }

  }


}
