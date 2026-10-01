package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MoneyCoachAi
import com.example.data.PriceInsightRepository
import com.example.model.InsightTier
import com.example.model.ScannedItem
import com.example.model.TaxLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    assertEquals("TrueTag", appName)
  }

  @Test
  fun `verify exact tax calculation with bigdecimal`() {
    // $10.00 item in Chicago (10.25% tax)
    val itemChicago = ScannedItem(
      name = "Organic Milk",
      tagPrice = 10.00,
      taxRate = 0.1025,
      cityName = "Chicago, IL"
    )
    assertEquals(1.03, itemChicago.taxAmount, 0.001)
    assertEquals(11.03, itemChicago.truePrice, 0.001)

    // $50.00 item in Portland, OR (0% tax)
    val itemPortland = ScannedItem(
      name = "Sneakers",
      tagPrice = 50.00,
      taxRate = 0.0,
      cityName = "Portland, OR"
    )
    assertEquals(0.00, itemPortland.taxAmount, 0.001)
    assertEquals(50.00, itemPortland.truePrice, 0.001)
  }

  @Test
  fun `verify money coach context aware answers`() {
    val taxLocation = TaxLocation(
      zip = "60601",
      city = "Chicago",
      state = "IL",
      county = "Cook",
      combinedRate = 0.1025
    )
    val cartItems = listOf(
      ScannedItem(name = "Coffee", tagPrice = 12.00, taxRate = 0.1025, cityName = "Chicago, IL")
    )

    val budgetAnswer = MoneyCoachAi.generateAnswer(
      question = "Am I over budget?",
      cartItems = cartItems,
      taxLocation = taxLocation,
      budget = 50.0,
      recentTrips = emptyList()
    )
    assertTrue(budgetAnswer.contains("budget"))

    val oregonAnswer = MoneyCoachAi.generateAnswer(
      question = "How much would I save in Oregon?",
      cartItems = cartItems,
      taxLocation = taxLocation,
      budget = 50.0,
      recentTrips = emptyList()
    )
    assertTrue(oregonAnswer.contains("Oregon"))
  }

  @Test
  fun `verify price insight repository evaluates deal`() {
    val insight = PriceInsightRepository.evaluate("Milk", 2.50)
    assertNotNull(insight)
    assertTrue(insight.summary.isNotBlank())
  }
}
