package fulguris.js

/**
 * Observes changes to theme-color and color-scheme meta tags in the HTML document.
 * Reports changes via console messages that are parsed by WebPageChromeClient.
 */
interface ThemeColor {
    fun provideJs(): String
}