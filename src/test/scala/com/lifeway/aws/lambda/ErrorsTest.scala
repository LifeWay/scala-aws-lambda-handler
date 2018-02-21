package com.lifeway.aws.lambda

import io.circe._
import io.circe.syntax._
import utest._

object ErrorsTest extends TestSuite with ProxyEncoder {

  val tests = Tests {
    'InputError - {
      val error    = InputError("my error")
      val response = error.toResponse

      'toResponse - {
        val expectedResponse = APIGatewayProxyResponse(400, None, Some(error))
        assert(response == expectedResponse)
      }

      "encoded JSON" - {
        val encodedJson = encode[Errors](response)
        val expectedJson =
          """{"statusCode":400,"headers":null,"body":"{\"errorCode\":null,\"message\":\"my error\"}","isBase64Encoded":false}"""

        assert(encodedJson == expectedJson)
      }
    }

    'ComplexErrorType - {
      val error    = ComplexErrorType(Seq(2, 3, 5, 7, 11, 13, 17, 19, 23, 29))
      val response = error.toResponse

      'toResponse - {
        val expectedResponse = APIGatewayProxyResponse(500, Some(Map("custom-header" -> "some-value")), Some(error))
        assert(response == expectedResponse)
      }

      "encoded JSON" - {
        val encodedJson = encode[Errors](response)
        val expectedJson =
          """{"statusCode":500,"headers":{"custom-header":"some-value"},"body":"{\"data\":{\"errorPoints\":[2,3,5,7,11,13,17,19,23,29]},\"errorCode\":1001,\"message\":\"ComplexError. See data points\"}","isBase64Encoded":false}"""

        assert(encodedJson == expectedJson)
      }
    }
  }
}

case class ComplexErrorType(errorPoints: Seq[Int]) extends Errors {
  val message: String                 = "ComplexError. See data points"
  override val httpStatus: Int        = 500
  override val headers                = Some(Map("custom-header" -> "some-value"))
  override val errorCode: Option[Int] = Some(1001)
  override val data: Option[Json]     = Some(this.asJson(Encoder.forProduct1("errorPoints")(u => u.errorPoints)))
}
