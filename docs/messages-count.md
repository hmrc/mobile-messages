Messages
----
Returns a count of all the user's digital messages

* **URL**

  `/messages/count`

* **Method:**

  `GET`

* **URL Params**

  **Required:**
  `journeyId=[String]`

  a string which is included for journey tracking purposes but has no functional impact

* **Success Response:**

    * **Code:** 200 <br />
      **Response body:**

```json
{
  "count": {
    "total": 5,
    "unread": 2
  }
}
```

* **Error Response:**

    * **Code:** 401 UNAUTHORIZED <br />
      **Content:** `{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}`

    * **Code:** 401 UNAUTHORIZED <br />
      **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}`

    * **Code:** 406 NOT ACCEPTABLE <br />
      **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

    * **Code:** 500 INTERNAL_SERVER_ERROR <br />


