package com.lifeway.aws.lambda

import io.circe._
import io.circe.syntax._
import io.circe.CursorOp.DownField
import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

/**
  * Abstract Lambda Handler for Scala for Cloud Formation Custom Resource events. You must extend
  * this class and implement the handler method. Additionally, you must provide a Circe decoder for
  * your Input and Output types.
  *
  * @param inputDecoder - the Circe decoder for your input type
  * @param outputEncoder - the Circe encoder for your output type
  * @tparam Input - Input type, a Circe type
  * @tparam Output - Output type, a Circe type
  */
abstract class CustomResourceProvider[Input, Output](
    implicit inputDecoder: Decoder[Input],
    outputEncoder: Encoder[Output]
) {

  protected [lambda] val baseLogger: Logger = LoggerFactory.getLogger("BASE_HANDLER")

  implicit val requestDecoder: Decoder[CustomResourceProvider.Request] =
    CustomResourceProvider.decodeRequest(inputDecoder)

  implicit val responseEncoder: Encoder[CustomResourceProvider.Response] =
    CustomResourceProvider.encodeResponse(outputEncoder)

  def handler(request: CustomResourceProvider.Request, context: Context): CustomResourceProvider.Response

  final def handler(is: InputStream, os: OutputStream, context: Context): Unit = {

    val inputString = Source.fromInputStream(is).mkString

    baseLogger.debug(s"Lambda Proxy Input: $inputString")

    val input = parser.decode[CustomResourceProvider.Request](inputString).right.get

    val outputString: String = handler(input, context).asJson.noSpaces

    baseLogger.debug(s"Lambda Proxy Output: $outputString")

    os.write(outputString.getBytes)
    os.close()
  }
}

object CustomResourceProvider {

  object RequestTypes {

    val Create = "Create"
    val Update = "Update"
    val Delete = "Delete"
  }

  sealed trait Request

  case class CreateRequest[T](
      requestID: String,
      responseUrl: String,
      resourceType: String,
      logicalResourceID: String,
      stackID: String,
      resourceProperties: Option[T]
  ) extends Request {

    val requestType = RequestTypes.Create
  }

  case class UpdateRequest[T](
      requestID: String,
      responseUrl: String,
      resourceType: String,
      logicalResourceID: String,
      stackID: String,
      physicalResourceID: String,
      resourceProperties: Option[T],
      oldResourceProperties: Option[T]
  ) extends Request {

    val requestType = RequestTypes.Update
  }

  case class DeleteRequest[T](
      requestID: String,
      responseUrl: String,
      resourceType: String,
      logicalResourceID: String,
      stackID: String,
      physicalResourceID: String,
      resourceProperties: Option[T]
  ) extends Request {

    val requestType = RequestTypes.Delete
  }

  def decodeRequest[T](typeDecoder: Decoder[T]): Decoder[Request] = (c: HCursor) => {

    implicit val propDecoder: Decoder[T] = Decoder.decodeString
      .withErrorMessage("The request resource properties must be a stringified JSON object. Parsing failed.")
      .emap[T](
        jsonString =>
          parser
            .decode[T](jsonString)(typeDecoder)
            .fold(
              error => Left(error.getMessage),
              obj => Right(obj)
          )
      )

    val res = for {

      requestType           <- c.get[String]("RequestType")
      requestID             <- c.get[String]("RequestId")
      responseUrl           <- c.get[String]("ResponseURL")
      resourceType          <- c.get[String]("ResourceType")
      logicalResourceID     <- c.get[String]("LogicalResourceId")
      physicalResourceID    <- c.get[Option[String]]("PhysicalResourceId")
      stackID               <- c.get[String]("StackId")
      resourceProperties    <- c.get[Option[T]]("ResourceProperties")
      oldResourceProperties <- c.get[Option[T]]("OldResourceProperties")

    } yield {

      requestType match {

        case RequestTypes.Create =>
          Right(
            CreateRequest(
              requestID,
              responseUrl,
              resourceType,
              logicalResourceID,
              stackID,
              resourceProperties
            )
          )

        case RequestTypes.Update if physicalResourceID.isDefined =>
          Right(
            UpdateRequest(
              requestID,
              responseUrl,
              resourceType,
              logicalResourceID,
              stackID,
              physicalResourceID.get,
              resourceProperties,
              oldResourceProperties
            )
          )

        case RequestTypes.Update =>
          Left(
            DecodingFailure(
              "The PhysicalResourceId field is required for the Update RequestType",
              List(DownField("PhysicalResourceId"))
            )
          )

        case RequestTypes.Delete if physicalResourceID.isDefined =>
          Right(
            DeleteRequest(
              requestID,
              responseUrl,
              resourceType,
              logicalResourceID,
              stackID,
              physicalResourceID.get,
              resourceProperties
            )
          )

        case RequestTypes.Delete =>
          Left(
            DecodingFailure(
              "The PhysicalResourceId field is required for the Delete RequestType",
              List(DownField("PhysicalResourceId"))
            )
          )

        case other =>
          Left(
            DecodingFailure(
              s"Invalid request type ($other). Parsing failed.",
              List(DownField("RequestType"))
            )
          )
      }
    }

    // Flatten out the Either
    res.fold(
      df => Left(df),
      decoder => decoder
    )
  }

  sealed trait Response

  case class Success[T](
      physicalResourceID: String,
      stackID: String,
      requestID: String,
      logicalResourceID: String,
      noEcho: Boolean = false,
      reason: Option[String] = None,
      data: Option[T] = None
  ) extends Response {

    val status = "SUCCESS"
  }

  case class Failure(
      reason: String,
      physicalResourceID: String,
      stackID: String,
      requestID: String,
      logicalResourceID: String
  ) extends Response {

    val status = "SUCCESS"
  }

  def encodeSuccess[T](implicit typeEncoder: Encoder[T]): Encoder[Success[T]] =
    Encoder.forProduct8(
      "Status",
      "PhysicalResourceId",
      "StackId",
      "RequestId",
      "LogicalResourceId",
      "NoEcho",
      "Reason",
      "Data"
    )(
      s =>
        (
          s.status,
          s.physicalResourceID,
          s.stackID,
          s.requestID,
          s.logicalResourceID,
          s.noEcho,
          s.reason,
          s.data
      )
    )

  implicit val encodeFailure: Encoder[Failure] = Encoder.forProduct6(
    "Status",
    "Reason",
    "PhysicalResourceId",
    "StackId",
    "RequestId",
    "LogicalResourceId"
  )(
    f =>
      (
        f.status,
        f.reason,
        f.physicalResourceID,
        f.stackID,
        f.requestID,
        f.logicalResourceID
    )
  )

  def encodeResponse[T](implicit typeEncoder: Encoder[T]): Encoder[Response] = {

    case s: Success[T] => s.asJson(encodeSuccess)
    case f: Failure    => f.asJson(encodeFailure)
  }
}
