
# mobile-token-exchange

[![Build Status](https://travis-ci.org/hmrc/mobile-token-exchange.svg?branch=master)](https://travis-ci.org/hmrc/mobile-token-exchange) [ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-token-exchange/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-token-exchange/_latestVersion)

The micro-service is responsible for storing a users refresh-token in mongo using the deviceId as a key to the record. The refresh-token is sourced from the API Gateway service /oauth/token.


## Endpoints

| Path                               | Supported Methods | Description  |
| -----------------------------------| ------------------| ------------|
|```/token/registration ```          | POST              | Create or update an existing record with a users refresh-token. [More...](docs/registration.md) |
|```/token/registration/:deviceId``` | GET               | Find a existing record by deviceId. [More...](docs/registration.md) [More...](docs/find.md) |
|```/token/registration/update```    | POST              | Update an existing record with a new refresh-token. [More...](docs/update.md) ||


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    