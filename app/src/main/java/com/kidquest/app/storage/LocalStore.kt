// 기기에 연령과 배지만 저장한다. 아이 이름, 목소리, 위치는 저장하지 않는다.
package com.kidquest.app.storage

import android.content.Context

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var ageGroup: String?
        get() = prefs.getString(KEY_AGE_GROUP, null)
        set(value) = prefs.edit().putString(KEY_AGE_GROUP, value).apply()

    var badges: Set<String>
        get() = prefs.getStringSet(KEY_BADGES, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_BADGES, value).apply()

    private companion object {
        const val FILE = "kidquest"
        const val KEY_AGE_GROUP = "age_group"
        const val KEY_BADGES = "badges"
    }
}
