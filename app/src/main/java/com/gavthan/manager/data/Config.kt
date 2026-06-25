package com.gavthan.manager.data

/**
 * App configuration — the SAME values the web app embeds in its `GH_CONFIG`
 * block. This Android client talks to the SAME Supabase project, so the login,
 * ledger and inventory data are shared with the web/billing apps.
 *
 * (For a production build you would normally move the keys into
 * `local.properties` / BuildConfig, but they are kept here verbatim to match
 * the source web app and guarantee zero missing configuration.)
 */
object Config {
    const val SUPABASE_URL = "https://glaksnwcmiijiztsncvi.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdsYWtzbndjbWlpaml6dHNuY3ZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg0OTU2MjIsImV4cCI6MjA5NDA3MTYyMn0.IEAWtwrjCYFV1QuHRwri0qvBGXCfPdB6ni998DSFjgo"

    const val HOTEL_NAME = "Gavthan"
    const val CURRENCY = "₹" // ₹
}
