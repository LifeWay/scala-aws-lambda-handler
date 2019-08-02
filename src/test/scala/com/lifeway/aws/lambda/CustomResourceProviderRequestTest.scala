package com.lifeway.aws.lambda

import utest._

import io.circe._
import io.circe.syntax._
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder}

import CustomResourceProvider._

object CustomResourceProviderRequestTest extends TestSuite {

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
            .noSpaces
            .asJson
        )

        val expectedError = Left(
          DecodingFailure(
            "Invalid request type (Bad). Parsing failed.",
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
          "ResourceProperties" -> expectedOutput.resourceProperties.asJson.noSpaces.asJson
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
            .noSpaces
            .asJson
        )

        val expectedError = DecodingFailure(
          "Attempt to decode value on failed cursor: DownField(key1)",
          List(DownField("ResourceProperties"))
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
          "ResourceProperties"    -> expectedOutput.resourceProperties.asJson.noSpaces.asJson,
          "OldResourceProperties" -> expectedOutput.oldResourceProperties.asJson.noSpaces.asJson
        )

        assert(input.as[CustomResourceProvider.Request] == Right(expectedOutput))
      }

      "invalid Update request" - {

        val input = Json.obj(
          "RequestType"       -> "Update".asJson,
          "RequestId"         -> "unique id for this create request".asJson,
          "ResponseURL"       -> "pre-signed-url-for-create-response".asJson,
          "ResourceType"      -> "Custom::MyCustomResourceType".asJson,
          "LogicalResourceId" -> "name of resource in template".asJson,
          "StackId"           -> "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid".asJson,
          "OldResourceProperties" -> Json
            .obj(
              "key1" -> "string".asJson,
              "key2" -> Seq("list").asJson,
              "key3" -> Json.obj("key4" -> "map".asJson)
            )
            .noSpaces
            .asJson,
          "ResourceProperties" -> Json
            .obj(
              "key1" -> "new-string".asJson,
              "key2" -> Seq("new-list").asJson,
              "key3" -> Json.obj("key4" -> "new-map".asJson)
            )
            .noSpaces
            .asJson
        )

        val expectedError = Left(
          DecodingFailure(
            "The PhysicalResourceId field is required for the Update RequestType",
            List(DownField("PhysicalResourceId"))
          )
        )

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
          "ResourceProperties" -> expectedOutput.resourceProperties.asJson.noSpaces.asJson
        )

        assert(input.as[CustomResourceProvider.Request] == Right(expectedOutput))
      }

      "invalid Delete request" - {

        val input = Json.obj(
          "RequestType"       -> "Delete".asJson,
          "RequestId"         -> "unique id for this create request".asJson,
          "ResponseURL"       -> "pre-signed-url-for-create-response".asJson,
          "ResourceType"      -> "Custom::MyCustomResourceType".asJson,
          "LogicalResourceId" -> "name of resource in template".asJson,
          "StackId"           -> "arn:aws:cloudformation:us-east-2:namespace:stack/stack-name/guid".asJson,
          "OldResourceProperties" -> Json
            .obj(
              "key1" -> "string".asJson,
              "key2" -> Seq("list").asJson,
              "key3" -> Json.obj("key4" -> "map".asJson)
            )
            .noSpaces
            .asJson,
          "ResourceProperties" -> Json
            .obj(
              "key1" -> "new-string".asJson,
              "key2" -> Seq("new-list").asJson,
              "key3" -> Json.obj("key4" -> "new-map".asJson)
            )
            .noSpaces
            .asJson
        )

        val expectedError = Left(
          DecodingFailure(
            "The PhysicalResourceId field is required for the Delete RequestType",
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
          "Status"             -> response.status.asJson,
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
          "Status"             -> response.status.asJson,
          "Reason"             -> response.reason.asJson,
          "RequestId"          -> response.requestID.asJson,
          "LogicalResourceId"  -> response.logicalResourceID.asJson,
          "StackId"            -> response.stackID.asJson,
          "PhysicalResourceId" -> response.physicalResourceID.asJson
        )

        assert(response.asInstanceOf[Response].asJson == json)
      }
    }
  }
}
