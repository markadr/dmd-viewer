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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static nz.dereeper.dmdviewer.Frame.FrameType.COLOUR;
import static nz.dereeper.dmdviewer.Frame.FrameType.DIMENSIONS;
import static nz.dereeper.dmdviewer.Frame.FrameType.GAME_NAME;
import static nz.dereeper.dmdviewer.Frame.FrameType.INVALID;
import static nz.dereeper.dmdviewer.Frame.FrameType.PALETTE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class FrameTest {

    @DataProvider
    public static Object[][] validData() {
        return new Object[][]{
                {
                    GAME_NAME,
                    new byte[] { 0x67, 0x61, 0x6D, 0x65, 0x4E, 0x61, 0x6D, 0x65, 0x00,
                                 0x61, 0x66, 0x6D, 0x5F, 0x31, 0x31, 0x33, 0x62 },
                    "afm_113b"
                },
                {
                    DIMENSIONS,
                    new byte[] { 0x64, 0x69, 0x6D, 0x65, 0x6E, 0x73, 0x69, 0x6F, 0x6E, 0x73, 0x00,
                                (byte) 0x80, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00 },
                    new Dimensions(128, 32)
                },
                {
                    COLOUR,
                    new byte[] { 0x63, 0x6F, 0x6C, 0x6F, 0x72, 0x00, 0x00, 0x45, (byte) 0xFF, 0x00},
                    0xFF4500,
                },
                {
                    PALETTE,
                    new byte[] { 0x70, 0x61, 0x6C, 0x65, 0x74, 0x74, 0x65, 0x00,
                                 0x04, 0x00, 0x00, 0x00,
                                 0x00, 0x00, 0x00, 0x00,
                                (byte) 0xD6, 0x00, 0x06, 0x00,
                                (byte) 0xC1, 0x0F, (byte) 0xE1, 0x00,
                                 0x20, 0x58, (byte) 0xFF, 0x00 },
                    new int[] { 0, 0x600D6, 0xE10FC1, 0xFF5820 }
                }
        };
    }

    @Test(dataProvider = "validData")
    public void testValidFrames(final Frame.FrameType frameType, final byte[] rawData, Object expected) {
        final Frame frame = new Frame(wrap(rawData).order(LITTLE_ENDIAN));
        assertEquals(frameType, frame.getFrameType());
        switch (frameType) {
            case GAME_NAME:
                assertEquals(frame.getGameName(), expected);
                break;
            case DIMENSIONS:
                assertEquals(frame.getDimensions(), expected);
                break;
            case COLOUR:
                assertEquals(frame.getColour(), expected);
                break;
            case PALETTE:
                assertEquals(frame.getPalette().length, ((int[]) expected).length);
                assertTrue(Arrays.equals(frame.getPalette(), (int[]) expected));
                break;
            default:
                fail("validData frameType:" + frameType + " not covered by test case");
        }
    }

    @DataProvider
    public static Object[][] invalidData() {
        return new Object[][] {
                { new byte[] { 0x64, 0x69, 0x6D, 0x65, 0x6E, 0x73, 0x69, 0x6F, 0x6E, 0x73, 0x00,
                               (byte) 0x80, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00 } },
                { new byte[] { 0x63, 0x6F, 0x6C, 0x6F, 0x72, 0x00, 0x00, 0x45 } },
                { new byte[] { 0x70, 0x61, 0x6C, 0x65, 0x74, 0x74, 0x65, 0x00,
                               0x04, 0x00, 0x00, 0x00,
                               0x00, 0x00, 0x00, 0x00,
                              (byte) 0xD6, 0x00, 0x06, 0x00,
                              (byte) 0xC1, 0x0F, (byte) 0xE1, 0x00 } }
        };
    }

    @Test(dataProvider = "invalidData")
    public void testInvalidFrames(final byte[] rawData) {
        final Frame invalidFrame = new Frame(wrap(rawData).order(LITTLE_ENDIAN));
        assertEquals(invalidFrame.getFrameType(), INVALID);
    }
}