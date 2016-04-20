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

package uk.gov.hmrc.mobilemessages.sandbox

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.time.DateTimeUtils

import scala.util.Random

object DomainGenerator {

  import uk.gov.hmrc.domain.Generator

  val generator = new Generator()
  val saUtrGenerator = new SaUtrGenerator()

  def nextNino = generator.nextNino
  def nextSaUtr = saUtrGenerator.nextSaUtr

  val nino = nextNino
  val saUtr = nextSaUtr

  val readMessageId = BSONObjectID.generate.toString()
  def readMessageHeader(saUtr : SaUtr = nextSaUtr) = {
    MessageHeader(readMessageId,
      "Your Tax Return",
      DateTimeUtils.now.minusDays(3).toLocalDate,
      Some(DateTimeUtils.now.minusDays(1)),
      s"/message/sa/$saUtr/$readMessageId/read-time",
      false)
  }
  val readMessageHeaderJson = Json.toJson(readMessageHeader())

  val unreadMessageId = BSONObjectID.generate.toString()
  def unreadMessageHeader(saUtr: SaUtr = nextSaUtr) = MessageHeader(unreadMessageId,
    "Your Tax Return",
    DateTimeUtils.now.toLocalDate,
    None,
    s"/message/sa/$saUtr/$unreadMessageId/read-time",
    false)
  val unreadMessageHeaderJson = Json.toJson(unreadMessageHeader())

}


//TODO add this to domain
sealed class SaUtrGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def randomNext = random.nextInt(1000000)

  def nextSaUtr: SaUtr = SaUtr(randomNext.toString)
}
