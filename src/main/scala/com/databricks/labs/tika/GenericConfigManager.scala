package com.databricks.labs.tika

class GenericConfigManager[T](setFunc: T => Unit, originalValue: T, newValue: T) {
  def apply[U](block: => U): U = {
    try {
      setFunc(newValue)
      block // Execute the block of code with the new setting
    } finally {
      setFunc(originalValue) // Reset to original value
    }
  }
}
