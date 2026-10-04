package dev.jordanempire.youflow.ui

import dev.jordanempire.youflow.ui.model.UiState
import dev.jordanempire.youflow.ui.util.recaptchaUrl
import dev.jordanempire.youflow.ui.util.toUiError
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

class ErrorsTest {
    @Test
    fun recaptchaUrlIsFoundDirectly() {
        val e = ReCaptchaException("challenge", "https://www.google.com/sorry/index")
        assertEquals("https://www.google.com/sorry/index", e.recaptchaUrl())
    }

    @Test
    fun recaptchaUrlIsFoundInTheCauseChain() {
        val e = RuntimeException("wrapped", IOException(ReCaptchaException("c", "https://example/sorry")))
        assertEquals("https://example/sorry", e.recaptchaUrl())
    }

    @Test
    fun otherErrorsHaveNoRecaptchaUrl() {
        assertNull(IOException("boom").recaptchaUrl())
    }

    @Test
    fun uiErrorForRecaptchaAsksToVerify() {
        val error = ReCaptchaException("c", "https://example/sorry").toUiError()
        assertEquals("https://example/sorry", error.recaptchaUrl)
        assertTrue(error.message.contains("robot"))
    }

    @Test
    fun uiErrorForPlainFailureKeepsTheMessage() {
        val error: UiState.Error = IOException("no network").toUiError()
        assertEquals("no network", error.message)
        assertNull(error.recaptchaUrl)
    }
}
