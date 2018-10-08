package com.lifeway.aws.lambda

import com.amazonaws.services.lambda.runtime.Context
import java.io.{InputStream, OutputStream}

import io.circe._
import io.circe.syntax._
import org.slf4j.LoggerFactory

import scala.io.Source

trait Proxy[F] extends ProxyEncoder {
  private[lambda] val baseLogger = LoggerFactory.getLogger("BASE_HANDLER")

  def handler(inputString: String, context: Context): String

  final def handler[T](is: InputStream, os: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(is).mkString

    baseLogger.debug(s"Lambda Proxy Input: $inputString")

    val outputString = Proxy.checkForWarmer(inputString).fold(
      handler(inputString, context)
    )(
      warmer => "ACK"
    )

    baseLogger.debug(s"Lambda Proxy Output: $outputString")

    os.write(outputString.getBytes)
    os.close()
  }

  def invalidInput(circeError: Error): APIGatewayProxyResponse[F]
}

object Proxy {

  type Response[F, S] = Either[APIGatewayProxyResponse[F], APIGatewayProxyResponse[S]]

  val WARMER_KEY = "X-LAMBDA-WARMER"

  def checkForWarmer(input: String): Option[Unit] = parser.parse(input).toOption.flatMap(
    json => json.hcursor.get[Boolean](WARMER_KEY).getOrElse(false) match {
      case true => Some(())
      case false => None
    }
  )
}

trait ProxyEncoder {
  def encode[T](obj: APIGatewayProxyResponse[T])(implicit encoder: Encoder[T],
                                                 stringEncoder: Encoder[String]): String = {
    val bodyAsJsonString = obj.body.map(_.asJson.noSpaces)
    obj.copy[String](body = bodyAsJsonString).asJson(APIGatewayProxyResponse.encode[String]).noSpaces
  }
}
