package com.gavthan.manager.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.json.Json

/**
 * Single Supabase client for the whole app (auth + postgrest + realtime).
 * Rows are decoded into [kotlinx.serialization.json.JsonObject] and mapped by
 * hand (see Models.kt), so we never crash on a column type quirk.
 */
object Supa {

    /** Lenient JSON used for manual parsing (e.g. a bill's embedded items array). */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = Config.SUPABASE_URL,
            supabaseKey = Config.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
