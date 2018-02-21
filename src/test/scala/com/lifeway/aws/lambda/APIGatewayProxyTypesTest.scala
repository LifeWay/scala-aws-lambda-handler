package com.lifeway.aws.lambda

import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.parser._
import utest._

object APIGatewayProxyTypesTest extends TestSuite with LambdaProxy {
  val tests = Tests {
    'RequestContextIdentityDecoding - {
      "all fields provided" - {
        val providedJson =
          """
            |{
            |  "cognitoIdentityPoolId": "poolId",
            |  "accountId": "accountId",
            |  "cognitoIdentityId": "cognitoId",
            |  "caller": "caller",
            |  "apiKey": "apiKey",
            |  "sourceIp": "sourceIp",
            |  "cognitoAuthenticationType": "cognitoAuthType",
            |  "cognitoAuthenticationProvider": "cognitoAuthProvider",
            |  "userArn": "userArn",
            |  "userAgent": "userAgent",
            |  "user": "user"
            |}
          """.stripMargin
        val expectedObject = RequestContextIdentity(
          Some("poolId"),
          Some("accountId"),
          Some("cognitoId"),
          Some("caller"),
          Some("apiKey"),
          "sourceIp",
          Some("cognitoAuthType"),
          Some("cognitoAuthProvider"),
          Some("userArn"),
          Some("userAgent"),
          Some("user")
        )

        val json       = parse(providedJson).right.get
        val decodedObj = json.as[RequestContextIdentity].right.get

        assert(decodedObj == expectedObject)
      }

      "required fields only with new fields" - {
        val providedJson =
          """
            |{
            |  "sourceIp": "sourceIp",
            |  "newFeatureId": "Some Awesome new feature from Amazon!"
            |}
          """.stripMargin
        val expectedObject = RequestContextIdentity(
          None,
          None,
          None,
          None,
          None,
          "sourceIp",
          None,
          None,
          None,
          None,
          None
        )

        val json       = parse(providedJson).right.get
        val decodedObj = json.as[RequestContextIdentity].right.get

        assert(decodedObj == expectedObject)
      }
    }

    'RequestContextDecoding - {
      "all fields provided" - {
        val providedJson =
          """
            |{
            |  "path": "/{proxy+}",
            |  "accountId": "123456789012",
            |  "resourceId": "nl9h80",
            |  "stage": "test-invoke-stage",
            |  "requestId": "test-invoke-request",
            |  "identity": {
            |    "sourceIp": "sourceIP"
            |  },
            |  "resourcePath": "/{proxy+}",
            |  "httpMethod": "POST",
            |  "apiId": "r275xc9bmd"
            |}
          """.stripMargin

        val expectedObject = RequestContext(
          "/{proxy+}",
          Some(123456789012l),
          Some("nl9h80"),
          "test-invoke-stage",
          Some("test-invoke-request"),
          RequestContextIdentity(None, None, None, None, None, "sourceIP", None, None, None, None, None),
          "/{proxy+}",
          "POST",
          Some("r275xc9bmd")
        )

        val json       = parse(providedJson).right.get
        val decodedObj = json.as[RequestContext].right.get

        assert(decodedObj == expectedObject)
      }

