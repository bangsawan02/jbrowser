package fulguris.js

/**
 * Reads a blob: URL via fetch(), converts it to a base64 data URL, and
 * posts the result back through the _fulgurisBlobDownload JS interface.
 */
interface BlobDownload {
    fun provideJs(): String
}
