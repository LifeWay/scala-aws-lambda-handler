package com.lifeway.aws.lambda

import com.amazonaws.services.lambda.runtime.Context

import io.circe
import io.circe._
import io.circe.parser._

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
abstract class Proxy[I, F, S](implicit inputDecoder: Decoder[I], failureEncoder: Encoder[F], successEncoder: Encoder[S])
    extends ProxyBase[F] {
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
  * Abstract Lambda Handler for Scala for API Gateway Proxied requests containing a JSON message body. You must extend
  * this class and implement the handler and invalidInput methods. Additionally, you must provide a Circe decoder for
  * your Input type and a Circe encoder for your Success type.
  *
  * @param inputDecoder - the Circe decoder for your input type
  * @param successEncoder - the Circe encoder for your success type
  * @tparam I - Input type, a Circe type
  * @tparam S - Success type, a Circe type
  */
abstract class ProxyTypedErrors[I, S](implicit inputDecoder: Decoder[I], successEncoder: Encoder[S])
    extends Proxy[I, Errors, S] {

  override def invalidInput(circeError: circe.Error): APIGatewayProxyResponse[Errors] =
    InputError(circeError.getMessage).toResponse
}
