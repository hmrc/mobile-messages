/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDateTime

import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilemessages.domain.{MessageHeader, MessageId}

import scala.util.Random

object DomainGenerator {

  import uk.gov.hmrc.domain.Generator

  val dateTime: LocalDateTime = LocalDateTime.now

  val generator      = new Generator()
  val saUtrGenerator = new SaUtrGenerator()

  def nextNino: Nino = generator.nextNino

  def nextSaUtr: SaUtr = saUtrGenerator.nextSaUtr

  val nino:          Nino      = nextNino
  val saUtr:         SaUtr     = nextSaUtr
  val readMessageId: MessageId = MessageId("543e8c6001000001003e4a9e")

  def readMessageHeader(saUtr: SaUtr = nextSaUtr): MessageHeader =
    MessageHeader(readMessageId,
                  "You have a new tax statement",
                  dateTime.minusDays(3).toLocalDate,
                  Some(dateTime.minusDays(1)),
                  sentInError = false)

  val unreadMessageId: MessageId = MessageId("643e8c5f01000001003e4a8f")

  def unreadMessageHeader(saUtr: SaUtr = nextSaUtr): MessageHeader =
    MessageHeader(unreadMessageId, "Stopping Self Assessment", dateTime.toLocalDate, None, sentInError = false)
}

//TODO add this to domain
sealed class SaUtrGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def randomNext: Int = random.nextInt(1000000)

  def nextSaUtr: SaUtr = SaUtr(randomNext.toString)
}
