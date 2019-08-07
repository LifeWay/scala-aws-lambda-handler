package com.lifeway.aws.lambda

import io.circe._
import io.circe.syntax._
import io.circe.CursorOp.DownField

import scala.io.Source
import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.Context
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

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

  protected[lambda] val baseLogger: Logger = LoggerFactory.getLogger("BASE_HANDLER")

  implicit val requestDecoder: Decoder[CustomResourceProvider.Request] =
    CustomResourceProvider.decodeRequest(inputDecoder)

  implicit val responseEncoder: Encoder[CustomResourceProvider.Response] =
    CustomResourceProvider.encodeResponse(outputEncoder)

  def handler(request: CustomResourceProvider.Request, context: Context): CustomResourceProvider.Response

  // $COVERAGE-OFF$
  final def handler(is: InputStream, os: OutputStream, context: Context): Unit =
    CustomResourceProvider.handler(
      handler,
      (url, data) => requests.put(url = url, data = data)
    )(
      baseLogger
    )(
      is,
      os,
      context
    )
  // $COVERAGE-ON$
}

object CustomResourceProvider {

  def handler(
      handler: (Request, Context) => Response,
      putRequest: (String, String) => requests.Response
  )(
      baseLogger: Logger
  )(
      is: InputStream,
      os: OutputStream,
      context: Context
  )(
      implicit requestDecoder: Decoder[Request],
      responseEncoder: Encoder[Response]
  ): Unit = {

    val inputString = Source.fromInputStream(is).mkString

    baseLogger.info(s"Lambda Custom Resource Input: $inputString")

    parser
      .decode[Request](inputString)
      .fold(
        error => baseLogger.error("Unable to decode lambda input", error),
        input => {

          val output = Try(handler(input, context)).fold(
            error => input.toFailure(error.getMessage),
            identity
          )

          val outputString = output.asJson.noSpaces
          baseLogger.info(s"Lambda Custom Resource Output: ${output.asJson.spaces4}")

          val response = putRequest(
            input.responseUrl,
            outputString
          )

          baseLogger.info("Response status code: {}}", response.statusCode)
        }
      )
  }

  object RequestTypes {

    val Create = "Create"
    val Update = "Update"
    val Delete = "Delete"
  }

  sealed trait Request {

    def responseUrl: String

    def toSuccess[U](noEcho: Boolean, reason: Option[String], data: Option[U]): Response
    def toFailure(reason: String): Response
  }

  case class CreateRequest[T](
      requestID: String,
      responseUrl: String,
      resourceType: String,
      logicalResourceID: String,
      stackID: String,
      resourceProperties: Option[T]
  ) extends Request {

    val requestType = RequestTypes.Create

    def toSuccess[U](
        physicalResourceID: String,
        noEcho: Boolean = false,
        reason: Option[String] = None,
        data: Option[U] = None
    ): Response = Success(
      requestID,
      stackID,
      logicalResourceID,
      physicalResourceID,
      noEcho,
      reason,
      data
    )

    override def toSuccess[U](noEcho: Boolean, reason: Option[String], data: Option[U]): Response =
      toSuccess("", noEcho, reason, data)

    def toFailure(reason: String): Response = toFailure(reason, "")
    def toFailure(reason: String, physicalResourceID: String): Response = Failure(
      reason,
      requestID,
      stackID,
      logicalResourceID,
      physicalResourceID
    )
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

    def toSuccess[U](
        noEcho: Boolean = false,
        reason: Option[String] = None,
        data: Option[U] = None
    ): Response = Success(
      requestID,
      stackID,
      logicalResourceID,
      physicalResourceID,
      noEcho,
      reason,
      data
    )

    def toFailure(reason: String): Response = Failure(
      reason,
      requestID,
      stackID,
      logicalResourceID,
      physicalResourceID
    )
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

    def toSuccess[U](
        noEcho: Boolean = false,
        reason: Option[String] = None,
        data: Option[U] = None
    ): Response = Success(
      requestID,
      stackID,
      logicalResourceID,
      physicalResourceID,
      noEcho,
      reason,
      data
    )

    def toFailure(reason: String): Response = Failure(
      reason,
      requestID,
      stackID,
      logicalResourceID,
      physicalResourceID
    )
  }

