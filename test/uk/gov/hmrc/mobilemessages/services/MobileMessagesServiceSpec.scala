/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.connector.model.ResourceActionLocation
import uk.gov.hmrc.mobilemessages.controllers.auth.Authority
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageId, UnreadMessage}
import uk.gov.hmrc.mobilemessages.stubs.AuditStub
import uk.gov.hmrc.mobilemessages.utils.Setup
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MobileMessagesServiceSpec extends UnitSpec with Setup with AuditStub {

  val service: MobileMessagesService = new MobileMessagesService(mockMessageConnector, mockAuditConnector, configuration)

  "readAndUnreadMessages()" should {
    "return an empty seq of messages" in {
      stubAuditReadAndUnreadMessages()
      (mockMessageConnector.messages()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *)
        .returns(Future successful Seq.empty)

      await(service.readAndUnreadMessages()) shouldBe Seq.empty
    }

    "return a populated seq of messages" in {


      stubAuditReadAndUnreadMessages()
      (mockMessageConnector.messages()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *)
        .returns(Future successful messageServiceHeadersResponse)

      await(service.readAndUnreadMessages()) shouldBe messageServiceHeadersResponse
    }
  }

  "readMessageContent(messageId: MessageId)" should {
    "return an html page and mark an unread message as read" in {
      stubAuditReadMessageContent()
      (mockMessageConnector.getMessageBy(_: MessageId)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *)
        .returns(Future successful message.convertedFrom(message.bodyWith(
          id = messageId.value, markAsReadUrl = Some(ResourceActionLocation("sa-message-renderer", "url")))))
      (mockMessageConnector.render(_: Message, _: HeaderCarrier)(_: ExecutionContext, _: Option[Authority])).expects(*, *, *, *)
        .returns(Future successful html)
      (mockMessageConnector.markAsRead(_: UnreadMessage)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *)
        .returns(ReadSuccessEmptyResult)

      await(service.readMessageContent(messageId)) shouldBe html
    }

    "return an html page when receiving read messages" in {
      stubAuditReadMessageContent()
      (mockMessageConnector.getMessageBy(_: MessageId)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *)
        .returns(Future successful message.convertedFrom(message.bodyWith(id = messageId.value)))
      (mockMessageConnector.render(_: Message, _: HeaderCarrier)(_: ExecutionContext, _: Option[Authority])).expects(*, *, *, *)
        .returns(Future successful html)

      await(service.readMessageContent(messageId)) shouldBe html
    }

  }
}