      "required fields only with new fields" - {
        val providedJson =
          """
            |{
            |  "path": "/{proxy+}",
            |  "awesomeNewFeature": "SomeReallyImportantNewthing",
            |  "stage": "test-invoke-stage",
            |  "identity": {
            |    "sourceIp": "sourceIP"
            |  },
            |  "resourcePath": "/{proxy+}",
            |  "httpMethod": "POST"
            |}
          """.stripMargin

        val expectedObject = RequestContext(
          "/{proxy+}",
          None,
          None,
          "test-invoke-stage",
          None,
          RequestContextIdentity(None, None, None, None, None, "sourceIP", None, None, None, None, None),
          "/{proxy+}",
          "POST",
          None
        )

        val json       = parse(providedJson).right.get
        val decodedObj = json.as[RequestContext].right.get

        assert(decodedObj == expectedObject)
      }
    }

    'APIGatewayProxyRequestNoBodyDecoding - {
      "all fields provided" - {
        val providedJson =
          """
            |{
            |    "resource": "/{proxy+}",
            |    "path": "/Seattle",
            |    "httpMethod": "POST",
            |    "headers": {
            |      "day": "Friday"
            |    },
            |    "queryStringParameters": {
            |      "time": "morning"
            |    },
            |    "pathParameters": {
            |      "proxy": "Seattle"
            |    },
            |    "stageVariables": {
            |      "varname": "varvalue"
            |    },
            |    "requestContext": {
            |      "path": "/{proxy+}",
            |      "stage": "test-invoke-stage",
            |      "identity": {
            |        "sourceIp": "sourceIP"
            |      },
            |      "resourcePath": "/{proxy+}",
            |      "httpMethod": "POST"
            |    },
            |    "isBase64Encoded": false
            |  }
          """.stripMargin

        val expectedObject = APIGatewayProxyRequestNoBody(
          "/{proxy+}",
          "/Seattle",
          "POST",
          Some(Map("day"     -> "Friday")),
          Some(Map("time"    -> "morning")),
          Some(Map("proxy"   -> "Seattle")),
          Some(Map("varname" -> "varvalue")),
          RequestContext(
            "/{proxy+}",
            None,
            None,
            "test-invoke-stage",
            None,
            RequestContextIdentity(None, None, None, None, None, "sourceIP", None, None, None, None, None),
            "/{proxy+}",
            "POST",
            None
          ),
          Some(false)
        )

        val json       = parse(providedJson).right.get
        val decodedObj = json.as[APIGatewayProxyRequestNoBody].right.get

        assert(decodedObj == expectedObject)
      }

      "required fields only with new fields" - {
        val providedJson =
          """
            |{
            |    "resource": "/{proxy+}",
            |    "path": "/Seattle",
            |    "httpMethod": "POST",
            |    "requestContext": {
            |      "path": "/{proxy+}",
            |      "stage": "test-invoke-stage",
            |      "identity": {
            |        "sourceIp": "sourceIP"
            |      },
            |      "resourcePath": "/{proxy+}",
            |      "httpMethod": "POST"
            |    },
            |    "someAwesomeNewFeature": "youveGottaHaveIt"
            |  }
          """.stripMargin

        val expectedObject = APIGatewayProxyRequestNoBody(
          "/{proxy+}",
          "/Seattle",
          "POST",
          None,
          None,
          None,
          None,
          RequestContext(
            "/{proxy+}",
            None,
            None,
            "test-invoke-stage",
            None,
            RequestContextIdentity(None, None, None, None, None, "sourceIP", None, None, None, None, None),
            "/{proxy+}",
            "POST",
            None
          ),
          None
        )

        val json       = parse(providedJson).right.get
        val decodedObj = json.as[APIGatewayProxyRequestNoBody].right.get

        assert(decodedObj == expectedObject)

      }
    }

    'APIGatewayProxyRequestWithTypedBody - {
      case class TypedBodyTest(callerName: String)

      object TypedBodyTest {
        implicit val decoder: Decoder[TypedBodyTest] = Decoder.forProduct1("callerName")(TypedBodyTest.apply)
      }

      "all fields provided" - {
        val providedJson =
          """
            |{
            |    "resource": "/{proxy+}",
            |    "path": "/Seattle",
            |    "httpMethod": "POST",
            |    "headers": {
            |      "day": "Friday"
            |    },
            |    "queryStringParameters": {
            |      "time": "morning"
            |    },
            |    "pathParameters": {
            |      "proxy": "Seattle"
            |    },
            |    "stageVariables": {
            |      "varname": "varvalue"
            |    },
            |    "requestContext": {
            |      "path": "/{proxy+}",
            |      "stage": "test-invoke-stage",
            |      "identity": {
            |        "sourceIp": "sourceIP"
            |      },
            |      "resourcePath": "/{proxy+}",
            |      "httpMethod": "POST"
            |    },
            |    "body": "{ \"callerName\": \"John\" }",
            |    "isBase64Encoded": false
            |  }
          """.stripMargin

        val expectedObject = APIGatewayProxyRequest[TypedBodyTest](
          "/{proxy+}",
          "/Seattle",
          "POST",
          Some(Map("day"     -> "Friday")),
          Some(Map("time"    -> "morning")),
          Some(Map("proxy"   -> "Seattle")),
          Some(Map("varname" -> "varvalue")),
          RequestContext(
            "/{proxy+}",
            None,
            None,
            "test-invoke-stage",
            None,
            RequestContextIdentity(None, None, None, None, None, "sourceIP", None, None, None, None, None),
            "/{proxy+}",
            "POST",
            None
          ),
          TypedBodyTest("John"),
          Some(false)
        )

        val json = parse(providedJson).right.get
        val decodedObj = json
          .as[APIGatewayProxyRequest[TypedBodyTest]](
            APIGatewayProxyRequest.decode[TypedBodyTest](TypedBodyTest.decoder))
          .right
          .get

        assert(decodedObj == expectedObject)
      }

      "required fields only with new fields" - {
        val providedJson =
          """
            |{
            |    "resource": "/{proxy+}",
            |    "path": "/Seattle",
            |    "httpMethod": "POST",
            |    "requestContext": {
            |      "path": "/{proxy+}",
            |      "stage": "test-invoke-stage",
            |      "identity": {
            |        "sourceIp": "sourceIP"
            |      },
            |      "resourcePath": "/{proxy+}",
            |      "httpMethod": "POST"
            |    },
            |    "body": "{ \"callerName\": \"John\" }",
            |    "someAwesomeNewFeature": "youveGottaHaveIt"
            |  }
          """.stripMargin

        val expectedObject = APIGatewayProxyRequest[TypedBodyTest](
          "/{proxy+}",
          "/Seattle",
          "POST",
          None,
          None,
          None,
          None,
          RequestContext(
            "/{proxy+}",
            None,
            None,
            "test-invoke-stage",
            None,
            RequestContextIdentity(None, None, None, None, None, "sourceIP", None, None, None, None, None),
            "/{proxy+}",
            "POST",
            None
          ),
          TypedBodyTest("John"),
          None
        )

        val json = parse(providedJson).right.get
        val decodedObj = json
          .as[APIGatewayProxyRequest[TypedBodyTest]](
            APIGatewayProxyRequest.decode[TypedBodyTest](TypedBodyTest.decoder))
          .right
          .get

        assert(decodedObj == expectedObject)
      }

      "return a DecodingFailure if the body field is not stringly JSON" - {
        val providedJson =
          """
            |{
            |    "resource": "/{proxy+}",
            |    "path": "/Seattle",
            |    "httpMethod": "POST",
            |    "requestContext": {
            |      "path": "/{proxy+}",
            |      "stage": "test-invoke-stage",
            |      "identity": {
            |        "sourceIp": "sourceIP"
            |      },
            |      "resourcePath": "/{proxy+}",
            |      "httpMethod": "POST"
            |    },
            |    "body": {
            |       "callerName": "John"
            |    }
            |  }
          """.stripMargin

        val json = parse(providedJson).right.get
        val decodingFailure = json
          .as[APIGatewayProxyRequest[TypedBodyTest]](
            APIGatewayProxyRequest.decode[TypedBodyTest](TypedBodyTest.decoder))
          .left
          .get

        assert(decodingFailure == DecodingFailure("String", List(DownField("body"))))
      }

      "return a DecodingFailure if the body field is a string, but not parsable to JSON" - {
        val providedJson =
          """
            |{
            |    "resource": "/{proxy+}",
            |    "path": "/Seattle",
            |    "httpMethod": "POST",
            |    "requestContext": {
            |      "path": "/{proxy+}",
            |      "stage": "test-invoke-stage",
            |      "identity": {
            |        "sourceIp": "sourceIP"
            |      },
            |      "resourcePath": "/{proxy+}",
            |      "httpMethod": "POST"
            |    },
            |    "body": "\"callerName\": \"John\""
            |  }
          """.stripMargin

        val json = parse(providedJson).right.get
        val decodingFailure = json
          .as[APIGatewayProxyRequest[TypedBodyTest]](
            APIGatewayProxyRequest.decode[TypedBodyTest](TypedBodyTest.decoder))
          .left
          .get

        assert(decodingFailure.message.startsWith("The request body must be a stringified JSON object. Parsing failed"))
        assert(decodingFailure.history == List(DownField("body")))
      }
    }

    'APIGatewayProxyResponse - {
      'EncodeResponseCorrectly - {
        case class MyResponseType(thing: String, numbers: Seq[Int])

        object MyResponseType {
          implicit val encoder: Encoder[MyResponseType] =
            Encoder.forProduct2("thing", "numbers")(x => (x.thing, x.numbers))
        }

        val myResponse = MyResponseType("big thing!", Seq(1, 10000, 100000000))

        val gatewayResponse =
          APIGatewayProxyResponse[MyResponseType](200,
                                                  Some(Map("custom-header" -> "my-awesome-header-value")),
                                                  Some(myResponse),
                                                  isBase64Encoded = false)

        val expectedJson =
          """{"statusCode":200,"headers":{"custom-header":"my-awesome-header-value"},"body":"{\"thing\":\"big thing!\",\"numbers\":[1,10000,100000000]}","isBase64Encoded":false}"""

        val json = encode(gatewayResponse)
        assert(json == expectedJson)
      }
    }
  }
}
