Update
----
  Update an existing record refresh-token.
  
* **URL**

  `/token/registration/update `

* **Method:**
  
  `POST`

*  **JSON**

Please note the deviceId is the cookie mdtpdi.

```json
{
  "deviceId": "some device Id",
  "refreshToken": "some token",
  "timestamp": 123456789
}
```

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** 

The service returns the updated record which is associated with the deviceId.

```json
{
  "recordId": "some record Id",
  "deviceId": "some device Id",
  "refreshToken": "some refresh token",
  "timestamp": 123456789
}
```

* **Error Response:**

  * **Code:** 400 BADREQUEST <br />

  * **Code:** 404 NOT_FOUND <br/>

  * **Code:** 500 INTERNAL_SERVER_ERROR <br/>



