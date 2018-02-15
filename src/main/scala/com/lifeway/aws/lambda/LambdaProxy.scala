package com.lifeway.aws.lambda

import com.amazonaws.services.lambda.runtime.Context
import java.io.{InputStream, OutputStream}

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.slf4j.LoggerFactory

import scala.io.Source

sealed trait LambdaProxyBase[F] {
  private[lambda] val baseLogger = LoggerFactory.getLogger("BASE_HANDLER")

  def handler(inputString: String, context: Context): String

  final def handler[T](is: InputStream, os: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(is).mkString
    baseLogger.debug(s"Lambda Proxy Input: $inputString")
    val outputString = handler(inputString, context)
    baseLogger.debug(s"Lambda Proxy Output: $outputString")
    os.write(outputString.getBytes)
    os.close()
  }

  def encode[T](obj: APIGatewayProxyResponse[T])(implicit encoder: Encoder[T],
                                                 stringEncoder: Encoder[String]): String = {
    val bodyAsJsonString = obj.body.map(_.asJson.noSpaces)
    obj.copy[String](body = bodyAsJsonString).asJson(APIGatewayProxyResponse.encode[String]).noSpaces
  }

  def invalidInput(circeError: Error): APIGatewayProxyResponse[F]
}

/**
  * Abstract Lambda Handler for Scala for API Gateway Proxied requests containing a JSON message body. You must extend
  * this class and implement the handler and invalidInput methods. Additionally, you must provide a Circe decoder for
  * your Input type and Circe encoders for your Failure type and Success types.
  *
  * @param inputDecoder - the Circe decoder for your input type
  * @param failureEncoder - the Circe encoder for your failure type
  * @param successEncoder - the Circe encoder for your success type
  * @tparam I - Input type, a Circe type
  * @tparam F - Failure type, a Circe type
  * @tparam S - Success type, a Circe type
  */
abstract class LambdaProxy[I, F, S](implicit inputDecoder: Decoder[I],
                                    failureEncoder: Encoder[F],
                                    successEncoder: Encoder[S])
    extends LambdaProxyBase[F] {
  implicit val reqDecoder: Decoder[APIGatewayProxyRequest[I]] = APIGatewayProxyRequest.decode[I](inputDecoder)

  def handler(request: APIGatewayProxyRequest[I], c: Context): LambdaProxy.Response[F, S]

  override final def handler(inputString: String, context: Context): String = {
    val input = decode[APIGatewayProxyRequest[I]](inputString)
    input.fold(
      e => encode[F](invalidInput(e)),
      s =>
        handler(s, context).fold(
          hf => encode[F](hf),
          hs => encode[S](hs)
      )
    )
  }
}

/**
  * Abstract Lambda Handler for Scala for API Gateway Proxied requests that don't have a JSON message body (e.g. a GET).
  * You must extend this class and implement the handler and invalidInput methods. Additionally, you must provide Circe
  * encoders for your Failure type and Success types.
  *
  * @param failureEncoder - the Circe encoder for your failure type
  * @param successEncoder - the Circe encoder for your success type
  * @tparam F - Failure type, a Circe type
  * @tparam S - Success type, a Circe type
  */
abstract class LambdaProxyNoReqBody[F, S](implicit
                                          failureEncoder: Encoder[F],
                                          successEncoder: Encoder[S])
    extends LambdaProxyBase[F] {

  def handler(request: APIGatewayProxyRequestNoBody, c: Context): LambdaProxy.Response[F, S]

  override final def handler(inputString: String, context: Context): String = {
    val input = decode[APIGatewayProxyRequestNoBody](inputString)
    input.fold(
      e => encode[F](invalidInput(e)),
      s =>
        handler(s, context).fold(
          hf => encode[F](hf),
          hs => encode[S](hs)
      )
    )
  }
}

object LambdaProxy {
  type Response[F, S] = Either[APIGatewayProxyResponse[F], APIGatewayProxyResponse[S]]
}
