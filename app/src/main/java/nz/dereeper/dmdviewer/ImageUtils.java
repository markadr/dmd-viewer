/*
 * Copyright 2020 Mark de Reeper
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package nz.dereeper.dmdviewer;

import android.graphics.Color;

import timber.log.Timber;

import static androidx.core.graphics.ColorUtils.HSLToColor;

/**
 * A set of static methods that support generating a raw image depending on the FrameType.
 * Borrowed heavily from the code/algorithms in https://github.com/freezy/dmd-extensions
 */
class ImageUtils {

    private ImageUtils() {
        // Just statics
    }

    static int[] toRawImageFromRgb24(final byte[] colours, final Metadata metadata) {
        if (colours.length % 3 == 0) {
            final int width = metadata.getDimensions().width;
            final int height = metadata.getDimensions().height;
            // Calculate this now rather than every time we want to set a pixel when LED enabled.
            final int extraWidth = extraWidthCalc(metadata);
            final int[] rawImage = createRawImage(metadata);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int index = y * width + x;
                    // RGB24 is in BGR order
                    final int b = (0xFF & colours[3 * index]);
                    final int g = (0xFF & colours[3 * index + 1]);
                    final int r = (0xFF & colours[3 * index + 2]);
                    setPixel(rawImage, x, y, Color.rgb(r, g, b), metadata, extraWidth);
                }
            }
            return rawImage;
        } else {
            Timber.e("Planes length not a multiple of 3 in RGB24: %s", colours.length);
            return null;
        }
    }

    static int[] toRawImage(final byte[] planes,
                            final int[] palette,
                            final int bitLength,
                            final Metadata metadata) {
        if (planesAreValid(planes, bitLength, metadata.getDimensions())) {
            final int width = metadata.getDimensions().width;
            final int height = metadata.getDimensions().height;
            // Calculate this now rather than every time we want to set a pixel when LED enabled.
            final int extraWidth = extraWidthCalc(metadata);
            final int[] rawImage = createRawImage(metadata);
            final byte[] plane = joinPlanes(planes, bitLength, metadata.getDimensions());
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    setPixel(rawImage, x, y, palette[plane[y * width + x]], metadata, extraWidth);
                }
            }
            return rawImage;
        }
        Timber.w("Planes data was not valid, bitLength: %s, planes length: %s, area: %s",
                bitLength, planes.length, metadata.getDimensions().area);
        return null;
    }

    static int[] toRawImage(final byte[] planes,
                            final float numberOfColours,
                            final int bitLength,
                            final Metadata metadata) {
        if (planesAreValid(planes, bitLength, metadata.getDimensions())) {
            final int width = metadata.getDimensions().width;
            final int height = metadata.getDimensions().height;
            // Calculate this now rather than every time we want to set a pixel when LED enabled.
            final int extraWidth = extraWidthCalc(metadata);
            final int[] rawImage = createRawImage(metadata);
            final byte[] plane = joinPlanes(planes, bitLength, metadata.getDimensions());
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final float[] newHsl = metadata.getHslCopy();
                    newHsl[2] = newHsl[2] * (plane[y * width + x] / numberOfColours); // Lum value
                    setPixel(rawImage, x, y, HSLToColor(newHsl), metadata, extraWidth);
                }
            }
            return rawImage;
        }
        Timber.w("Planes data was not valid, bitLength: %s, planes length: %s, area: %s",
                bitLength, planes.length, metadata.getDimensions().area);
        return null;
    }

    private static boolean planesAreValid(final byte[] planes,
                                          final int bitLength,
                                          final Dimensions dimensions) {
        // Sanity check that we have a valid set of planes data compared to expected values
        return dimensions.area % 8 == 0 &&
               planes.length % bitLength == 0 &&
               planes.length / bitLength == dimensions.area / 8;
    }

    private static int[] createRawImage(final Metadata metadata) {
        final LedMatrix ledMatrix = metadata.getLedMatrix();
        final int width = metadata.getDimensions().width;
        final int height = metadata.getDimensions().height;
        if (ledMatrix.enabled) {
            return new int[(width * ledMatrix.ledWidth) * (height * ledMatrix.ledHeight)];
        } else {
            return new int[width * height];
        }
    }

    private static void setPixel(final int[] rawImage,
                                 final int x,
                                 final int y,
                                 final int colour,
                                 final Metadata metadata,
                                 final int extraWidth) {
        // Not much point in painting a black pixel
        if (colour != -1) {
            if (metadata.getLedMatrix().enabled) {
                drawLedMatrixPixels(x, y, colour, rawImage, metadata, extraWidth);
            } else {
                // Calc the index into the array based on the X and Y values using the width to
                // determine the column.
                rawImage[y * extraWidth + x] = colour;
            }
        } else {
            Timber.v("Skipping black pixel");
        }
    }

    private static void drawLedMatrixPixels(final int x,
                                            final int y,
                                            final int colour,
                                            final int[] rawImage,
                                            final Metadata metadata,
                                            final int extraWidth) {
        final LedMatrix ledMatrix = metadata.getLedMatrix();
        final Dimensions dimensions = metadata.getDimensions();
        // Based on ideas from https://github.com/sallar/led-matrix/blob/master/src/index.ts
        final int index = y * dimensions.width + x;
        final int dy = (int)Math.floor((float)index / (float)dimensions.width);
        final int dx = index - dy * dimensions.width;
        final int newX = dx * ledMatrix.ledWidth;
        final int newY = dy * ledMatrix.ledHeight;
        // Draw the actual rect for this pixel
        for (int i = 0; i < ledMatrix.pixelHeight; i++) {
            for (int j = 0; j < ledMatrix.pixelWidth; j++) {
                // Calc the index into the array based on the new X and Y values, compensating for
                // the additional width we need for the extra pixels in the LED matrix.
                rawImage[(newY + i) * extraWidth + newX + j] = colour;
            }
        }
    }

    private static byte[] joinPlanes(final byte[] planes,
                                     final int bitLength,
                                     final Dimensions dimensions) {
        final byte[] plane = new byte[dimensions.area];
        final int bytes = dimensions.area / 8;
        // From my understanding....
        // The frame is made up of a width x height / 8 (number of bits in a byte) items across
        // bitLength number of planes.
        // To work out the colour of any one of the width x height pixels we need to grab the same
        // column value from each plane and then split them into bits to get a combined value that
        // represents the colour palette lookup value, most significant bit first.
        // For 2 planes we have a colour palette of 4, for 4 planes we have 16.
        for (int bytePos = 0; bytePos < bytes; bytePos++) {
            for (int bitPos = 7; bitPos >= 0; bitPos--) {
                for (int planePos = 0; planePos < bitLength; planePos++) {
                    final int bit = theBit(planes[bytes * planePos + bytePos], bitPos);
                    plane[bytePos * 8 + bitPos] |= (bit << planePos);
                }
            }
        }
        return plane;
    }

    private static int theBit(final byte b, final int pos) {
        if (b == 0) {
            return 0;
        }
        return (b & (1 << pos)) != 0 ? 1 : 0;
    }

    private static int extraWidthCalc(final Metadata metadata) {
        // If LED effect is enabled then the extra width needs to take this into account.
        if (metadata.getLedMatrix().enabled) {
            return metadata.getDimensions().width * metadata.getLedMatrix().ledWidth;
        }
        return metadata.getDimensions().width;
    }
}
