package com.smartelk.translator.remote

import akka.util.Timeout
import com.smartelk.translator.remote.HttpClient._
import com.smartelk.translator.remote.TokenProviderActor.{Token, TokenRequestMessage}
import scala.concurrent.Future
import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Try
import scala.xml.XML

private[translator] object RemoteServiceClient {

  trait RemoteServiceClient {
    def translate(r: TranslateRequest): Future[String]
  }

  class RemoteServiceClientImpl(val clientId: String, val clientSecret: String, val tokenProviderActor: ActorRef, val tokenRequestTimeoutMillis: Int, val httpClient: HttpClient) extends RemoteServiceClient {
    implicit val defaultAskTimeout = Timeout(tokenRequestTimeoutMillis.millis)

    def translate(r: TranslateRequest): Future[String] = {
      require(!r.text.isEmpty)
      require(!r.to.isEmpty)
      require(r.contentType.isEmpty || !r.contentType.get.isEmpty)
      require(r.category.isEmpty || !r.category.get.isEmpty)

     call(translateUri)(httpClient.get) {
        Seq("text" -> r.text, "to" -> r.to) ++: fromOption(r.from, "from") ++: fromOption(r.contentType, "contentType") ++: fromOption(r.category, "category")
      }.map(XML.loadString(_).text)
     }

    private def call(uri: String)(func: (String, KeyValueSeq, KeyValueSeq) => Try[Response])(params: KeyValueSeq): Future[String] = {
      (tokenProviderActor ? TokenRequestMessage).flatMap {
        case Token(accessToken, _) => {
          tryToFuture(func(uri, params, Seq("Authorization" -> ("Bearer " + accessToken)))).flatMap {
            case SuccessHttpResponse(result) => Future.successful(result)
            case ErrorHttpResponse(problem) => Future.failed(new RuntimeException(s"Remote service returned a problem: $problem"))
          }
        }
        case Status.Failure(e) => Future.failed(e)
      }
    }

    private def fromOption(op: Option[String], name: String): KeyValueSeq = op.map(f => Seq(name -> f)).getOrElse(Seq())
  }

  case class TranslateRequest(text: String, to: String, from: Option[String], contentType: Option[String], category: Option[String])
}

