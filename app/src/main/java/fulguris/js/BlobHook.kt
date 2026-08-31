package fulguris.js

/**
 * Hooks URL.createObjectURL to store Blob references so they remain
 * accessible even after the page revokes the object URL.
 */
interface BlobHook {
    fun provideJs(): String
}
