package com.lifeway.aws.lambda
import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import io.circe
import utest._

object ProxyTest extends TestSuite with LambdaTestUtils {

  val tests = Tests {

    'Proxy - {

      'ValidWarmerTrue - {

        val output = new ByteArrayOutputStream()
        val input = """{ "X-LAMBDA-WARMER" : true }"""
        val proxy = new TestHandler

        proxy.handler(streamFromString(input), output, makeContext())

        assert(output.toString == "ACK")
      }

      'ValidWarmerFalse - {

        val output = new ByteArrayOutputStream()
        val input = """{ "X-LAMBDA-WARMER" : false }"""
        val proxy = new TestHandler

        proxy.handler(streamFromString(input), output, makeContext())

        assert(output.toString == "bad")
      }

      'ValidWarmerMissing - {

        val output = new ByteArrayOutputStream()
        val input = """{ "other_key" : false }"""
        val proxy = new TestHandler

        proxy.handler(streamFromString(input), output, makeContext())

        assert(output.toString == "bad")
      }

      'InvalidJSON - {

        val output = new ByteArrayOutputStream()
        val input = """} This is not valid JSON {"""
        val proxy = new TestHandler

        proxy.handler(streamFromString(input), output, makeContext())

        assert(output.toString == "bad")
      }
    }
  }
}

class TestHandler extends Proxy[Errors] {

  override def handler(inputString: String, context: Context): String = "bad"

  override def invalidInput(circeError: circe.Error): APIGatewayProxyResponse[Errors] =
    APIGatewayProxyResponse[Errors](400)

}
