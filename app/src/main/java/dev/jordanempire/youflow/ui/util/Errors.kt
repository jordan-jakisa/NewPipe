package dev.jordanempire.youflow.ui.util

import dev.jordanempire.youflow.ui.model.UiState
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/** The reCAPTCHA url when YouTube/Google asked for a human check anywhere in the cause chain. */
fun Throwable.recaptchaUrl(): String? {
    var t: Throwable? = this
    while (t != null) {
        if (t is ReCaptchaException) return t.url
        t = t.cause
    }
    return null
}

fun Throwable.toUiError(): UiState.Error {
    val url = recaptchaUrl()
    return if (url != null) {
        UiState.Error("YouTube wants to check you're not a robot. Solve the check once, then try again.", url)
    } else {
        UiState.Error(message ?: javaClass.simpleName)
    }
}
