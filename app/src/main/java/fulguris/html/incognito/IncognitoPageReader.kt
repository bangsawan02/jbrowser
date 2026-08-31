/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package fulguris.html.incognito

/**
 * The store for the incognito HTML.
 */
interface IncognitoPageReader {

    fun provideHtml(): String

}