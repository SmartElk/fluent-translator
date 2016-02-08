package com.smartelk.fluent.translator.microsoft.actions

import com.smartelk.fluent.translator.Dsl.Microsoft.TranslatorClient
import com.smartelk.fluent.translator.Dsl._
import com.smartelk.fluent.translator.basic.ActionState
import com.smartelk.fluent.translator.microsoft.remote.MicrosoftRemoteServiceClient
import com.smartelk.fluent.translator.basic._
import MicrosoftRemoteServiceClient.TranslateRequest
import scala.concurrent.Future

private[translator] object MicrosoftTranslateAction {

  case class TranslateActionParams(text: String,
                              fromLang: Option[String] = None,
                              toLang: Option[String] = None,
                              contentType: Option[TextContentType] = None,
                              category: Option[String] = None) {
    requireValidMicrosoftText(text)
  }

  class TranslateActionState(val state: TranslateActionParams) extends ActionState[TranslateActionParams]{
    def from(lang: String) = {
      requireValidFrom(lang)
      new TranslateActionStateFrom(state.copy(fromLang = Some(lang)))
    }

    def to(lang: String) = {
      new TranslateActionStateTo(state.copy(toLang = Some(lang)))
    }
  }

  class TranslateActionStateFrom(val state: TranslateActionParams) extends ActionState[TranslateActionParams]{
    def to(lang: String) = {
      requireValidTo(lang)
      new TranslateActionStateTo(state.copy(toLang = Some(lang)))
    }
  }

  class TranslateActionStateTo(val state: TranslateActionParams) extends ActionState[TranslateActionParams]{
    def withContentType(contentType: TextContentType) = {
      new TranslateActionStateTo(state.copy(contentType = Some(contentType)))
    }

    def withCategory(category: String) = {
      requireValidMicrosoftCategory(category)
      new TranslateActionStateTo(state.copy(category = Some(category)))
    }

    def as(scalaFutureWord: future.type)(implicit client: TranslatorClient): Future[String] = {
      client.remoteServiceClient.translate(TranslateRequest(state.text, state.toLang.get, state.fromLang, state.contentType, state.category))
    }
  }
}


