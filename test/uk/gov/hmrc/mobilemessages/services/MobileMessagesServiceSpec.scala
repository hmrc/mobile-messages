package uk.gov.hmrc.mobilemessages.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.stubs.AuditStub
import uk.gov.hmrc.mobilemessages.utils.Setup
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

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
      ???
    }

  }
}
