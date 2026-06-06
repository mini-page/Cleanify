/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.domain.util

import android.graphics.Bitmap
import android.graphics.Color
import java.math.BigInteger
import androidx.core.graphics.scale

/**
 * A utility object for creating and comparing "difference" perceptual hashes (dHash) for images.
 * This implementation is self-contained and requires no external libraries.
 */
object PHashUtil {

    private const val HASH_WIDTH = 9
    private const val HASH_HEIGHT = 8

    /**
     * Calculates the "difference hash" (dHash) of a given bitmap.
     *
     * The process is as follows:
     * 1. **Resize:** The image is scaled down to a small, fixed size (9x8) to remove details.
     * 2. **Grayscale:** The scaled image is converted to grayscale.
     * 3. **Calculate Differences:** It computes the difference between adjacent pixels, row by row.
     *    If the pixel on the right is brighter than the one on the left, the bit is '1', otherwise it's '0'.
     * 4. **Construct:** The resulting bits are combined to form a single 64-bit hash, represented as a hex string.
     *
     * This method is more robust against simple brightness/contrast changes than an average hash.
     *
     * @param bitmap The input image to hash.
     * @return A 16-character hexadecimal string representing the perceptual hash.
     */
    fun calculateDHash(bitmap: Bitmap): String {
        // 1. Resize the bitmap to HASH_WIDTH x HASH_HEIGHT.
        val scaledBitmap = bitmap.scale(HASH_WIDTH, HASH_HEIGHT)

        val pixels = IntArray(HASH_WIDTH * HASH_HEIGHT)
        scaledBitmap.getPixels(pixels, 0, HASH_WIDTH, 0, 0, HASH_WIDTH, HASH_HEIGHT)
        scaledBitmap.recycle()

        // 2. Generate the bit string by comparing adjacent pixel luminance.
        val hashBits = StringBuilder(HASH_WIDTH * HASH_HEIGHT)
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                val leftPixelIndex = y * HASH_WIDTH + x
                val rightPixelIndex = leftPixelIndex + 1

                val leftLuminance = getLuminance(pixels[leftPixelIndex])
                val rightLuminance = getLuminance(pixels[rightPixelIndex])

                hashBits.append(if (rightLuminance > leftLuminance) '1' else '0')
            }
        }

        // 3. Construct the hex string from the bits.
        return BigInteger(hashBits.toString(), 2).toString(16).padStart(16, '0')
    }

    private fun getLuminance(color: Int): Double {
        // Standard luminance calculation
        return 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)
    }

    /**
     * Calculates the Hamming distance between two perceptual hash strings.
     * The Hamming distance is the number of positions at which the corresponding bits are different.
     * A lower distance means the images are more similar.
     *
     * @param hash1 The first hexadecimal hash string.
     * @param hash2 The second hexadecimal hash string.
     * @return The integer distance (number of differing bits). Returns -1 if hashes are invalid.
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != 16 || hash2.length != 16) {
            return -1 // Hashes must be 16 hex characters (64 bits).
        }
        try {
            // Convert hex strings to BigInteger
            val h1 = BigInteger(hash1, 16)
            val h2 = BigInteger(hash2, 16)

            // XOR the two hashes and count the number of set bits (1s)
            return h1.xor(h2).bitCount()
        } catch (e: NumberFormatException) {
            return -1 // Invalid hex string
        }
    }
}