  def decodeRequest[T](implicit typeDecoder: Decoder[T]): Decoder[Request] = {

    val InvalidRequestTypeFailure = DecodingFailure(
      s"Invalid request type",
      List(DownField("RequestType"))
    )

    def checkRequestType(expectedRequestType: String)(c: HCursor): Either[DecodingFailure, String] =
      c.get[String]("RequestType")
        .filterOrElse(
          rt => rt == expectedRequestType,
          InvalidRequestTypeFailure
        )

    val decodeCreateRequest: Decoder[Request] = (c: HCursor) =>
      for {

        _                  <- checkRequestType(RequestTypes.Create)(c)
        requestID          <- c.get[String]("RequestId")
        responseUrl        <- c.get[String]("ResponseURL")
        resourceType       <- c.get[String]("ResourceType")
        logicalResourceID  <- c.get[String]("LogicalResourceId")
        stackID            <- c.get[String]("StackId")
        resourceProperties <- c.get[Option[T]]("ResourceProperties")

      } yield {

        CreateRequest(
          requestID,
          responseUrl,
          resourceType,
          logicalResourceID,
          stackID,
          resourceProperties
        )
    }

    val decodeUpdateRequest: Decoder[Request] = (c: HCursor) =>
      for {

        _                     <- checkRequestType(RequestTypes.Update)(c)
        requestID             <- c.get[String]("RequestId")
        responseUrl           <- c.get[String]("ResponseURL")
        resourceType          <- c.get[String]("ResourceType")
        logicalResourceID     <- c.get[String]("LogicalResourceId")
        physicalResourceID    <- c.get[String]("PhysicalResourceId")
        stackID               <- c.get[String]("StackId")
        resourceProperties    <- c.get[Option[T]]("ResourceProperties")
        oldResourceProperties <- c.get[Option[T]]("OldResourceProperties")

      } yield {

        UpdateRequest(
          requestID,
          responseUrl,
          resourceType,
          logicalResourceID,
          stackID,
          physicalResourceID,
          resourceProperties,
          oldResourceProperties
        )
    }

    val decodeDeleteRequest: Decoder[Request] = (c: HCursor) =>
      for {

        _                  <- checkRequestType(RequestTypes.Delete)(c)
        requestID          <- c.get[String]("RequestId")
        responseUrl        <- c.get[String]("ResponseURL")
        resourceType       <- c.get[String]("ResourceType")
        logicalResourceID  <- c.get[String]("LogicalResourceId")
        physicalResourceID <- c.get[String]("PhysicalResourceId")
        stackID            <- c.get[String]("StackId")
        resourceProperties <- c.get[Option[T]]("ResourceProperties")

      } yield {

        DeleteRequest(
          requestID,
          responseUrl,
          resourceType,
          logicalResourceID,
          stackID,
          physicalResourceID,
          resourceProperties
        )
    }

    val decoders = Seq(decodeCreateRequest, decodeUpdateRequest, decodeDeleteRequest)

    decoders.tail.foldLeft(decoders.head)(
      (acc, decoder) =>
        acc.handleErrorWith {

          case InvalidRequestTypeFailure => decoder
          case df                        => Decoder.failed(df)
      }
    )
  }

  sealed trait Response

  case class Success[T](
      requestID: String,
      stackID: String,
      logicalResourceID: String,
      physicalResourceID: String,
      noEcho: Boolean = false,
      reason: Option[String] = None,
      data: Option[T] = None
  ) extends Response {

    val status = "SUCCESS"
  }

  case class Failure(
      reason: String,
      requestID: String,
      stackID: String,
      logicalResourceID: String,
      physicalResourceID: String
  ) extends Response {

    val status = "FAILED"
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
