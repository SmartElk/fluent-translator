package com.smartelk.fluent.translator.microsoft.remote

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.testkit.TestKit
import com.smartelk.fluent.translator.Dsl.{MaxQuality, `audio/wav`, `text/plain`}
import com.smartelk.fluent.translator.basic.HttpClient.{HttpClient, _}
import com.smartelk.fluent.translator.microsoft.remote.MicrosoftRemoteServiceClient._
import com.smartelk.fluent.translator.microsoft.remote.MicrosoftTokenProviderActor.{Token, TokenRequestMessage}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import scala.concurrent.Future

class MicrosoftRemoteServiceClientSpec(system: ActorSystem) extends TestKit(system) with WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  def this() = this(ActorSystem("test"))

  val httpClient = mock[HttpClient]

  override def beforeEach() = {reset(httpClient)}
  override def afterAll {TestKit.shutdownActorSystem(system)}
  implicit override val patienceConfig  = PatienceConfig(timeout = Span(500, Millis))

  "Translating" when {
    "token provider returns failure" should {
      "fail with that failure" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Status.Failure(new RuntimeException("Bad!"))}
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        //act
        whenReady(client.translate(TranslateRequest("bla", "fr", None, None, None)).failed) {res =>

          //assert
          res.getMessage should be ("Bad!")
        }
      }
    }

    "gets an exception during calling a remote method" should {
      "fail with that exception" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)}
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        when(httpClient.get[String](HttpClientBasicRequest(translateUri, Seq("text"-> "bla", "to"-> "fr", "from"->"en", "contentType" -> `text/plain`.toString, "category"-> "default"), Seq("Authorization"->"Bearer 111aaa")))).thenReturn(Future.failed(new RuntimeException("Can't connect")))

        //act
        whenReady(client.translate(TranslateRequest("bla", "fr", Some("en"), Some(`text/plain`), Some("default"))).failed) {res =>

          //assert
          res.getMessage should be ("Can't connect")
        }
      }
    }

    "everything is fine" should {
      "get a translation" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)
          }
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        when(httpClient.get[String](HttpClientBasicRequest(translateUri, Seq("text" -> "blabla", "to" -> "fr"), Seq("Authorization" -> "Bearer 111aaa")))).thenReturn(Future.successful(200, "<string>albalb</string>"))

        //act
        whenReady(client.translate(TranslateRequest("blabla", "fr", None, None, None))) { res =>

          //assert
          res should be ("albalb")
        }
      }
    }
  }

  "Getting translations" when {
    "token provider returns failure" should {
      "fail with that failure" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Status.Failure(new RuntimeException("Bad!"))
          }
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        //act
        whenReady(client.getTranslations(GetTranslationsRequest("blabla", 10, "en", "fr", None)).failed) { res =>

          //assert
          res.getMessage should be("Bad!")
        }
      }
    }

    "gets an exception during calling a remote method" should {
      "fail with that exception" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)}
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        val gonnaBeBody = xml.Utility.trim(
          <TranslateOptions xmlns="http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2">
            <Category>super</Category>
            <ContentType></ContentType>
            <ReservedFlags></ReservedFlags>
            <State></State>
            <Uri></Uri>
            <User></User>
          </TranslateOptions>).buildString(true)

        when(httpClient.post[String](HttpClientBasicRequest(getTranslationsUri, Seq("text" -> "bla", "from"-> "en", "to" -> "fr", "maxTranslations" -> "10"), Seq("Authorization"->"Bearer 111aaa", "Content-Type" -> "text/xml")), gonnaBeBody)).thenReturn(Future.failed(new RuntimeException("Can't connect")))

        //act
        whenReady(client.getTranslations(GetTranslationsRequest("bla", 10, "en", "fr", Some("super"))).failed) {res =>

          //assert
          res.getMessage should be ("Can't connect")
        }
      }
    }

    "gets 200 from a remote method but gotten XML is invalid" should {
      "fail with an exception" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)
          }
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        val gonnaBeBody = xml.Utility.trim(
          <TranslateOptions xmlns="http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2">
            <Category>{None.getOrElse("")}</Category>
            <ContentType></ContentType>
            <ReservedFlags></ReservedFlags>
            <State></State>
            <Uri></Uri>
            <User></User>
          </TranslateOptions>).buildString(true)

        val gonnaReturn = xml.Utility.trim(<bad>very bad</bad>).buildString(true)

        when(httpClient.post[String](HttpClientBasicRequest(getTranslationsUri, Seq("text" -> "bla", "from"-> "en", "to" -> "fr", "maxTranslations" -> "10"), Seq("Authorization"->"Bearer 111aaa", "Content-Type" -> "text/xml")), gonnaBeBody)).thenReturn(Future.successful(200, gonnaReturn))

        //act
        whenReady(client.getTranslations(GetTranslationsRequest("bla", 10, "en", "fr", None)).failed) { res =>

          //assert
          res.getMessage should be("Remote service returned bad XML: " + gonnaReturn)
        }
      }
    }

    "everything is fine" should {
      "get a translation" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)
          }
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        val gonnaBeBody = xml.Utility.trim(
          <TranslateOptions xmlns="http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2">
            <Category>cool</Category>
            <ContentType></ContentType>
            <ReservedFlags></ReservedFlags>
            <State></State>
            <Uri></Uri>
            <User></User>
          </TranslateOptions>).buildString(true)

        val gonnaReturn = xml.Utility.trim(
        <GetTranslationsResponse xmlns="http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
          <Translations>
            <TranslationMatch>
              <Count>1001</Count>
              <MatchDegree>40</MatchDegree>
              <Rating>6</Rating>
              <TranslatedText>ablabl</TranslatedText>
            </TranslationMatch>
          </Translations>
        </GetTranslationsResponse>).buildString(true)

        when(httpClient.post[String](HttpClientBasicRequest(getTranslationsUri, Seq("text" -> "blabla", "from"-> "en", "to" -> "fr", "maxTranslations" -> "10"), Seq("Authorization" -> "Bearer 111aaa", "Content-Type" -> "text/xml")), gonnaBeBody)).thenReturn(Future.successful(200, gonnaReturn))

        //act
        whenReady(client.getTranslations(GetTranslationsRequest("blabla", 10, "en", "fr", Some("cool")))) { res =>

          //assert
          res should be (GetTranslationsResponse(Seq(TranslationMatch("ablabl", 40, 6, 1001))))
        }
      }
    }
  }

  "Speaking" when {
    "token provider returns failure" should {
      "fail with that failure" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Status.Failure(new RuntimeException("Bad!"))
          }
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        //act
        whenReady(client.speak(SpeakRequest("blabla", "ru", None, None)).failed) { res =>

          //assert
          res.getMessage should be("Bad!")
        }
      }
    }

    "gets an exception during calling a remote method" should {
      "fail with that exception" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)}
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        when(httpClient.get[Array[Byte]](HttpClientBasicRequest(speakUri, Seq("text"-> "bla", "language"-> "fr", "format" -> `audio/wav`.toString, "options"-> MaxQuality.toString), Seq("Authorization"->"Bearer 111aaa")))).thenReturn(Future.failed(new RuntimeException("Can't connect")))

        //act
        whenReady(client.speak(SpeakRequest("bla", "fr", Some(`audio/wav`), Some(MaxQuality))).failed) {res =>

          //assert
          res.getMessage should be ("Can't connect")
        }
      }
    }

    "everything is fine" should {
      "get a pronunciation" in {
        //arrange
        val actorRef = system.actorOf(Props(new Actor {
          def receive = {
            case TokenRequestMessage => sender ! Token("111aaa", 1000)
          }
        }))

        val client = new RemoteServiceClientImpl("my-client-id", "my-client-secret", actorRef, 2000, httpClient)

        when(httpClient.get[Array[Byte]](HttpClientBasicRequest(speakUri, Seq("text"-> "blabla", "language"-> "fr"), Seq("Authorization"->"Bearer 111aaa")))).thenReturn(Future.successful(200, Array[Byte](1, 2, 3)))

        //act
        whenReady(client.speak(SpeakRequest("blabla", "fr", None, None))) { res =>

          //assert
          res.data should be (Array[Byte](1, 2, 3))
        }
      }
    }
  }
}
