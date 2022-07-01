package uk.gov.hmrc.mobilemessages.mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers.contentAsJson
import uk.gov.hmrc.mobilemessages.domain.{MessageCount, MessageCountResponse}

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

  def messagesAreFound(): Unit =
    stubFor(
      get(urlPathEqualTo("/messages")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(messagesJson)
      )
    )

  def messagesNotFoundException(): Unit =
    stubFor(get(urlPathEqualTo("/messages")).willReturn(aResponse().withStatus(404)))

  def messagesServiceUnavailableException(): Unit =
    stubFor(get(urlPathEqualTo("/messages")).willReturn(aResponse().withStatus(500)))

  def messagesResponse(service: String): String =
    s"""
       |{
       |  "id": "url1",
       |  "renderUrl": {
       |    "service": "$service",
       |    "url": "messagesByUrl"
       |  },
       |  "body": {
       |    "type": "2wsm-advisor",
       |    "threadId": "9794f96d-f595-4b03-84dc-1861408918fb"
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
      get(urlPathEqualTo(s"/messages/$id")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application.json")
          .withBody(if (headers) messagesResponse(service) else messagesResponseNoHeaders(service))
      )
    )

  def messagesNotFound(id: String): Unit =
    stubFor(
      get(urlPathEqualTo(s"/messages/$id")).willReturn(
        aResponse().withStatus(404)
      )
    )

  def messagesServiceIsUnavailable(id: String): Unit =
    stubFor(
      get(urlPathEqualTo(s"/messages/$id")).willReturn(
        aResponse().withStatus(500)
      )
    )

  def messageIsRenderedSuccessfully(): Unit =
    stubFor(
      get(urlPathEqualTo("/messagesByUrl")).willReturn(
        aResponse().withStatus(200)
      )
    )

}
