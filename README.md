AWS-Scala-Lambda-Handler
---

This is very small library purpose built to help you create Lambda functions with Scala and Proxied API Gateway Requests.

Inspiration for this library came from another Scala Lambda library https://github.com/mkotsur/aws-lambda-scala. The largest difference between these two libraries as of February 2018:

* AWS-Scala-Lambda-Handler ONLY supports Proxied API Gateway Requests.
* AWS-Scala-Lambda-Handler is built around a strong type system - no Exceptions should ever be thrown - they should be defined as types where the error types return valid HTTP responses for the API Gateway Proxy. We've included an abstract Errors class that you can use that is purpose built for this, or you can provide your own.

This library attempts to be as light as possible so as to keep your cold start times down.
* Circe is used for parsing, but using only custom encoders / decoders. Feel free to pull in more Circe libs for your project if you wish, but we don't require them.
* SLF4J API is used within the library on a DEBUG level for printing out the input / output of the lambda to the log. Be aware that this may leak sensitive data into your log if you enable a DEBUG log level in Production!
  * The name of this logger is "BASE_HANDLER". I recommend setting it to WARN or ERROR and only going down to DEBUG on it for local / DEV environments.
  * No logger implementation is provided - you pick. Given that you are using lambda, you may likely use the log4j2 handler from AWS. You will also need to pull in the log4j-slf4j-impl to have SLFJ4 logs from this library map over to log4j 

### Install
(TODO: once published to maven central)

### Using the library
There are two basic uses of the Library - for requests with a body (e.g. POST, PUT, DELETE) and requests without a body (e.g. GET, HEAD, OPTIONS)

The following examples used the TypedErrors classes, there is another variation of both of these that allow you to provide your own error type and implement one additional method for what the library should do when the input request could not be decoded into the input type.

#### Requests without a body

```scala
import com.amazonaws.services.lambda.runtime.Context
import com.lifeway.aws.lambda._

class Handler extends ProxyNoBodyTypedError[String] {
  override def handler(request: APIGatewayProxyRequestNoBody, c: Context): Proxy.Response[Errors, String] = {
    //TODO: your biz logic, response is Left / Right APIGatewayProxyResponse of either Errors type or String type.
    Right(APIGatewayProxyResponse(200, None, Some("My Successful String!")))
  }
}
```

#### Requests with a body

```scala
import com.amazonaws.services.lambda.runtime.Context
import com.lifeway.aws.lambda._
import io.circe.Decoder

case class InputType(data: String)

object InputType {
  implicit val decoder: Decoder[InputType] = Decoder.forProduct1("data")(InputType.apply)
}

class Handler extends ProxyWithBodyTypedErrors[InputType, String] {
  override def handler(request: APIGatewayProxyRequest[InputType], c: Context): Proxy.Response[Errors, String] = {
    //TODO: your biz logic, response is Left / Right APIGatewayProxyResponse of either Errors type or String type.
    val input = request.body //my input type decoded!
    Right(APIGatewayProxyResponse(200, None, Some("My Successful String!")))
  }
}
```


### Lambda Function Setup
For your handler, assuming your Handler implementation was `com.myorg.thing.MyHandler` then the Lambda Handler path would be `com.myorg.thing.MyHandler::handler`

TIP: SAM Local works great with this library. Assuming you are using `sbt assembly` to build your uber jar, the following would be a good starting point for a SAM template:

```yaml
#
# Deployment Stack
# Creates the API Gateway, Lambda, all roles, etc and maps them together.
#
---
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Parameters:
  Environment:
    Description: The app env - used primiarly for naming things.
    Type: String
    AllowedValues:
      - "dev"
      - "test"
      - "int"
      - "uat"
      - "stage"
      - "prod"

Resources:
  DemoFunction:
    Type: AWS::Serverless::Function
    Properties:
      Runtime: java8
      Handler: com.myorg.myservice:MyHandler::handler
      CodeUri: ./target/scala-2.12/myservice-assembly-1.1.jar
      MemorySize: 3008
      Timeout: 5
      Environment:
        Variables:
          LOG_LEVEL: WARN
      Events:
        RootRequest:
          Type: Api
          Properties:
            Path: /
            Method: get
            RestApiId: !Ref APIGateway  #Remove this is if you are doing automatic API Gateway setup.
      Tracing:  Active
      AutoPublishAlias: latest

  #
  # Manually defined API gateway instead of automatic (this is optional - based on your needs). You need this to setup authorization, etc on endpoints.
  #
  APIGateway:
    Type: AWS::Serverless::Api
    Properties:
      Name: !Sub "demo-${Environment}"
      StageName: "published"
      DefinitionBody:
        swagger: "2.0"
        info:
          title: !Sub "demo-${Environment}"
        paths:
          /:
            get:
              responses: {}
              x-amazon-apigateway-integration:
                uri: !Sub "arn:aws:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/${DemoFunction.Arn}:latest/invocations"
                passthroughBehavior: "when_no_match"
                httpMethod: "POST"
                type: "aws_proxy"
                timeoutInMillis: 5000
```