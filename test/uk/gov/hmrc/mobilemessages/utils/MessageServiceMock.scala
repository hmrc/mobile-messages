/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.mobilemessages.utils

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import uk.gov.hmrc.mobilemessages.connector.model.{Details, ResourceActionLocation, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.domain._

class MessageServiceMock(authToken: String) {

  def fullUrlFor(
    serviceName: String,
    path:        String
  ): String = {
    val port = 9000
    val host = "localhost"
    s"http://$host:$port$path"
  }

  def convertedFrom(messageBody: UpstreamMessageResponse): Message =
    messageBody.markAsReadUrl match {
      case Some(location) =>
        UnreadMessage(
          MessageId(messageBody.id),
          fullUrlFor(messageBody.renderUrl.service, messageBody.renderUrl.url),
          fullUrlFor(location.service, location.url),
          Some("2wsm-advisor"),
          Some("9794f96d-f595-4b03-84dc-1861408918fb")
        )
      case _ =>
        ReadMessage(
          MessageId(messageBody.id),
          fullUrlFor(messageBody.renderUrl.service, messageBody.renderUrl.url),
          Some("2wsm-advisor"),
          Some("9794f96d-f595-4b03-84dc-1861408918fb")
        )
    }

  def bodyWith(
    id:            String,
    renderUrl:     ResourceActionLocation = ResourceActionLocation("sa-message-renderer", "/utr/render"),
    markAsReadUrl: Option[ResourceActionLocation] = None
  ): UpstreamMessageResponse =
    UpstreamMessageResponse(id,
                            renderUrl,
                            markAsReadUrl,
                            Some(Details(Some("2wsm-advisor"), Some("9794f96d-f595-4b03-84dc-1861408918fb"))))

  def bodyToBeMarkedAsReadWith(id: String): UpstreamMessageResponse =
    bodyWith(id = id, markAsReadUrl = Some(ResourceActionLocation("message", s"/messages/$id/read-time")))

  def headerWith(
    id:          String,
    subject:     String = "message subject",
    validFrom:   LocalDate = Instant.ofEpochMilli(29348L).atZone(ZoneId.of("UTC")).toLocalDate,
    readTime:    Option[LocalDateTime] = None,
    sentInError: Boolean = false
  ): MessageHeader =
    MessageHeader(MessageId(id), subject, validFrom, readTime, sentInError)

  def jsonRepresentationOf(message: UpstreamMessageResponse): String =
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

  def jsonRepresentationOf(messageHeaders: Seq[MessageHeader]): String =
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

  private def messageHeaderAsJson(messageHeader: MessageHeader): String =
    if (messageHeader.readTime.isDefined)
      s"""
         | {
         | "id": "${messageHeader.id.value}",
         | "subject": "${messageHeader.subject}",
         | "validFrom": "${messageHeader.validFrom}",
         | "readTime": "${messageHeader.readTime.get}",
         | "sentInError": ${messageHeader.sentInError}
         | }
      """.stripMargin
    else
      s"""
         | {
         | "id": "${messageHeader.id.value}",
         | "subject": "${messageHeader.subject}",
         | "validFrom": "${messageHeader.validFrom}",
         | "sentInError": ${messageHeader.sentInError}
         | }
      """.stripMargin
}
