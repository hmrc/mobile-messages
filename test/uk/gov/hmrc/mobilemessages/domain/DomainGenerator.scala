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

package uk.gov.hmrc.mobilemessages.domain

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils

import scala.util.Random


class DomainFormatCheckSpec extends UnitSpec {

  import DomainGenerator._

  "MessageHeader" should {
    import MessageHeader.formats

    "be set to READ" in {
      Logger.debug("READ message header response : " + Json.prettyPrint(readMessageHeaderJson))
    }
    "be set to UNREAD" in {
      Logger.debug("UNREAD message header response : " + Json.prettyPrint(unreadMessageHeaderJson))
    }
  }
}

object DomainGenerator {

  import uk.gov.hmrc.domain.Generator

  val nino = new Generator().nextNino
  val saUtr = new SaUtrGenerator().nextSaUtr

  val readMessageId = "543e8c6001000001003e4a9e"
  val readMessageHeader = MessageHeader(readMessageId,
    "Your Tax Return",
    DateTimeUtils.now.minusDays(3).toLocalDate,
    Some(DateTimeUtils.now.minusDays(1)),
    s"/message/sa/$saUtr/$readMessageId/read-time",
    false)
  val readMessageHeaderJson = Json.toJson(readMessageHeader)

  val unreadMessageId = "643e8c5f01000001003e4a8f"
  val unreadMessageHeader = MessageHeader(unreadMessageId,
    "Your Tax Return",
    DateTimeUtils.now.toLocalDate,
    None,
    s"/message/sa/$saUtr/$unreadMessageId/read-time",
    false)
  val unreadMessageHeaderJson = Json.toJson(unreadMessageHeader)


}

//TODO add this to domain
sealed class SaUtrGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def randomNext = random.nextInt(1000000)

  def nextSaUtr: SaUtr = SaUtr(randomNext.toString)
}
