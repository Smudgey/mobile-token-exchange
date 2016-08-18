registration
----
  Register an API Gateway refresh-token against a users unique DeviceId. The service will either create a new record and update an existing record based on the deviceId.
  
* **URL**

  `/token/registration`

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

  * **Code:** 201 create or 200 update<br />
    **Content:** 

The service returns the record Id associated with the deviceId.
```json
{
  "id":"Some Record Id"
}
```

* **Error Response:**

  * **Code:** 400 BADREQUEST <br />

  * **Code:** 500 INTERNALSERVERERROR <br/>



