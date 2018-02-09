package com.lifeway.aws.lambda

import io.circe.{Decoder, Encoder}

case class RequestContextIdentity(
    cognitoIdentityPoolId: Option[String] = None,
    accountId: Option[String] = None,
    cognitoIdentityId: Option[String] = None,
    caller: Option[String] = None,
    apiKey: Option[String] = None,
    sourceIp: String,
    cognitoAuthenticationType: Option[String] = None,
    cognitoAuthenticationProvider: Option[String] = None,
    userArn: Option[String] = None,
    userAgent: Option[String] = None,
    user: Option[String] = None
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
    identity: Option[RequestContextIdentity] = None,
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
    headers: Option[Map[String, String]] = None,
    queryStringParameters: Option[Map[String, String]] = None,
    pathParameters: Option[Map[String, String]] = None,
    stageVariables: Option[Map[String, String]] = None,
    requestContext: RequestContext,
    body: Option[T] = None,
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
