Messages
----
  Returns a metadata list of all the user's digital messages

* **URL**

  `/messages`

* **Method:**

  `GET`


* **Success Response:**

  * **Code:** 200 <br />
    **Response body:**

```json
[
  { //read message, i.e has readTime value
    "id" : "543e8c6001000001003e4a9e",
    "subject" : "Your Tax Return",
    "validFrom" : "2013-06-04",
    "readTime": "2014-05-02T17:17:45.618Z",
    "sentInError": false
  },
  { //unread message
    "id" : "543e8c5f01000001003e4a9c",
    "subject" : "Your Tax Return",
    "validFrom" : "2013-06-04",
    "sentInError": false
  }
]
```

* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


