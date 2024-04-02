package com.databricks.labs.tika

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GenericConfigManagerTest extends AnyFlatSpec with Matchers {

  "GenericConfigManager" should "set and reset a configuration value" in {
    var configValue = 10 // Initial configuration value

    def getConfig: Int = configValue // Getter function
    def setConfig(value: Int): Unit = configValue = value // Setter function

    val originalValue = getConfig
    val newValue = 20

    val configManager = new GenericConfigManager[Int](setConfig, originalValue, newValue)

    // Before applying the config manager, the value should be the original
    getConfig should be(originalValue)

    // Apply the config manager to change the value temporarily
    val result = configManager {
      getConfig should be(newValue) // Inside the block, the value should be the new value
      "Success" // Return some result from the block
    }

    // After applying the config manager, the value should be reset to the original
    getConfig should be(originalValue)
    result should be("Success") // The result of the block should be returned correctly
  }
}
