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

import androidx.annotation.NonNull;

public class Dmd {

    private final int pixels;
    private final int margin; // pixel gap around the outside of the drawn pixels
    private final int combined; // the combined number of pixels and margin
    private final boolean enabled;
    private final boolean round;

    private static final boolean[][] square4x4 = {
        { true, true, true, true },
        { true, true, true, true },
        { true, true, true, true },
        { true, true, true, true }
    };

    private static final boolean[][] circle4x4 = {
        { false, true, true, false },
        { true, true, true, true },
        { true, true, true, true },
        { false, true, true, false }
    };

    /**
     * Defines a simple representation of a Dot Matrix Display (DMD).
     * Currently set to fixed size of 4 x 4 pixels with a margin of 1 pixel.
     * @param enabled set to true to enable.
     * @param round set to true to enable round pixels.
     */
    Dmd(boolean enabled, boolean round) {
        this.pixels = 4;
        this.margin = 1;
        this.enabled = enabled;
        this.round = round;
        combined = pixels + margin;
    }

    /**
     * Return true if the DMD is enabled.
     * @return true if the DMD is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * The size in pixels of this matrix.
     * @return the size in pixels of this matrix.
     */
    public int getPixels() {
        return pixels;
    }

    /**
     * The size in pixels including the margin.
     * @return the size in pixels including the margin.
     */
    public int getCombined() {
        return combined;
    }

    /**
     * Depending on if round has been enabled, return the shape to draw that represents a single
     * pixel, either a square or a circle.
     * @return a matrix of pixels x pixels sized matrix where
     * true means the pixel should be on.
     */
    public boolean[][] getShape() {
        if (round) {
            return circle4x4;
        }
        return square4x4;
    }

    @NonNull
    @Override
    public String toString() {
        return  "pixels:" + pixels +
                " margin:" + margin +
                " enabled:" + enabled;
    }
}
