package fulguris.js

/**
 * Detects whether a touch event targets a nested CSS scrollable element.
 * Notifies the native side via the _fulgurisScroll JavaScript interface
 * so that pull-to-refresh can be suppressed when appropriate.
 */
interface NestedScrollDetect {
    fun provideJs(): String
}
