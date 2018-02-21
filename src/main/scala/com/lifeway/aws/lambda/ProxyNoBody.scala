package com.lifeway.aws.lambda

import com.amazonaws.services.lambda.runtime.Context
import io.circe
import io.circe.Encoder
import io.circe.parser.decode

/**
  * Abstract Lambda Handler for Scala for API Gateway Proxied requests that don't have a JSON message body (e.g. a GET).
  * You must extend this class and implement the handler and invalidInput methods. Additionally, you must provide a
  * Circe encoder for your Failure type and Success types.
  *
  * @param failureEncoder - the Circe encoder for your failure type
  * @param successEncoder - the Circe encoder for your success type
  * @tparam F - Failure type, a Circe type
  * @tparam S - Success type, a Circe type
  */
abstract class ProxyNoBody[F, S](implicit failureEncoder: Encoder[F], successEncoder: Encoder[S]) extends ProxyBase[F] {

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

/**
  * Abstract Lambda Handler for Scala for API Gateway Proxied requests that don't have a JSON message body (e.g. a GET).
  * You must extend this class and implement the handler and invalidInput methods. Additionally, you must provide a
  * Circe encoder for your Success type.
  *
  * All of your Error types must extend from the abstract Errors type.
  *
  * @param successEncoder - the Circe encoder for your success type
  * @tparam S - Success type, a Circe type
  */
abstract class ProxyNoBodyTypedError[S](implicit successEncoder: Encoder[S]) extends ProxyNoBody[Errors, S] {

  override def invalidInput(circeError: circe.Error): APIGatewayProxyResponse[Errors] =
    InputError(circeError.getMessage).toResponse
}
