package com.luna.morningagent.data.sharedposts

import kotlinx.serialization.Serializable

/**
 * Multimodal cross-analysis of a shared post's image against its body text.
 *
 * Produced by [SharedPostBodyFetcher.analyzeImage] — Gemini OCRs the image,
 * identifies the scene + any named landmark, then reconciles those signals
 * with the post caption into a single deduplicated [locationSignals] list the
 * caller can feed to [SharedPostBodyFetcher.resolvePostLocations].
 *
 * `@Serializable` so the model can return it directly as JSON
 * (`responseMimeType = application/json`); every field defaults so a partial
 * model response still decodes.
 */
@Serializable
data class ImageAnalysisResult(
    /** Raw text the model read from the image (signage, menus, house numbers). */
    val ocrText: String? = null,
    /** Coarse scene label, e.g. "café interior" / "street view" / "food close-up". */
    val sceneType: String? = null,
    /** Flat ordered name list (most-specific first). Kept as the salvage/fallback
     *  source when [places] can't be parsed from a malformed response. */
    val locationSignals: List<String> = emptyList(),
    /** Named venue/landmark when confidently identified; null otherwise. */
    val landmark: String? = null,
    /** The distinct real-world destinations the post is about, each with a
     *  complete geocodable [PlaceQuery.query] (venue + neighbourhood + city).
     *  Ordered by prominence; a single-venue post has one entry, a listicle
     *  several. Excludes bare city/district/country "breadcrumb" entries. */
    val places: List<PlaceQuery> = emptyList(),
)

/** One destination the model pulled from a post: a short display [name] and a
 *  complete, geocodable [query] ("Hippo, Yeonnam-dong, Seoul"). */
@Serializable
data class PlaceQuery(
    val name: String = "",
    val query: String = "",
)
