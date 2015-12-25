package com.smartelk.translator.remote

import akka.actor.{Status, Actor}
import com.smartelk.translator.remote.HttpClient._
import org.json4s.native.JsonMethods._
import scala.util.{Failure, Success, Try}

private[translator] object TokenProviderActor {

  class TokenProviderActor(clientId: String, clientSecret: String, httpClient: HttpClient) extends Actor {
    private var token = Token("", 0L)
    def getCurrentTimeMillis = System.currentTimeMillis()

    private def getToken(): Try[Token] =  {
      val now = getCurrentTimeMillis
      if (now > token.expiresMillis) {
        for {
          response <- httpClient.post(HttpClientBasicRequest(requestAccessTokenUri), Seq(
              "grant_type" -> "client_credentials",
              "client_id" -> clientId,
              "client_secret" -> clientSecret,
              "scope" -> "http://api.microsofttranslator.com"))
          newToken <- response match {
            case SuccessHttpResponse(value) => Try {
              val newTokenJson = parse(value)
              val accessToken = (newTokenJson \ "access_token").extract[String]
              val expiresIn = (newTokenJson \ "expires_in").extract[Long]
              Token(accessToken, now + expiresIn)
            }
            case ErrorHttpResponse(problem) => Failure(new RuntimeException(s"Remote service returned a problem: $problem"))
          }
        } yield newToken
      }
      else {
        Success(token)
      }
    }

    override def receive: Receive = {
      case TokenRequestMessage => getToken() match {
        case Success(gottenToken) =>{
          token = gottenToken
          sender ! token
        }
        case Failure(e) =>  sender ! Status.Failure(e)
      }
    }
  }

  case class Token(accessToken: String, expiresMillis: Long)
  case object TokenRequestMessage
}

