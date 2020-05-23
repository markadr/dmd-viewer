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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.java_websocket.server.WebSocketServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import static android.graphics.Bitmap.createBitmap;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static androidx.core.graphics.ColorUtils.colorToHSL;
import static nz.dereeper.dmdviewer.Frame.FrameType.COLORED_GRAY_2;
import static nz.dereeper.dmdviewer.Frame.FrameType.GRAY_2_PLANES;
import static nz.dereeper.dmdviewer.ImageUtils.toRawImage;
import static nz.dereeper.dmdviewer.ImageUtils.toRawImageFromRgb24;
import static nz.dereeper.dmdviewer.MainActivity.DMD_WS_PORT;
import static nz.dereeper.dmdviewer.MainActivity.DMD_LED_ENABLED;


public class DmdActivity extends AppCompatActivity implements Processing, Metadata {

    private static final String TAG = "DMD";
    private static final int DEFAULT_COLOUR = 0xec843d;
    private static final int viewUISettings = View.SYSTEM_UI_FLAG_LOW_PROFILE |
                                              View.SYSTEM_UI_FLAG_FULLSCREEN |
                                              View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                              View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                              View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    private ImageView dmdView;
    private Bitmap dmdImage;
    private int dmdImageWidth;
    private int dmdImageHeight;
    private WebSocketServer webSocketServer;
    private Dimensions dimensions = null;
    private String gameName;
    private float[] hsl = new float[3];
    private int[] palette;
    private LedMatrix ledMatrix;
    private Frame previousFrame;
    private Frame openingFrame;

    @Override
    public LedMatrix getLedMatrix() {
        return ledMatrix;
    }

    @Override
    public Dimensions getDimensions() {
        return dimensions;
    }

    @Override
    public float[] getHslCopy() {
        // Provide a copy as the value gets updated before being used to generate a pixel colour.
        return hsl.clone();
    }

