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

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class DmdWebSocketServer extends WebSocketServer {

    private static final String TAG = "DMD_WS";
    private final Processing processing;

    DmdWebSocketServer(final Processing processing, final int port) {
        super(new InetSocketAddress(port));
        this.processing = processing;
    }

    @Override
    public void onMessage(final WebSocket webSocket, final ByteBuffer message) {
        Log.v(TAG, "Binary message received from client");
        processing.processFrame(new Frame(message.order(LITTLE_ENDIAN)));
    }

    @Override
    public void onMessage(final WebSocket webSocket, final String message) {
        Log.d(TAG, "Text message: " + message + " received from client");
    }

    @Override
    public void onOpen(final WebSocket webSocket, final ClientHandshake clientHandshake) {
        Log.i(TAG, "A new client connected");

    }

    @Override
    public void onClose(final WebSocket webSocket, final int i, final String s, final boolean b) {
        Log.i(TAG, "A client disconnected");
    }

    @Override
    public void onError(final WebSocket webSocket, final Exception e) {
        Log.e(TAG, "Exception triggered during WebSocket processing", e);
        processing.closeDown(e.getMessage());
    }

    @Override
    public void onStart() {
        Log.i(TAG, "Starting the WS Server on port:" + getAddress().getPort());
    }
}
