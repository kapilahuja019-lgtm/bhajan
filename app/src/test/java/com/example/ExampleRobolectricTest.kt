package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BHAJAN", appName)
  }

  @Test
  fun `test sindhi transliterator on user input`() {
    val result = com.example.util.SindhiTransliterator.toSindhiArabic("kapil is greart")
    org.junit.Assert.assertTrue("Result should not be empty or single letter", result.length > 3)
    org.junit.Assert.assertTrue("Should contain Sindhi Arabic characters", com.example.util.SindhiTransliterator.isSindhiArabicScript(result))
    
    val devotionalResult = com.example.util.SindhiTransliterator.toSindhiArabic("om jai ram hare krishna")
    org.junit.Assert.assertEquals("اوم جئي رام هري ڪرشن", devotionalResult)
  }
}
