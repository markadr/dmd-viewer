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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import timber.log.Timber;

import static nz.dereeper.dmdviewer.Frame.FrameType.INVALID;
import static nz.dereeper.dmdviewer.Frame.FrameType.UNKNOWN;
import static nz.dereeper.dmdviewer.Frame.FrameType.getEnum;

class Frame {

    private FrameType frameType;
    private byte[] planes;
    private Dimensions dimensions;
    private String gameName;
    private int timeStamp;
    private int colour;
    private int[] palette;

    public enum FrameType {
        COLORED_GRAY_4("coloredGray4"),
        COLORED_GRAY_2("coloredGray2"),
        GRAY_2_PLANES("gray2Planes"),
        GRAY_4_PLANES("gray4Planes"),
        RGB24("rgb24"),
        DIMENSIONS("dimensions"),
        COLOUR("color"),
        PALETTE("palette"),
        CLEAR_COLOUR("clearColor"),
        CLEAR_PALETTE("clearPalette"),
        GAME_NAME("gameName"),
        UNKNOWN("unknown"),
        // For the case where we have a known type but the data is not as expected.
        INVALID("invalid");

        private final String type;

        FrameType(String type) {
            this.type = type;
        }

        public static FrameType getEnum(final String type) {
            for (FrameType frameType : FrameType.values()) {
                if (frameType.type.equals(type)) {
                    return frameType;
                }
            }
            Timber.w("Unknown frame type: %s", type);
            return UNKNOWN;
        }

        @NonNull
        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Create a {@link Frame} from a dmdext binary WS message.
     * @param data the binary message from dmdext.
     */
    Frame(ByteBuffer data) {
        frameType = getEnum(stringFromData(data, true));
        if (!frameType.equals(UNKNOWN)) {
            deserialize(data);
        }
    }

    /**
     * Create a {@link Frame} of type {@code FrameType.RGB24} from a pre-defined set of BGR data.
     * @param bgrData the pre-defined set of BGR data.
     */
    Frame(byte[] bgrData) {
        this.frameType = FrameType.RGB24;
        this.planes = bgrData;
        this.timeStamp = 0;
    }

    FrameType getFrameType() {
        return frameType;
    }

    Dimensions getDimensions() {
        return dimensions;
    }

    String getGameName() {
        return gameName;
    }

    int getTimeStamp() {
        return timeStamp;
    }

    int getColour() {
        return colour;
    }

    byte[] getPlanes() {
        return planes;
    }

    int[] getPalette() {
        return palette;
    }

    // Attempt to construct the additional data based on the type of frame we are
    private void deserialize(ByteBuffer frameData) {
        final int remainingFrameData = frameData.remaining();
        try {
            switch (frameType) {
                case GAME_NAME:
                    gameName = stringFromData(frameData, false);
                    break;
                case DIMENSIONS:
                    dimensions = new Dimensions(frameData.getInt(), frameData.getInt());
                    break;
                case COLORED_GRAY_2:
                case COLORED_GRAY_4:
                    timeStamp = frameData.getInt();
                    palette = paletteFromData(frameData);
                    // The remainder of the data contains the planes.
                    planes = new byte[frameData.remaining()];
                    frameData.get(planes);
                    break;
                case RGB24:
                case GRAY_2_PLANES:
                case GRAY_4_PLANES:
                    timeStamp = frameData.getInt();
                    // The remainder of the data contains the planes.
                    planes = new byte[frameData.remaining()];
                    frameData.get(planes);
                    break;
                case COLOUR:
                    colour = frameData.getInt();
                    break;
                case PALETTE:
                    palette = paletteFromData(frameData);
                    break;
            }
        } catch (BufferUnderflowException e) {
            Timber.e(e, "Remaining frameData: %s(bytes) was less than expected for type: %s",
                    remainingFrameData, getFrameType());
            frameType = INVALID;
        }
    }

    private String stringFromData(final ByteBuffer data, final boolean skip) {
        int start = data.position();
        byte b = data.get();
        if (skip) {
            // skip over any null bytes at the beginning of the data.
            while (b == 0) {
                b = data.get();
                start++;
            }
        }
        int length = 0;
        // Look through the data until we find a null (0) byte or we hit the end of the data
        while (b != 0) {
            length++;
            if (data.hasRemaining()) {
                b = data.get();
            } else {
                break;
            }
        }
        return new String(data.array(), start, length);
    }

    private int[] paletteFromData(final ByteBuffer data) {
        // First int is how many palette items to expect
        int[] palette = new int[data.getInt()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = data.getInt();
        }
        return palette;
    }
}