    @Override
    public void closeDown(final String errorMessage) {
        Log.i(TAG, "Closing DmdActivity...");
        getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
        stopServer();
        if (errorMessage != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Closing down due to an error: \n" + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
        finish();
    }

    @Override
    public void processFrame(final Frame frame) {
        switch (frame.getFrameType()) {
            case COLORED_GRAY_4:
            case COLORED_GRAY_2:
            case GRAY_2_PLANES:
            case GRAY_4_PLANES:
            case RGB24:
                if (isNewFrame(frame)) {
                    Log.v(TAG, frame.getFrameType() + ", timestamp:" + frame.getTimeStamp());
                    if (renderFrame(frame)) {
                        // If this frame resulted in an image being generated,
                        // keep track of the previous frame so it can be compared against
                        // the next one to avoid processing a duplicate.
                        previousFrame = frame;
                    }
                } else {
                    Log.v(TAG, "Skipping duplicate frame of type: " + frame.getFrameType());
                }
                break;
            case COLOUR:
                Log.i(TAG, "Colour frame: " + frame.getColour());
                setColour(frame.getColour());
                break;
            case PALETTE:
                palette = frame.getPalette();
                Log.i(TAG, "Palette frame of length: " + palette.length);
                break;
            case DIMENSIONS:
                setDimensions(frame.getDimensions());
                break;
            case CLEAR_COLOUR:
                Log.i(TAG, "Clear colour frame");
                setColour(DEFAULT_COLOUR);
                break;
            case CLEAR_PALETTE:
                Log.i(TAG, "Clear palette frame");
                palette = null;
                break;
            case GAME_NAME:
                gameName = frame.getGameName();
                Log.i(TAG, "Game name frame: " + gameName);
                break;
            case UNKNOWN:
                Log.i(TAG, "Binary message received is unknown type");
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmd);
        dmdView = findViewById(R.id.dmdView);
        getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        setColour(DEFAULT_COLOUR);
        // TODO: make the params of the LedMatrix configurable via UI
        //  A 2x2 with a 1 pixel margin good to start with.
        ledMatrix = new LedMatrix(2, 2, 1, getIntent().getBooleanExtra(DMD_LED_ENABLED, true));
        Log.i(TAG, "LED Matrix: " + ledMatrix);
        openingFrame = createOpeningFrame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // No point in running the server if we are being paused.
        stopServer();
    }

    @Override
    protected void onResume() {
        webSocketServer = new DmdWebSocketServer(this, getIntent().getIntExtra(DMD_WS_PORT, 9090));
        webSocketServer.start();
        super.onResume();
        // Hide elements of the UI since we are working in fullscreen mode
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(viewUISettings);
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // Attempt to show game name and dimensions if they have been set.
                            final ActionBar actionBar = getSupportActionBar();
                            if (actionBar != null) {
                                actionBar.show();
                            }
                            // Attempt to return to fullscreen mode after a delay has passed
                            decorView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (actionBar != null) {
                                        actionBar.hide();
                                    }
                                    decorView.setSystemUiVisibility(viewUISettings);
                                }
                            }, 5000); // 5sec delay before hiding UI items again.
                        }
                    }
                });
        showOpeningFrame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDown(null);
    }

    private void stopServer() {
        if (webSocketServer != null) {
            try {
                Log.i(TAG, "Stopping the WS Server");
                // Give it some time to clean up on stopping.
                webSocketServer.stop(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "We had an exception when trying to stop the WS Server", e);
            }
            webSocketServer = null;
        }
    }

    private void setColour(final int c) {
        colorToHSL(c, hsl);
    }

    private void setDimensions(final Dimensions dimensions) {
        this.dimensions = dimensions;
        if (ledMatrix.enabled) {
            dmdImageWidth = dimensions.width * ledMatrix.ledWidth;
            dmdImageHeight = dimensions.height * ledMatrix.ledHeight;
        } else {
            dmdImageWidth = dimensions.width;
            dmdImageHeight = dimensions.height;
        }
        // If the dimensions change, hint to the system that the old bitmap is no longer needed.
        if (dmdImage != null) {
            dmdImage.recycle();
        }
        dmdImage = createBitmap(dmdImageWidth, dmdImageHeight, Bitmap.Config.ARGB_8888);
        dmdImage.setHasAlpha(false);
        Log.i(TAG, "Dimensions frame: " + dimensions);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (gameName != null) {
                        actionBar.setSubtitle(gameName + " - " + dimensions.toString());
                    } else {
                        actionBar.setSubtitle(dimensions.toString());
                    }
                }
            });
        }
    }

    private boolean isNewFrame(final Frame currentFrame) {
        if (previousFrame == null) {
            return true;
        }
        // Flagged as a new frame if they are different frame types or they don't contain the exact
        // same set of planes data.
        return !currentFrame.getFrameType().equals(previousFrame.getFrameType()) ||
               !Arrays.equals(currentFrame.getPlanes(), previousFrame.getPlanes());
    }

    private boolean renderFrame(final Frame frame) {
        // Don't try to process any frames that may come before we know the size of the display
        if (dimensions != null) {
            final int[] rawImage;
            switch (frame.getFrameType()) {
                case GRAY_2_PLANES:
                case GRAY_4_PLANES:
                    final int numberOfGrays = frame.getFrameType().equals(GRAY_2_PLANES) ? 2 : 4;
                    final int expectedPaletteSize = numberOfGrays * 4;
                    // If we don't have a valid palette to match the required size,
                    // fall-back to HSL colour.
                    if (palette != null && palette.length == expectedPaletteSize) {
                        rawImage = ImageUtils.toRawImage(frame.getPlanes(), palette, numberOfGrays, this);
                    } else {
                        rawImage = toRawImage(frame.getPlanes(), expectedPaletteSize, numberOfGrays, this);
                    }
                    break;
                case COLORED_GRAY_2:
                case COLORED_GRAY_4:
                    final int numberOfColours = frame.getFrameType().equals(COLORED_GRAY_2) ? 2 : 4;
                    rawImage = ImageUtils.toRawImage(frame.getPlanes(), frame.getPalette(), numberOfColours, this);
                    break;
                case RGB24:
                    rawImage = toRawImageFromRgb24(frame.getPlanes(), this);
                    break;
                default:
                    rawImage = null;
                    break;
            }
            if (rawImage != null) {
                // Overwrite the existing image with our new raw image values.
                dmdImage.setPixels(rawImage, 0, dmdImageWidth, 0, 0, dmdImageWidth, dmdImageHeight);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dmdView.setImageBitmap(dmdImage);
                    }
                });
                return true;
            }
        }
        return false;
    }

    private void showOpeningFrame() {
        if (openingFrame != null) {
            // We know the dimensions of the opening frame, trigger the creation of the bitmap.
            setDimensions(new Dimensions(128, 32));
            renderFrame(openingFrame);
        }
    }

    private Frame createOpeningFrame() {
        DataInputStream frameStream = null;
        try {
            frameStream = new DataInputStream(getResources().openRawResource(R.raw.openingframe));
            byte[] openingFrameBytes = new byte[frameStream.available()];
            frameStream.readFully(openingFrameBytes);
            return new Frame(openingFrameBytes);
        } catch (IOException e) {
            Log.w(TAG, "There was an issue creating the opening frame", e);
        } finally {
            if (frameStream != null) {
                try {
                    frameStream.close();
                } catch (IOException e) {
                    // We tried, ignore.
                }
            }
        }
        return null;
    }
}
