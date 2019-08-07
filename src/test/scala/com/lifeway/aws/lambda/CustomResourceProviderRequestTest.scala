package com.lifeway.aws.lambda

import java.io.{ByteArrayOutputStream, OutputStream}

import utest._
import io.circe._
import io.circe.syntax._
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder}
import CustomResourceProvider._
import com.amazonaws.services.lambda.runtime.Context

object CustomResourceProviderRequestTest extends TestSuite with LambdaTestUtils {

  case class ResProp(key1: String, key2: Seq[String], key3: Map[String, String])

  object ResProp {
    implicit val decoder: Decoder[ResProp] =
      Decoder.forProduct3("key1", "key2", "key3")(ResProp.apply)
    implicit val encoder: Encoder[ResProp] =
      Encoder.forProduct3("key1", "key2", "key3")(ResProp.unapply(_).get)
  }

  val tests = Tests {

    implicit val requestDecoder: Decoder[Request]   = decodeRequest(ResProp.decoder)
    implicit val responseEncoder: Encoder[Response] = encodeResponse(ResProp.encoder)

    'RequestDecoding - {

      "invalid request type" - {

        val input = Json.obj(
          "RequestType"       -> "Bad".asJson,
          "RequestId"         -> "unique id for this create request".asJson,
          "ResponseURL"       -> "pre-signed-url-for-create-response".asJson,
          "ResourceType"      -> "Custom::MyCustomResourceType".asJson,
          "LogicalResourceId" -> "name of resource in template".asJson,
          "StackId"           -> "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid".asJson,
          "ResourceProperties" -> Json
            .obj(
              "key1" -> "string".asJson,
              "key2" -> Seq("list").asJson,
              "key3" -> Json.obj("key4" -> "map".asJson)
            )
        )

        val expectedError = Left(
          DecodingFailure(
            "Invalid request type",
            List(DownField("RequestType"))
          )
        )

        assert(input.as[CustomResourceProvider.Request] == expectedError)
      }

      "valid Create request" - {

        val expectedOutput = CreateRequest(
          requestID = "unique id for this create request",
          responseUrl = "pre-signed-url-for-create-response",
          resourceType = "Custom::MyCustomResourceType",
          logicalResourceID = "name of resource in template",
          stackID = "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
          resourceProperties = Some(
            ResProp(
              "string",
              Seq("list"),
              Map("key4" -> "map")
            )
          )
        )

        val input = Json.obj(
          "RequestType"        -> expectedOutput.requestType.asJson,
          "RequestId"          -> expectedOutput.requestID.asJson,
          "ResponseURL"        -> expectedOutput.responseUrl.asJson,
          "ResourceType"       -> expectedOutput.resourceType.asJson,
          "LogicalResourceId"  -> expectedOutput.logicalResourceID.asJson,
          "StackId"            -> expectedOutput.stackID.asJson,
          "ResourceProperties" -> expectedOutput.resourceProperties.asJson
        )

        assert(input.as[CustomResourceProvider.Request] == Right(expectedOutput))
      }

      "invalid Create request" - {

        val request = CreateRequest(
          requestID = "unique id for this create request",
          responseUrl = "pre-signed-url-for-create-response",
          resourceType = "Custom::MyCustomResourceType",
          logicalResourceID = "name of resource in template",
          stackID = "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
          resourceProperties = Some(
            ResProp(
              "string",
              Seq("list"),
              Map("key4" -> "map")
            )
          )
        )

        val input = Json.obj(
          "RequestType"       -> request.requestType.asJson,
          "RequestId"         -> request.requestID.asJson,
          "ResponseURL"       -> request.responseUrl.asJson,
          "ResourceType"      -> request.resourceType.asJson,
          "LogicalResourceId" -> request.logicalResourceID.asJson,
          "StackId"           -> request.stackID.asJson,
          "ResourceProperties" -> Json
            .obj(
              "key2" -> Seq("new-list").asJson,
              "key3" -> Json.obj("key4" -> "new-map".asJson)
            )
        )

        val expectedError = DecodingFailure(
          "Attempt to decode value on failed cursor",
          List(DownField("key1"), DownField("ResourceProperties"))
        )

        assert(input.as[CustomResourceProvider.Request] == Left(expectedError))
      }

      "valid Update request" - {

        val expectedOutput = UpdateRequest(
          requestID = "unique id for this update request",
          responseUrl = "pre-signed-url-for-update-response",
          resourceType = "Custom::MyCustomResourceType",
          logicalResourceID = "name of resource in template",
          stackID = "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
          physicalResourceID = "custom resource provider-defined physical id",
          resourceProperties = Some(
            ResProp(
              "new-string",
              Seq("new-list"),
              Map("key4" -> "new-map")
            )
          ),
          oldResourceProperties = Some(
            ResProp(
              "string",
              Seq("list"),
              Map("key4" -> "map")
            )
          )
        )

        val input = Json.obj(
          "RequestType"           -> expectedOutput.requestType.asJson,
          "RequestId"             -> expectedOutput.requestID.asJson,
          "ResponseURL"           -> expectedOutput.responseUrl.asJson,
          "ResourceType"          -> expectedOutput.resourceType.asJson,
          "LogicalResourceId"     -> expectedOutput.logicalResourceID.asJson,
          "StackId"               -> expectedOutput.stackID.asJson,
          "PhysicalResourceId"    -> expectedOutput.physicalResourceID.asJson,
          "ResourceProperties"    -> expectedOutput.resourceProperties.asJson,
          "OldResourceProperties" -> expectedOutput.oldResourceProperties.asJson
        )

        assert(input.as[CustomResourceProvider.Request] == Right(expectedOutput))
      }

      "invalid Update request" - {

        val input = Json.obj(
          "RequestType"       -> "Update".asJson,
          "RequestId"         -> "unique id for this update request".asJson,
          "ResponseURL"       -> "pre-signed-url-for-update-response".asJson,
          "ResourceType"      -> "Custom::MyCustomResourceType".asJson,
          "LogicalResourceId" -> "name of resource in template".asJson,
          "StackId"           -> "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid".asJson,
          "OldResourceProperties" -> Json.obj(
            "key1" -> "string".asJson,
            "key2" -> Seq("list").asJson,
            "key3" -> Json.obj("key4" -> "map".asJson)
          ),
          "ResourceProperties" -> Json.obj(
            "key1" -> "new-string".asJson,
            "key2" -> Seq("new-list").asJson,
            "key3" -> Json.obj("key4" -> "new-map".asJson)
          )
        )

        val expectedError = Left(
          DecodingFailure(
            "Attempt to decode value on failed cursor",
            List(DownField("PhysicalResourceId"))
          )
        )

        println()
        println(input.as[CustomResourceProvider.Request])
        println()

        assert(input.as[CustomResourceProvider.Request] == expectedError)
      }

      "valid Delete request" - {

        val expectedOutput = DeleteRequest(
          requestID = "unique id for this update request",
          responseUrl = "pre-signed-url-for-update-response",
          resourceType = "Custom::MyCustomResourceType",
          logicalResourceID = "name of resource in template",
          stackID = "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
          physicalResourceID = "custom resource provider-defined physical id",
          resourceProperties = Some(
            ResProp(
              "string",
              Seq("list"),
              Map("key4" -> "map")
            )
          )
        )

        val input = Json.obj(
          "RequestType"        -> expectedOutput.requestType.asJson,
          "RequestId"          -> expectedOutput.requestID.asJson,
          "ResponseURL"        -> expectedOutput.responseUrl.asJson,
          "ResourceType"       -> expectedOutput.resourceType.asJson,
          "LogicalResourceId"  -> expectedOutput.logicalResourceID.asJson,
          "StackId"            -> expectedOutput.stackID.asJson,
          "PhysicalResourceId" -> expectedOutput.physicalResourceID.asJson,
          "ResourceProperties" -> expectedOutput.resourceProperties.asJson
        )

        assert(input.as[CustomResourceProvider.Request] == Right(expectedOutput))
      }

      "invalid Delete request" - {

        val input = Json.obj(
          "RequestType"       -> "Delete".asJson,
          "RequestId"         -> "unique id for this delete request".asJson,
          "ResponseURL"       -> "pre-signed-url-for-delete-response".asJson,
          "ResourceType"      -> "Custom::MyCustomResourceType".asJson,
          "LogicalResourceId" -> "name of resource in template".asJson,
          "StackId"           -> "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid".asJson,
          "ResourceProperties" -> Json.obj(
            "key1" -> "string".asJson,
            "key2" -> Seq("list").asJson,
            "key3" -> Json.obj("key4" -> "map".asJson)
          )
        )

        val expectedError = Left(
          DecodingFailure(
            "Attempt to decode value on failed cursor",
            List(DownField("PhysicalResourceId"))
          )
        )

        assert(input.as[CustomResourceProvider.Request] == expectedError)
      }
    }

