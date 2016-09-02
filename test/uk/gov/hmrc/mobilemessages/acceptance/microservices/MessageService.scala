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
import play.api.Play
import play.api.http.HeaderNames
import uk.gov.hmrc.mobilemessages.connector.model.{GetMessageResponseBody, ResourceActionLocation}
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId}
import uk.gov.hmrc.mobilemessages.utils.ConfigHelper.microserviceConfigPathFor

class MessageService(authToken: String) {

  def fullUrlFor(serviceName: String, path: String) = {
    val port = Play.current.configuration.getString(s"${microserviceConfigPathFor(serviceName)}.port").get
    val host = Play.current.configuration.getString(s"${microserviceConfigPathFor(serviceName)}.host").get
    s"http://$host:$port$path"
  }

  def convertedFrom(messageBody: GetMessageResponseBody): Message = {
    Message(
      MessageId(messageBody.id),
      fullUrlFor(messageBody.renderUrl.service, messageBody.renderUrl.url),
      None
    )
  }

  def getByIdReturns(message: GetMessageResponseBody): Unit = {
    givenThat(get(urlEqualTo(s"/messages/${message.id}")).
      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().
        withBody(
          jsonRepresentationOf(message)
        )))
  }

  def headersListReturns(messageHeaders: Seq[MessageHeader]): Unit = {
    givenThat(get(urlEqualTo(s"/messages")).
      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().
        withBody(
          jsonRepresentationOf(messageHeaders)
        )))
  }

  def bodyWith(id: String,
               renderUrl: ResourceActionLocation = ResourceActionLocation("sa-message-renderer", "/utr/render"),
               markAsReadUrl: Option[ResourceActionLocation] = None) = {
    GetMessageResponseBody(id, renderUrl, markAsReadUrl)
  }

  def headerWith(id: String,
                 subject: String = "message subject",
                 validFrom: LocalDate = new LocalDate(29348L),
                 readTime: Option[DateTime] = None,
                 sentInError: Boolean = false) = {
    MessageHeader(id, subject, validFrom, readTime, sentInError)
  }

  def jsonRepresentationOf(message: GetMessageResponseBody) = {
    if (message.markAsReadUrl.isDefined) {
      s"""
         |    {
         |      "id": "${message.id}",
         |      "markAsReadUrl": {
         |         "service": "${message.markAsReadUrl.get.service}",
         |         "url": "${message.markAsReadUrl.get.url}"
         |      },
         |      "renderUrl": {
         |         "service": "${message.renderUrl.service}",
         |         "url": "${message.renderUrl.url}"
         |      }
         |    }
      """.stripMargin
    } else {
      s"""
         |    {
         |      "id": "${message.id}",
         |      "renderUrl": {
         |         "service": "${message.renderUrl.service}",
         |         "url": "${message.renderUrl.url}"
         |      }
         |    }
      """.stripMargin
    }

  }

  def jsonRepresentationOf(messageHeaders: Seq[MessageHeader]) = {
    s"""
       | {
       | "items":[
       |   ${messageHeaders.map(messageHeaderAsJson).mkString(",")}
       | ],
       | "count": {
       | "total":     ${messageHeaders.size},
       | "read":     ${messageHeaders.count(header => header.readTime.isDefined)}
       |}
       |
      }
      """.stripMargin
  }

  private def messageHeaderAsJson(messageHeader: MessageHeader): String = {
    if (messageHeader.readTime.isDefined)
      s"""
         | {
         | "id": "${messageHeader.id}",
         | "subject": "${messageHeader.subject}",
         | "validFrom": "${messageHeader.validFrom}",
         | "readTime": "${messageHeader.readTime.get}",
         | "sentInError": ${messageHeader.sentInError}
         | }
      """.stripMargin
    else
      s"""
         | {
         | "id": "${messageHeader.id}",
         | "subject": "${messageHeader.subject}",
         | "validFrom": "${messageHeader.validFrom}",
         | "sentInError": ${messageHeader.sentInError}
         | }
      """.stripMargin
    }
  }
