package uk.gov.hmrc.mobilemessages.mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.commons.codec.binary.Base64

object MessageMock {

  val messagesJson: String =
    """{
      |  "items": [
      |      {
      |        "id" : "543e8c6001000001003e4a9e",
      |        "subject" : "Your Tax Return",
      |        "validFrom" : "2013-06-04",
      |        "readTime": "2014-05-02T17:17:45.618Z",
      |        "sentInError": true
      |      },
      |      {
      |        "id" : "543e8c5f01000001003e4a9c",
      |        "subject" : "Your Tax Return",
      |        "validFrom" : "2013-06-04",
      |        "sentInError": true
      |      }
      |   ],
      |   "count": {
      |      "total": 2,
      |      "unread": 1
      |   }
      |}
      |""".stripMargin

  val messagesCountJson: String =
    """{
      |   "count": {
      |      "total": 2,
      |      "unread": 1
      |   }
      |}
      |""".stripMargin

  def messagesAreFound(): Unit =
    stubFor(
      get(urlPathEqualTo("/secure-messaging/messages")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(messagesJson)
      )
    )

  def messagesNotFoundException(): Unit =
    stubFor(get(urlPathEqualTo("/secure-messaging/messages")).willReturn(aResponse().withStatus(404)))

  def messagesTooManyRequestsException(): Unit =
    stubFor(get(urlPathEqualTo("/secure-messaging/messages")).willReturn(aResponse().withStatus(429)))

  def messagesServiceUnavailableException(): Unit =
    stubFor(get(urlPathEqualTo("/secure-messaging/messages")).willReturn(aResponse().withStatus(500)))

  def messagesResponse(service: String): String =
    s"""
       |{
       |  "id": "url1",
       |  "renderUrl": {
       |    "service": "$service",
       |    "url": "messagesByUrl"
       |  },
       |  "markAsReadUrl": {
       |    "service": "$service",
       |    "url": "url2"
       |  }
       |}""".stripMargin

  def messagesResponseNoHeaders(service: String): String =
    s"""
       |{
       |  "id": "url1",
       |  "renderUrl": {
       |    "service": "$service",
       |    "url": "messagesByUrl"
       |  },
       |  "markAsReadUrl": {
       |    "service": "$service",
       |    "url": "url2"
       |  }
       |}""".stripMargin

  def messageFound(
    id:      String,
    service: String,
    headers: Boolean = true
  ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/secure-messaging/messages/${Base64.encodeBase64String(id.getBytes)}")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application.json")
          .withBody(if (headers) messagesResponse(service) else messagesResponseNoHeaders(service))
      )
    )

  def messagesNotFound(id: String): Unit =
    stubFor(
      get(urlPathEqualTo(s"/secure-messaging/messages/${Base64.encodeBase64String(id.getBytes)}")).willReturn(
        aResponse().withStatus(404)
      )
    )

  def messagesServiceIsUnavailable(id: String): Unit =
    stubFor(
      get(urlPathEqualTo(s"/secure-messaging/messages/${Base64.encodeBase64String(id.getBytes)}")).willReturn(
        aResponse().withStatus(500)
      )
    )

  def messageIsRenderedSuccessfully(): Unit =
    stubFor(
      get(urlPathEqualTo("/messagesByUrl")).willReturn(
        aResponse().withStatus(200)
      )
    )

  def messageCountFound(): Unit =
    stubFor(
      get(urlPathEqualTo("/secure-messaging/messages/count")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(messagesCountJson)
      )
    )

  def messagesCountNotFoundException(): Unit =
    stubFor(get(urlPathEqualTo("/secure-messaging/messages/count")).willReturn(aResponse().withStatus(404)))

  def messagesCountTooManyRequestsException(): Unit =
    stubFor(get(urlPathEqualTo("/secure-messaging/messages/count")).willReturn(aResponse().withStatus(429)))

  def messagesCountServiceUnavailableException(): Unit =
    stubFor(get(urlPathEqualTo("/secure-messaging/messages/count")).willReturn(aResponse().withStatus(500)))

}