    'ResponseEncoding - {

      "Success[T]" - {

        val response = Success(
          requestID = "unique id for this update request",
          logicalResourceID = "name of resource in template",
          stackID = "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
          physicalResourceID = "custom resource provider-defined physical id",
          noEcho = true,
          reason = Some("Wooo!"),
          data = Some(
            ResProp(
              "string",
              Seq("list"),
              Map("key4" -> "map")
            )
          )
        )

        val json = Json.obj(
          "Status"             -> "SUCCESS".asJson,
          "Reason"             -> response.reason.asJson,
          "RequestId"          -> response.requestID.asJson,
          "LogicalResourceId"  -> response.logicalResourceID.asJson,
          "StackId"            -> response.stackID.asJson,
          "PhysicalResourceId" -> response.physicalResourceID.asJson,
          "NoEcho"             -> response.noEcho.asJson,
          "Data"               -> response.data.asJson
        )

        assert(response.asInstanceOf[Response].asJson == json)
      }

      "Failure" - {

        val response = Failure(
          requestID = "unique id for this update request",
          logicalResourceID = "name of resource in template",
          stackID = "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
          physicalResourceID = "custom resource provider-defined physical id",
          reason = "Required failure reason string"
        )

        val json = Json.obj(
          "Status"             -> "FAILED".asJson,
          "Reason"             -> response.reason.asJson,
          "RequestId"          -> response.requestID.asJson,
          "LogicalResourceId"  -> response.logicalResourceID.asJson,
          "StackId"            -> response.stackID.asJson,
          "PhysicalResourceId" -> response.physicalResourceID.asJson
        )

        assert(response.asInstanceOf[Response].asJson == json)
      }
    }

    'CustomResourceProvider - {

      "calls handler correctly" - {

        object Resource extends CustomResourceProvider[ResProp, ResProp] {

          override def handler(request: Request, context: Context): Response = {

            val req = request.asInstanceOf[CreateRequest[ResProp]]

            Success(
              req.requestID,
              req.stackID,
              req.logicalResourceID,
              physicalResourceID = context.getLogStreamName,
              data = req.resourceProperties
            )
          }
        }

        val inputString =
          """{
             |   "RequestType" : "Create",
             |   "RequestId" : "unique id for this create request",
             |   "ResponseURL" : "pre-signed-url-for-create-response",
             |   "ResourceType" : "Custom::MyCustomResourceType",
             |   "LogicalResourceId" : "name of resource in template",
             |   "StackId" : "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
             |   "ResourceProperties" : {
             |      "key1" : "string",
             |      "key2" : [ "list" ],
             |      "key3" : { "key4" : "map" }
             |   }
             |}
             |""".stripMargin

        val inputStream  = streamFromString(inputString)
        val outputStream = new ByteArrayOutputStream()
        val context      = makeContext()

        def writeToOutputStream(os: OutputStream, str: String): requests.Response = {
          os.write(str.getBytes)
          os.close()

          requests.Response("", 200, "", Map.empty, new requests.ResponseBlob("".getBytes), None)
        }

        CustomResourceProvider.handler(
          Resource.handler,
          (_, data) => writeToOutputStream(outputStream, data)
        )(
          Resource.baseLogger
        )(
          inputStream,
          outputStream,
          context
        )

        val expectedJsonOutput = parser
          .parse(
            s"""{
             |   "Status" : "SUCCESS",
             |   "RequestId" : "unique id for this create request",
             |   "LogicalResourceId" : "name of resource in template",
             |   "StackId" : "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
             |   "PhysicalResourceId" : "${context.getLogStreamName}",
             |   "NoEcho" : false,
             |   "Reason" : null,
             |   "Data" : {
             |      "key1" : "string",
             |      "key2" : [ "list" ],
             |      "key3" : { "key4" : "map" }
             |   }
             |}
             |""".stripMargin
          )
          .right
          .get

        val actualJsonOutput = parser.parse(outputStream.toString).right.get

        assert(actualJsonOutput == expectedJsonOutput)
      }

      "handles handler throwing an exception" - {

        object Resource extends CustomResourceProvider[ResProp, ResProp] {

          override def handler(request: Request, context: Context): Response =
            throw new Throwable("Handler did a bad, bad thing")
        }

        val inputString =
          """{
            |   "RequestType" : "Create",
            |   "RequestId" : "unique id for this create request",
            |   "ResponseURL" : "pre-signed-url-for-create-response",
            |   "ResourceType" : "Custom::MyCustomResourceType",
            |   "LogicalResourceId" : "name of resource in template",
            |   "StackId" : "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
            |   "ResourceProperties" : {
            |      "key1" : "string",
            |      "key2" : [ "list" ],
            |      "key3" : { "key4" : "map" }
            |   }
            |}
            |""".stripMargin

        val inputStream  = streamFromString(inputString)
        val outputStream = new ByteArrayOutputStream()
        val context      = makeContext()

        def writeToOutputStream(os: OutputStream, str: String): requests.Response = {
          os.write(str.getBytes)
          os.close()

          requests.Response("", 200, "", Map.empty, new requests.ResponseBlob("".getBytes), None)
        }

        CustomResourceProvider.handler(
          Resource.handler,
          (_, data) => writeToOutputStream(outputStream, data)
        )(
          Resource.baseLogger
        )(
          inputStream,
          outputStream,
          context
        )

        val expectedJsonOutput = parser
          .parse(
            s"""{
             |   "Status" : "FAILED",
             |   "RequestId" : "unique id for this create request",
             |   "LogicalResourceId" : "name of resource in template",
             |   "StackId" : "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid",
             |   "PhysicalResourceId" : "",
             |   "Reason" : "Handler did a bad, bad thing"
             |}
             |""".stripMargin
          )
          .right
          .get

        val actualJsonOutput = parser.parse(outputStream.toString).right.get

        assert(actualJsonOutput == expectedJsonOutput)
      }
    }
  }
}
