package com.lifeway.aws.lambda

import com.lifeway.aws.lambda.Errors
import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import com.lifeway.aws.lambda.Proxy.Response
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import utest._

object ProxyNoBodyTest extends TestSuite with ProxyEncoder with LambdaTestUtils {

  val tests = Tests {
    'ProxyNoBody - {
      'ValidInput - {
        val output              = new ByteArrayOutputStream()
        val handler             = new NoBodyTestHandleReturnSuccess
        val apiGatewayReq       = makeNoBodyReq
        val apiGatewayReqString = apiGatewayReq.asJson.toString()
        handler.handler(streamFromString(apiGatewayReqString), output, makeContext())

        val outputJson   = parse(output.toString).right.get
        val expectedJson = parse("""{
                                   |  "statusCode": 200,
                                   |  "headers": null,
                                   |  "body": "{\"data\":\"we have liftoff!\"}",
                                   |  "isBase64Encoded": false
                                   |}""".stripMargin).right.get

        assert(outputJson == expectedJson)
      }
      'HandlerGeneratedError - {
        val output              = new ByteArrayOutputStream()
        val handler             = new NoBodyTestHandleReturnError
        val apiGatewayReq       = makeNoBodyReq
        val apiGatewayReqString = apiGatewayReq.asJson.toString()
        handler.handler(streamFromString(apiGatewayReqString), output, makeContext())

        val outputJson   = parse(output.toString).right.get
        val expectedJson = parse("""{
                                   |  "statusCode": 500,
                                   |  "headers": null,
                                   |  "body": "{\"errorCode\":null,\"message\":\"boom!\"}",
                                   |  "isBase64Encoded": false
                                   |}""".stripMargin).right.get

        assert(outputJson == expectedJson)
      }
    }
  }
}

case class OutputDataTest(data: String)

object OutputDataTest {
  implicit val decoder: Decoder[OutputDataTest] = Decoder.forProduct1("data")(OutputDataTest.apply)
  implicit val encoder: Encoder[OutputDataTest] = Encoder.forProduct1("data")(x => x.data)
}

abstract class NoBodyTestHandle extends ProxyNoBody[Errors, OutputDataTest] {
  override def invalidInput(circeError: Error): APIGatewayProxyResponse[Errors] =
    APIGatewayProxyResponse(400, None, Some(InputError("bad data!")))
}

class NoBodyTestHandleReturnError extends NoBodyTestHandle {
  override def handler(request: APIGatewayProxyRequestNoBody, c: Context): Response[Errors, OutputDataTest] =
    Left(APIGatewayProxyResponse(500, None, Some(InputError("boom!"))))
}

class NoBodyTestHandleReturnSuccess extends NoBodyTestHandle {
  override def handler(request: APIGatewayProxyRequestNoBody, c: Context): Response[Errors, OutputDataTest] =
    Right(APIGatewayProxyResponse(200, None, Some(OutputDataTest("we have liftoff!"))))
}
