/*
 * Copyright (C) 2017-2019 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.hazuki.yuzubrowser.adblock.repository.abp

import fulguris.adblock.AbpListUpdater
import android.content.Context

class AbpDao(val context: Context) {
    val prefs = context.getSharedPreferences("ad_block_settings", Context.MODE_PRIVATE)

    fun getAll(): List<AbpEntity> {
        val set = prefs.getStringSet(ABP_ENTITIES, ABP_DEFAULT_ENTITIES)
        val list = mutableListOf<AbpEntity>()
        set!!.forEach { list.add(abpEntityFromString(it) ?: return@forEach) }
        // return sorted list to have consistent order shown in settings
        list.sortBy { it.entityId }
        return list
    }

    // update also handles new entities, they should have index 0 to avoid duplicates (can't happen in a db...)
    fun update(abpEntity: AbpEntity): Int {
        val list = getAll() as MutableList

        // check whether entity exists
        for (index in list.indices) {
            if (list[index].equals(abpEntity)) { // compares id only
                list[index] = abpEntity
                prefs.edit().putStringSet(ABP_ENTITIES, list.map { it.toString() }.toSet()).apply()
                return abpEntity.entityId
            }
        }

        // if entity has index 0, find a valid unique ne id
        if (abpEntity.entityId == 0) {
            val ids =  list.map { it.entityId }
            var i = 1
            while (ids.contains(i)) {
                ++i
            }
            abpEntity.entityId = i
        }
        list.add(abpEntity)
        prefs.edit().putStringSet(ABP_ENTITIES, list.map { it.toString() }.toSet()).apply()
        return abpEntity.entityId
    }

    fun delete(abpEntity: AbpEntity) {
        val list = getAll() as MutableList
        list.removeAll { it.entityId == abpEntity.entityId }
        AbpListUpdater(context).removeFiles(abpEntity)

        if (list.isEmpty())
            prefs.edit().remove(ABP_ENTITIES).apply()
        else
            prefs.edit().putStringSet(ABP_ENTITIES, list.map { it.toString() }.toSet()).apply()
    }

}

// add some default block lists
const val ABP_ENTITIES = "abpEntities"
val ABP_ENTITY_EASYLIST = AbpEntity(title = "EasyList", entityId = 2, url = "https://easylist.to/easylist/easylist.txt", homePage = "https://easylist.to")
val ABP_ENTITY_EASYPRIVACY = AbpEntity(title = "EasyPrivacy", entityId = 3, url = "https://easylist.to/easylist/easyprivacy.txt", homePage = "https://easylist.to")
val ABP_ENTITY_URLHAUS = AbpEntity(title = "Urlhaus Malicious URL Blocklist", entityId = 4, url = "https://raw.githubusercontent.com/curbengh/urlhaus-filter/master/urlhaus-filter-agh-online.txt", homePage = "https://gitlab.com/curben/urlhaus-filter")
val ABP_ENTITY_STEVEN_BLACK = AbpEntity(title = "StevenBlack hosts list", entityId = 5, url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", homePage = "https://github.com/StevenBlack/hosts", enabled = true)
val ABP_ENTITY_UBLOCK_FILTERS = AbpEntity(title = "uBlock Filters", entityId = 6, url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt", homePage = "https://github.com/uBlockOrigin/uAssets")
val ABP_ENTITY_UBLOCK_PRIVACY = AbpEntity(title = "uBlock Privacy Filters", entityId = 7, url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt", homePage = "https://github.com/uBlockOrigin/uAssets")
val ABP_ENTITY_ADGUARD_BASE = AbpEntity(title = "AdGuard Base Filter", entityId = 8, url = "https://filters.adtidy.org/extension/ublock/filters/2.txt", homePage = "https://adguard.com")
val ABP_ENTITY_ADGUARD_ANNOYANCES = AbpEntity(title = "AdGuard Annoyances (Cookie notices, popups)", entityId = 9, url = "https://filters.adtidy.org/extension/ublock/filters/14.txt", homePage = "https://adguard.com")
val ABP_ENTITY_FANBOY_ANNOYANCES = AbpEntity(title = "Fanboy Annoyances (Social widgets, cookie notices)", entityId = 10, url = "https://easylist.to/easylist/fanboy-annoyance.txt", homePage = "https://easylist.to")
val ABP_ENTITY_PETER_LOWES = AbpEntity(title = "Peter Lowe's Ad and tracking server list", entityId = 11, url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=1&mimetype=plaintext", homePage = "https://pgl.yoyo.org/adservers/")

val ABP_DEFAULT_ENTITIES = setOf(
    ABP_ENTITY_EASYLIST.toString(),
    ABP_ENTITY_EASYPRIVACY.toString(),
    ABP_ENTITY_URLHAUS.toString(),
    ABP_ENTITY_STEVEN_BLACK.toString(),
    ABP_ENTITY_UBLOCK_FILTERS.toString(),
    ABP_ENTITY_UBLOCK_PRIVACY.toString(),
    ABP_ENTITY_ADGUARD_BASE.toString(),
    ABP_ENTITY_ADGUARD_ANNOYANCES.toString(),
    ABP_ENTITY_FANBOY_ANNOYANCES.toString(),
    ABP_ENTITY_PETER_LOWES.toString(),
)
