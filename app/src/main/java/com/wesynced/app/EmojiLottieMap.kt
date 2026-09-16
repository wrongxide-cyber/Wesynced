package com.wesynced.app

import android.content.Context

/**
 * Looks up whether a bundled animated (Lottie) version exists for a given
 * emoji, based on whatever .json files are actually present in
 * assets/emoji_lottie/. Scales to any number of bundled files with no
 * hardcoded list -- add or remove files from that folder and the lookup
 * adjusts automatically.
 */
object EmojiLottieMap {

    private const val ASSET_DIR = "emoji_lottie"
    private var availableFiles: Set<String>? = null

    private fun index(context: Context): Set<String> {
        return availableFiles ?: try {
            context.assets.list(ASSET_DIR)?.toSet().orEmpty().also { availableFiles = it }
        } catch (e: Exception) {
            emptySet<String>().also { availableFiles = it }
        }
    }

    fun assetFor(context: Context, emoji: String): String? {
        val files = index(context)
        for (candidate in candidateFileNames(emoji)) {
            if (files.contains(candidate)) {
                return "$ASSET_DIR/$candidate"
            }
        }
        return null
    }

    private fun candidateFileNames(emoji: String): List<String> {
        val codepoints = emoji.codePoints().toArray()
        val withSelectors = codepoints.joinToString("_") { Integer.toHexString(it) }
        val withoutVariationSelector = codepoints
            .filter { it != 0xFE0F }
            .joinToString("_") { Integer.toHexString(it) }

        return listOf(
            "emoji_u$withSelectors.json",
            "emoji_u$withoutVariationSelector.json"
        ).distinct()
    }
}
