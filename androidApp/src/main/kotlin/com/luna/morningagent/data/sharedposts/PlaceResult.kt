package com.luna.morningagent.data.sharedposts

/**
 * A resolved real-world place from Google Places Text Search (Places API New).
 *
 * Returned by the fetcher's Places lookup for one query. Transient — never
 * persisted (see [ResolvedPlace] for the stored form) — so it stays a plain data
 * class; the Places JSON is parsed manually in the fetcher.
 */
data class PlaceResult(
    val displayName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val googleMapsUri: String? = null,
)

/**
 * A place resolved for a saved post, persisted in the local cache (so it must be
 * `@Serializable`, unlike the transient [PlaceResult]). A post can carry several
 * — a single-venue post has one; a "best cafés in Seoul" listicle has many.
 *
 * [name] drives both the Saved card's pin label and the map-app query (Hangul
 * names route to Naver/Kakao); [mapsUri] is the precise Google Maps deep link.
 */
@kotlinx.serialization.Serializable
data class ResolvedPlace(
    val name: String,
    val address: String = "",
    val mapsUri: String? = null,
)
