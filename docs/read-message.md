Read Message
----
  Returns a specific user message as an HTML partial

* **URL**

  `/messages/read`

* **Method:**

  `POST`

*  **Request body**

    The URL provided in the request body can be obtained from the [/messages response](docs/messages.md) ```readTimeUrl``` value

```json
    {
      "url":  "/message/sa/1234512345/543e8c5f01000001003e4a9c/read-time"
    }
```

* **Success Response:**

  * **Code:** 200 <br />
    **Response body:**

```hmtl
TODO
```

* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


