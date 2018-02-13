package com.lifeway.aws.lambda

import com.amazonaws.services.lambda.runtime.Context
import java.io.{InputStream, OutputStream}

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.slf4j.LoggerFactory

import scala.io.Source

/**
  * Abstract Lambda Handler for Scala. You must extend this class and implement the handler and invalidInput methods.
  * Additionally, you must provide a Circe decoder for your Input type and Circe encoders for your Failure type and
  * Success types.
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
                                    successEncoder: Encoder[S]) {
  implicit val reqDecoder: Decoder[APIGatewayProxyRequest[I]] = APIGatewayProxyRequest.decode[I](inputDecoder)
  private val logger                                          = LoggerFactory.getLogger("BASE_HANDLER")

  def handler(request: APIGatewayProxyRequest[I],
              c: Context): Either[APIGatewayProxyResponse[F], APIGatewayProxyResponse[S]]

  def invalidInput(circeError: Error): APIGatewayProxyResponse[F]

  final def handler(is: InputStream, os: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(is).mkString
    logger.debug(s"Lambda Proxy Input: $inputString")
    val input = decode[APIGatewayProxyRequest[I]](inputString)
    val outputString = input.fold(
      e => encode[F](invalidInput(e)),
      s =>
        handler(s, context).fold(
          hf => encode[F](hf),
          hs => encode[S](hs)
      )
    )
    logger.debug(s"Lambda Proxy Output: $outputString")
    os.write(outputString.getBytes)
    os.close()
  }

  private def encode[T](obj: APIGatewayProxyResponse[T])(implicit encoder: Encoder[T],
                                                         stringEncoder: Encoder[String]): String = {
    val bodyAsJsonString = obj.body.map(_.asJson.noSpaces)
    obj.copy[String](body = bodyAsJsonString).asJson(APIGatewayProxyResponse.encode[String]).noSpaces
  }
}
