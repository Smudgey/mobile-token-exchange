Find
----
  Find an existing record using the deviceId.
  
* **URL**

Please note the deviceId is the cookie mdtpdi.

  `/token/registration/:deviceId `

* **Method:**
  
  `GET`

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** 

The service returns the record associated with the deviceId.

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

  * **Code:** 500 INTERNALSERVERERROR <br/>



