package com.example.ripplelauncher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.UserManager
import java.io.File
import java.security.MessageDigest

/**
 * Keeps rendered app icons so the launcher doesn't redraw every icon on each start.
 *
 * - Memory: icons stay in RAM while the launcher runs, so a refresh after installing
 *   one app only renders that one icon.
 * - Disk: icons and the app list are saved as small PNGs, so after a restart (or an
 *   update of Ripple itself) the list appears immediately from the saved copy.
 *
 * Each icon is stored with a version (the app's install/update time). When an app
 * updates, its version changes and the icon is rendered again.
 */
class IconCache(private val ctx: Context, iconPx: Int) {

    data class Cached(
        val key: String,
        val label: String,
        val component: ComponentName,
        val userSerial: Long,
        val version: Long,
        val bitmap: Bitmap,
    )

    // Separate folder per icon size, so a display-size change never shows blurry icons.
    private val dir = File(ctx.cacheDir, "icons_$iconPx").apply { mkdirs() }
    private val index = File(dir, "index.tsv")
    private val memory = HashMap<String, Cached>()
    private val userManager = ctx.getSystemService(UserManager::class.java)

    @Synchronized
    fun get(key: String, version: Long): Bitmap? {
        memory[key]?.let { if (it.version == version) return it.bitmap }
        return null
    }

    /** Forget every icon of a package, e.g. when it changes without a version bump. */
    @Synchronized
    fun evictPackage(packageName: String) {
        memory.values.removeAll { it.component.packageName == packageName }
    }

    /** Reads the saved list from disk. Fast: only small PNG decodes, no icon rendering. */
    @Synchronized
    fun loadFromDisk(): List<Cached> {
        if (!index.exists()) return emptyList()
        val out = ArrayList<Cached>()
        runCatching {
            index.readLines().forEach { line ->
                val p = line.split('\t')
                if (p.size != 5) return@forEach
                val component = ComponentName.unflattenFromString(p[2]) ?: return@forEach
                val file = File(dir, fileName(p[0]))
                val bmp = BitmapFactory.decodeFile(file.path) ?: return@forEach
                val c = Cached(p[0], p[1], component, p[3].toLong(), p[4].toLong(), bmp)
                memory[c.key] = c
                out += c
            }
        }
        return out
    }

    /** Saves the current list: writes new icons, removes ones for uninstalled apps. */
    @Synchronized
    fun save(entries: List<Cached>) {
        runCatching {
            val keep = HashSet<String>()
            entries.forEach { c ->
                val name = fileName(c.key)
                keep += name
                val file = File(dir, name)
                val old = memory[c.key]
                if (!file.exists() || old == null || old.version != c.version || old.bitmap !== c.bitmap) {
                    file.outputStream().use { c.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                memory[c.key] = c
            }
            memory.keys.retainAll(entries.map { it.key }.toSet())
            dir.listFiles()?.forEach { f ->
                if (f.name.endsWith(".png") && f.name !in keep) f.delete()
            }
            val tmp = File(dir, "index.tmp")
            tmp.writeText(entries.joinToString("\n") { c ->
                val label = c.label.replace('\t', ' ').replace('\n', ' ')
                "${c.key}\t$label\t${c.component.flattenToString()}\t${c.userSerial}\t${c.version}"
            })
            tmp.renameTo(index)
        }
    }

    fun userFor(serial: Long) = userManager.getUserForSerialNumber(serial)
    fun serialFor(user: android.os.UserHandle) = userManager.getSerialNumberForUser(user)

    private fun fileName(key: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".png"
    }
}
