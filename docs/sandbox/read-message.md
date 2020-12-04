Read a single message
----
  Acts as a stub to the /read-message endpoint.
  
  To trigger the sandbox endpoints locally, either access the /sandbox endpoint directly or supply the use the 
  "X-MOBILE-USER-ID" header with one of the following values: 208606423740 or 167927702220
  
* **URL**

  `/sandbox/read-message`

* **Method:**

  `GET`

*  **URL Params**

   **Required:**
   `journeyId=[String]`

    a string which is included for journey tracking purposes but has no functional impact


* **Success Responses:**

  To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:
  
  | *Value* | *Description* |
  |--------|----|
  | "NEW-TAX-STATEMENT" | Happy path, /messages/read ONLY, New Tax Statement |
  | "ANNUAL-TAX-SUMMARY" | Happy path, /messages/read ONLY, Annual Tax Summary |
  | "STOPPING-SA" | Happy path, /messages/read ONLY, Stopping Self Assessment |
  | "OVERDUE-PAYMENT" | Happy path, /messages/read ONLY, Overdue Payment |
  | "ERROR-401" | Unhappy path, trigger a 401 Unauthorized response |
  | "ERROR-403" | Unhappy path, trigger a 403 Forbidden response |
  | "ERROR-500" | Unhappy path, trigger a 500 Internal Server Error response |
  | Not set or any other value | Happy path, non-excluded Tax Credits Users |
