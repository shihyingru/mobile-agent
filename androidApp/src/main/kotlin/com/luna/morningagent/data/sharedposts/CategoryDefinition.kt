package com.luna.morningagent.data.sharedposts

import kotlinx.serialization.Serializable

/**
 * A saved-posts category. Users can add / rename / delete categories from
 * Settings; the categorizer reads `keywords` as fuzzy semantic hints when
 * deciding which category fits a new post.
 *
 * Auto-grown categories (invented by the model on save) land with an empty
 * `keywords` list — Luna can add hints later in Settings to steer future
 * classifications.
 */
@Serializable
data class CategoryDefinition(
    val name: String,
    val keywords: List<String> = emptyList(),
)
