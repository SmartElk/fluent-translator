# fluent-translator

Scala library for working with Microsoft, Google(?)... translators via a fancy DSL.

Examples
--------------
Using Microsoft translator client:
```scala
  import com.smartelk.fluent.translator.Dsl._

  implicit object client extends MicrosoftTranslatorClient {
     val clientId = "microsoft client id"
     val clientSecret = "microsoft client secret"
  }

  Microsoft give me a translation of "Comment vas-tu?" from "fr" to "en" as future //Future[String]
  Microsoft give me a translation of "What a lovely weather today!" from "en" to "fr" withContentType `text/html` as future //Future[String]
  Microsoft give me many translations of "Doing well by doing good" from "en" to "ru" as future //Future[GetTranslationsResponse]
  Microsoft give me translations(3) of "Paris holidays" from "en" to "ru" withCategory "general" as future //Future[GetTranslationsResponse]
  Microsoft speak "I'm doing well enough now" in "en" withAudioContentType `audio/mp3` as future //Future[SpeakResponse]
  Microsoft speak "How are you doing?" in "en" withQuality MinSize as future //Future[SpeakResponse]
```

Installation
--------------
Build.sbt:
```scala
   resolvers += Resolver.bintrayRepo("smartelk", "maven") 
   libraryDependencies += "com.smartelk" %% "fluent-translator" % "0.1.1"
```