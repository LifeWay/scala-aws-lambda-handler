package com.lifeway.aws.lambda

import com.lifeway.aws.lambda.Errors
import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import com.lifeway.aws.lambda.Proxy.Response
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import utest._

object ProxyWithBodyTest extends TestSuite with ProxyEncoder with LambdaTestUtils {

  val tests = Tests {
    'ProxyWithBody - {
      'ValidInput - {
        val output        = new ByteArrayOutputStream()
        val handler       = new WithBodyTestHandleReturnSuccess
        val apiGatewayReq = makeRequest[InputDataTest](InputDataTest("input data!"))
        val apiGatewayReqString =
          apiGatewayReq.asJson(APIGatewayProxyRequest.encode[InputDataTest](InputDataTest.encoder)).toString()
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
      'InvalidInput - {
        val output        = new ByteArrayOutputStream()
        val handler       = new WithBodyTestHandleReturnSuccess
        val apiGatewayReq = makeRequest[BadInputData](BadInputData("input data!"))
        val apiGatewayReqString =
          apiGatewayReq.asJson(APIGatewayProxyRequest.encode[BadInputData](BadInputData.encoder)).toString()
        handler.handler(streamFromString(apiGatewayReqString), output, makeContext())

        val outputJson   = parse(output.toString).right.get
        val expectedJson = parse("""{
                                   |  "statusCode": 400,
                                   |  "headers": null,
                                   |  "body": "{\"errorCode\":null,\"message\":\"bad data!\"}",
                                   |  "isBase64Encoded": false
                                   |}""".stripMargin).right.get

        assert(outputJson == expectedJson)
      }
      'HandlerGeneratedError - {
        val output        = new ByteArrayOutputStream()
        val handler       = new WithBodyTestHandleReturnError
        val apiGatewayReq = makeRequest[InputDataTest](InputDataTest("input data!"))
        val apiGatewayReqString =
          apiGatewayReq.asJson(APIGatewayProxyRequest.encode[InputDataTest](InputDataTest.encoder)).toString()
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

case class InputDataTest(data: String)

object InputDataTest {
  implicit val decoder: Decoder[InputDataTest] = Decoder.forProduct1("data")(InputDataTest.apply)
  implicit val encoder: Encoder[InputDataTest] = Encoder.forProduct1("data")(x => x.data)
}

case class BadInputData(anotherThing: String)

object BadInputData {
  implicit val decoder: Decoder[BadInputData] = Decoder.forProduct1("anotherThing")(BadInputData.apply)
  implicit val encoder: Encoder[BadInputData] = Encoder.forProduct1("anotherThing")(x => x.anotherThing)
}

abstract class WithBodyTestHandle extends ProxyWithBody[InputDataTest, Errors, InputDataTest] {
  override def invalidInput(circeError: Error): APIGatewayProxyResponse[Errors] =
    APIGatewayProxyResponse(400, None, Some(InputError("bad data!")))
}

class WithBodyTestHandleReturnError extends WithBodyTestHandle {
  override def handler(request: APIGatewayProxyRequest[InputDataTest], c: Context): Response[Errors, InputDataTest] =
    Left(APIGatewayProxyResponse(500, None, Some(InputError("boom!"))))
}

class WithBodyTestHandleReturnSuccess extends WithBodyTestHandle {
  override def handler(request: APIGatewayProxyRequest[InputDataTest], c: Context): Response[Errors, InputDataTest] =
    Right(APIGatewayProxyResponse(200, None, Some(InputDataTest("we have liftoff!"))))
}
