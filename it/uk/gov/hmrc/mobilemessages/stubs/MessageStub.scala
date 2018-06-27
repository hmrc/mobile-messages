package uk.gov.hmrc.mobilemessages.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object MessageStub {
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

  def messagesAreFound(): Unit = {
    stubFor(get(urlPathEqualTo("/messages")).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json")
        .withBody(messagesJson)))
  }

  def messagesNotFoundException(): Unit = {
    stubFor(get(urlPathEqualTo("/messages")).willReturn(
      aResponse().withStatus(404)))
  }

  def messagesServiceUnavailableException(): Unit = {
    stubFor(get(urlPathEqualTo("/messages")).willReturn(
      aResponse().withStatus(500)))
  }

  val messagesByJson: String =
    """
      |{
      |  "id": "url1",
      |  "renderUrl": {
      |    "service": "service1",
      |    "url": "messagesByUrl"
      |  },
      |  "markAsReadUrl": {
      |    "service": "service2",
      |    "url": "url2"
      |  }
      |}""".stripMargin

  def messagesBySuccess(id: String): Unit =
    stubFor(get(urlPathEqualTo(s"/messages/$id")).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application.json")
        .withBody(messagesByJson)
    ))

  def messagesByNotFoundException(id: String): Unit =
    stubFor(get(urlPathEqualTo(s"/messages/$id")).willReturn(
      aResponse().withStatus(404)
    ))

  def messagesByServiceUnavailableException(id: String): Unit =
    stubFor(get(urlPathEqualTo(s"/messages/$id")).willReturn(
      aResponse().withStatus(500)
    ))

  def renderCallSuccess(): Unit =
    stubFor(get(urlPathEqualTo("/messagesByUrl")).willReturn(
      aResponse().withStatus(200)
    ))

}
