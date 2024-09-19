# mobile-messages

[![Build Status](https://travis-ci.org/hmrc/mobile-messages.svg?branch=master)](https://travis-ci.org/hmrc/customer-profile) [ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-messages/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-messages/_latestVersion)

Digital messages for a UK tax customer that has opted-in to paperless communications


Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```.

## Development Setup
- Run locally: `sbt run` which runs on port `8234` by default
- Run with test endpoints: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The service can be run locally from Service Manager, using the following profiles:

| Profile Details                  | Command                                                                                                                                                                                     |
|----------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| MOBILE_MESSAGES_ALL          | sm2 --start MOBILE_MESSAGES_ALL --appendArgs '{"SECURE_MESSAGE": ["-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"]}'                                                                  |


## Run Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it:test`
- Run Unit and Integration Tests: `sbt test it:test`
- Run Unit and Integration Tests with coverage report: `sbt clean compile coverage test it:test coverageReport dependencyUpdates`



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
