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

package uk.gov.hmrc.mobilemessages.acceptance.microservices

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.{DateTime, LocalDate}
import play.api.http.HeaderNames
import uk.gov.hmrc.mobilemessages.domain.MessageHeader

class MessageService(authToken: String) {

  def headersListReturns(messageHeaders: Seq[MessageHeader]): Unit = {
    givenThat(get(urlEqualTo(s"/messages")).
      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().
        withBody(
          jsonRepresentationOf(messageHeaders)
        )))
  }

  def jsonRepresentationOf(messageHeaders: Seq[MessageHeader]) = {
    s"""
       |{
       |  "items" : [
       |     ${messageHeaders.map(messageHeaderAsJson).mkString(",")}
       |  ],
       |  "count": {
       |    "total": ${messageHeaders.size},
       |    "read": ${messageHeaders.count(header => header.readTime.isDefined)}
       |  }
       |}""".stripMargin
  }

  def headerWith(id: String,
                 subject: String = "message subject",
                 validFrom: LocalDate = new LocalDate(29348L),
                 readTime: Option[DateTime] = None,
                 sentInError: Boolean = false) = {
    MessageHeader(id, subject, validFrom, readTime, sentInError)
  }

  private def messageHeaderAsJson(messageHeader: MessageHeader): String = {
    if (messageHeader.readTime.isDefined)
      s"""
         |    {
         |      "id": "${messageHeader.id}",
         |      "subject": "${messageHeader.subject}",
         |      "validFrom": "${messageHeader.validFrom}",
         |      "readTime": "${messageHeader.readTime.get}",
         |      "sentInError": ${messageHeader.sentInError}
         |    }"""
    else
      s"""
         |    {
         |      "id": "${messageHeader.id}",
         |      "subject": "${messageHeader.subject}",
         |      "validFrom": "${messageHeader.validFrom}",
         |      "sentInError": ${messageHeader.sentInError}
         |    }"""
  }
}
