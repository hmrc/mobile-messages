# mobile-messages

[![Build Status](https://travis-ci.org/hmrc/mobile-messages.svg?branch=master)](https://travis-ci.org/hmrc/customer-profile) [ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-messages/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-messages/_latestVersion)

Digital messages for a UK tax customer that has opted-in to paperless communications


Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```.


API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/messages``` | GET | Returns all the user's digital messages. [More...](docs/messages.md)  |
| ```/messages/read``` | POST | Returns a specific user message as an HTML partial. [More...](docs/read-message.md)  |
| ```/messages/count``` | GET | Returns a count of all the user's digital messages. [More...](docs/messages-count.md)  |


# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint, e.g.
```
    GET /sandbox/messages
```

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" to specify the appropriate status code and return payload. 
See each linked file for details:

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/messages``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/messages.md)  |
| ```/read-message``` | POST | Acts as a stub for the related live endpoint. [More...](docs/sandbox/read-message.md)  |
| ```/read-message``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/messages-count.md)  |


# Version
Version of API need to be provided in `Accept` request header
```
Accept: application/vnd.hmrc.v1.0+json
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
