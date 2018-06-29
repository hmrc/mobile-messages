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
        "id": "543e8c6001000001003e4a9e",
        "subject": "You have a new tax statement",
        "validFrom": "2018-06-25",
        "readTime": 1530089941120,
        "readTimeUrl": "U1d3ZExBdVZoL0RRbjFCekVnZ3pTMmNDaG1ULzNxZmcxV1Z4ZXVWY0FrRT0=",
        "sentInError": false
    },
    { //unread message
        "id": "643e8c5f01000001003e4a8f",
        "subject": "Stopping Self Assessment",
        "validFrom": "2018-06-28",
        "readTimeUrl": "dFhvaGhaTEdJTG0wUXFRcEhRL3RUUHozYlpsSnA4NG1IdWpTb3k0MG1zND0=",
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


