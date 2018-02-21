package com.lifeway.aws.lambda

import java.io.{ByteArrayInputStream, InputStream}

import com.amazonaws.services.lambda.runtime.{ClientContext, CognitoIdentity, Context, LambdaLogger}

trait LambdaTestUtils {
  val streamFromString: String => InputStream = x => new ByteArrayInputStream(x.getBytes)

  def makeContext(functionName: String = "unit-test-function",
                  timeRemaining: Int = 5000,
                  lambdaLogger: LambdaLogger = UnitTestLogger,
                  functionVersion: String = "unit-test-function-version",
                  memoryLimit: Int = 1024,
                  clientContext: ClientContext = null,
                  logStreamName: String = "/unit-test",
                  invokedFunctionArn: String = "unit-test-arn",
                  cognitoIdentity: CognitoIdentity = null,
                  logGroupName: String = "unit-test-group",
                  awsReqId: String = "unit-test-request-id"): Context =
    new Context {
      override def getFunctionName: String = functionName

      override def getRemainingTimeInMillis: Int = timeRemaining

      override def getLogger: LambdaLogger = lambdaLogger

      override def getFunctionVersion: String = functionVersion

      override def getMemoryLimitInMB: Int = memoryLimit

      override def getClientContext: ClientContext = clientContext

      override def getLogStreamName: String = logStreamName

      override def getInvokedFunctionArn: String = functionVersion

      override def getIdentity: CognitoIdentity = cognitoIdentity

      override def getLogGroupName: String = logGroupName

      override def getAwsRequestId: String = awsReqId
    }

  def makeRequest[T](body: T): APIGatewayProxyRequest[T] =
    APIGatewayProxyRequest(
      "/resource",
      "/unit-test",
      "POST",
      None,
      None,
      None,
      None,
      RequestContext("/path",
                     None,
                     None,
                     "unit",
                     None,
                     RequestContextIdentity(None, None, None, None, None, "127.0.0.1", None, None, None, None, None),
                     "/unit-test-path",
                     "POST",
                     None),
      body,
      None
    )

  def makeNoBodyReq: APIGatewayProxyRequestNoBody = APIGatewayProxyRequestNoBody(
    "/resource",
    "/unit-test",
    "POST",
    None,
    None,
    None,
    None,
    RequestContext("/path",
                   None,
                   None,
                   "unit",
                   None,
                   RequestContextIdentity(None, None, None, None, None, "127.0.0.1", None, None, None, None, None),
                   "/unit-test-path",
                   "POST",
                   None),
    None
  )
}

object UnitTestLogger extends LambdaLogger {
  override def log(message: String): Unit      = System.out.println(message)
  override def log(message: Array[Byte]): Unit = System.out.println(new String(message))
}
