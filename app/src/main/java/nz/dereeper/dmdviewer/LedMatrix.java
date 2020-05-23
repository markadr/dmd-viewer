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

public class LedMatrix {

    final int pixelWidth; // in pixels
    final int pixelHeight; // in pixels
    final int margin; // pixel gap around the outside of the drawn pixels
    final int ledWidth; // the combined number of width pixels
    final int ledHeight; // the combined number of height pixels
    final boolean enabled;

    /**
     * Defines a simple representation of a LED pixel matrix. For an instance set as 2, 2, 1,
     * a single pixel would get drawn as:
     *
     *    xxo
     *    xxo
     *    ooo
     *
     * Where x represents the pixel and the o represents the margin and is not drawn.
     *
     * @param pixelWidth the number of pixels in the drawn width.
     * @param pixelHeight the number of pixels in the drawn height.
     * @param margin the number of pixels in the margin.
     * @param enabled set to true to enable.
     */
    LedMatrix(int pixelWidth, int pixelHeight, int margin, boolean enabled) {
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.margin = margin;
        this.enabled = enabled;
        ledWidth = pixelWidth + margin;
        ledHeight = pixelHeight + margin;
    }

    @NonNull
    @Override
    public String toString() {
        return  "pixelWidth:" + pixelWidth +
                " pixelHeight:" + pixelHeight +
                " margin:" + margin +
                " enabled:" + enabled;
    }
}
