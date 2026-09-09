package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val samplePart = com.example.data.model.PartEntity(
      partId = "test-1",
      partName = "Spark Plug NGK CPR8EA",
      partNumber = "CPR8EA-9",
      firm = "SAA",
      rack = "A",
      rackNumber = "12",
      stockSaa = 45,
      stockTvs = 10,
      minStock = 5,
      sellingPrice = 180.0
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.PartItemCard(
          part = samplePart,
          onClick = {},
          onStockIn = {},
          onStockOut = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
