Messages
----
  Returns all the user's digital messages

* **URL**

  `/messages`

* **Method:**

  `GET`

*  **URL Params**

   N/A

* **Success Response:**

  * **Code:** 200 <br />
    **Response body:**

```json

```

* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


