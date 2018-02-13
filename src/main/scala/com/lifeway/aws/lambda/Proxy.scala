package com.lifeway.aws.lambda

import io.circe.{Decoder, Encoder}

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
    accountId: Option[Long],
    resourceId: Option[String],
    stage: String,
    requestId: Option[String],
    identity: Option[RequestContextIdentity],
    resourcePath: String,
    httpMethod: String,
    apiId: Option[String]
)

object RequestContext {
  implicit val decode: Decoder[RequestContext] =
    Decoder.forProduct8(
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

case class APIGatewayProxyRequest[T](
    resource: String,
    path: String,
    httpMethod: String,
    headers: Option[Map[String, String]],
    queryStringParameters: Option[Map[String, String]],
    pathParameters: Option[Map[String, String]],
    stageVariables: Option[Map[String, String]],
    requestContext: RequestContext,
    body: Option[T],
    isBase64Encoded: Option[Boolean]
)

object APIGatewayProxyRequest {
  def decode[T](implicit typeDecoder: Decoder[T]): Decoder[APIGatewayProxyRequest[T]] =
    Decoder.forProduct10("resource",
                         "path",
                         "httpMethod",
                         "headers",
                         "queryStringParameters",
                         "pathParameters",
                         "stageVariables",
                         "requestContext",
                         "body",
                         "isBase64Encoded")(APIGatewayProxyRequest.apply[T])
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
