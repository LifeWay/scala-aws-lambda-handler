package com.lifeway.aws.lambda

import io.circe.CursorOp.DownField
import io.circe._
import io.circe.parser._

case class RequestContextIdentity(
    cognitoIdentityPoolId: Option[String],
    accountId: Option[String],
    cognitoIdentityId: Option[String],
    caller: Option[String],
    apiKey: Option[String],
    sourceIp: String,
    cognitoAuthenticationType: Option[String],
    cognitoAuthenticationProvider: Option[String],
    userArn: Option[String],
    userAgent: Option[String],
    user: Option[String]
)

object RequestContextIdentity {
  implicit val decode: Decoder[RequestContextIdentity] =
    Decoder.forProduct11(
      "cognitoIdentityPoolId",
      "accountId",
      "cognitoIdentityId",
      "caller",
      "apiKey",
      "sourceIp",
      "cognitoAuthenticationType",
      "cognitoAuthenticationProvider",
      "userArn",
      "userAgent",
      "user"
    )(RequestContextIdentity.apply)
}

case class RequestContext(
    path: String,
    accountId: Option[Long],
    resourceId: Option[String],
    stage: String,
    requestId: Option[String],
    identity: RequestContextIdentity,
    resourcePath: String,
    httpMethod: String,
    apiId: Option[String]
)

object RequestContext {
  implicit val decode: Decoder[RequestContext] =
    Decoder.forProduct9(
      "path",
      "accountId",
      "resourceId",
      "stage",
      "requestId",
      "identity",
      "resourcePath",
      "httpMethod",
      "apiId"
    )(RequestContext.apply)
}

sealed trait APIGatewayProxyRequestBase {
  def resource: String
  def path: String
  def httpMethod: String
  def headers: Option[Map[String, String]]
  def queryStringParameters: Option[Map[String, String]]
  def pathParameters: Option[Map[String, String]]
  def stageVariables: Option[Map[String, String]]
  def requestContext: RequestContext
  def isBase64Encoded: Option[Boolean]
}

case class APIGatewayProxyRequestNoBody(
    resource: String,
    path: String,
    httpMethod: String,
    headers: Option[Map[String, String]],
    queryStringParameters: Option[Map[String, String]],
    pathParameters: Option[Map[String, String]],
    stageVariables: Option[Map[String, String]],
    requestContext: RequestContext,
    isBase64Encoded: Option[Boolean]
) extends APIGatewayProxyRequestBase

object APIGatewayProxyRequestNoBody {
  implicit val decode: Decoder[APIGatewayProxyRequestNoBody] =
    Decoder.forProduct9(
      "resource",
      "path",
      "httpMethod",
      "headers",
      "queryStringParameters",
      "pathParameters",
      "stageVariables",
      "requestContext",
      "isBase64Encoded"
    )(APIGatewayProxyRequestNoBody.apply)
}

case class APIGatewayProxyRequest[T](
    resource: String,
    path: String,
    httpMethod: String,
    headers: Option[Map[String, String]],
    queryStringParameters: Option[Map[String, String]],
    pathParameters: Option[Map[String, String]],
    stageVariables: Option[Map[String, String]],
    requestContext: RequestContext,
    body: T,
    isBase64Encoded: Option[Boolean]
) extends APIGatewayProxyRequestBase

object APIGatewayProxyRequest {
  def decode[T](typeDecoder: Decoder[T]): Decoder[APIGatewayProxyRequest[T]] = {
    implicit val bodyDecoder: Decoder[T] = Decoder.instance[T] { c: HCursor =>
      c.as[String].flatMap { stringlyJson =>
        parse(stringlyJson).fold(
          f =>
            Left(
              DecodingFailure(s"The request body must be a stringified JSON object. Parsing failed: ${f.message}",
                              List(DownField("body")))),
          s => s.as[T](typeDecoder)
        )
      }
    }

    Decoder.forProduct10(
      "resource",
      "path",
      "httpMethod",
      "headers",
      "queryStringParameters",
      "pathParameters",
      "stageVariables",
      "requestContext",
      "body",
      "isBase64Encoded"
    )(APIGatewayProxyRequest.apply)
  }
}

case class APIGatewayProxyResponse[T](
    statusCode: Int,
    headers: Option[Map[String, String]] = None,
    body: Option[T] = None,
    isBase64Encoded: Boolean = false
)

object APIGatewayProxyResponse {
  def encode[T](implicit typeEncoder: Encoder[T]): Encoder[APIGatewayProxyResponse[T]] =
    Encoder.forProduct4("statusCode", "headers", "body", "isBase64Encoded")(r =>
      (r.statusCode, r.headers, r.body, r.isBase64Encoded))
}
