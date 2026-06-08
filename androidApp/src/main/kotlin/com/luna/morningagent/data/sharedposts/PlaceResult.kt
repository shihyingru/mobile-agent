package com.luna.morningagent.data.sharedposts

/**
 * A place resolved for a saved post, persisted in the local cache (hence
 * `@Serializable`). A post can carry several — a single-venue post has one; a
 * "best cafés in Seoul" listicle has many.
 *
 * Resolved via Google Places Text Search at the cheapest **Essentials** SKU
 * (`places.id` only — no name/address/coords from the API): [name] is the
 * model's place name (drives the sheet title + the map-app query; Hangul names
 * route to Naver/Kakao), [area] is the neighbourhood/city the model attached,
 * and [mapsUri] is a Google Maps `query_place_id` deep link pinning the exact
 * place returned by Places.
 */
@kotlinx.serialization.Serializable
data class ResolvedPlace(
    val name: String,
    val area: String = "",
    val mapsUri: String? = null,
)
