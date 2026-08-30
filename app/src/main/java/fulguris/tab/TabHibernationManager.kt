package fulguris.tab

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages tab hibernation to conserve memory.
 * When memory is low, inactive tabs' WebViews can be hibernated and their state
 * saved, to be restored when the user switches back to them.
 */
@Singleton
class TabHibernationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Memory threshold percentage below which tabs start getting hibernated */
        private const val LOW_MEMORY_THRESHOLD = 0.25f
        /** Default maximum number of active (non-hibernated) tabs to keep */
        private const val DEFAULT_MAX_ACTIVE_TABS = 5
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Check if device memory is running low.
     */
    fun isMemoryLow(): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val usedRatio = 1.0f - (memoryInfo.availMem.toFloat() / memoryInfo.totalMem.toFloat())
        return usedRatio > (1.0f - LOW_MEMORY_THRESHOLD) || memoryInfo.lowMemory
    }

    /**
     * Get the maximum number of tabs that should remain active based on available memory.
     */
    fun getMaxActiveTabs(): Int {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val availableMb = memoryInfo.availMem / (1024 * 1024)
        return when {
            availableMb < 512 -> 2
            availableMb < 1024 -> 3
            availableMb < 2048 -> DEFAULT_MAX_ACTIVE_TABS
            else -> DEFAULT_MAX_ACTIVE_TABS * 2
        }
    }

    /**
     * Get current memory usage info for debugging.
     */
    fun getMemoryInfo(): String {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        val availMb = memoryInfo.availMem / (1024 * 1024)
        val usedMb = totalMb - availMb
        return "Memory: ${usedMb}MB / ${totalMb}MB (${availMb}MB available)${if (memoryInfo.lowMemory) " [LOW]" else ""}"
    }
}
