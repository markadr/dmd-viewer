DMD Viewer
==========

A simple Android app that can be used in conjunction with the dmdext network streaming [option](https://github.com/freezy/dmd-extensions/tree/master/Console/Server), available since v1.8 of dmdext.  
Since dmdext v1.8.2, there is a new retry option, that when enabled, will support dmdext retrying web socket connections which means DMD Viewer does not need to be running before starting dmdext.

## Building
This app is built using [Android Studio](https://developer.android.com/studio). There are some prebuilt versions of this app in the github project. To get the latest, clone and open in Studio to build and install on to your Android device after enabling [developer](https://developer.android.com/studio/debug/dev-options) mode.

## Running
This app has only been tested on a few phones, a couple running Android v6.0.1 and one running v9. Anything above v4.1 should work but has not been tested.

## App Permissions
- Wifi status permission
  - required so the app can discover it's own IP address and display the final WebSocket URL to be used in the dmdext configuration.
- Internet permission
  - required so the app can open up a network socket and act as a WebSocket Server and receive DMD frames from dmdext.

## Using DMD Viewer
Take the provided WS URL shown on the opening screen of this app and use this on the dmdext side as either:
- in the **url** of the **networkstream** option in **DmdDevice.ini**, be sure to also set **enabled** to **true**, or
- as the value passed to the **--url** parameter, used in conjunction with the **-d network** parameter, to the **dmdext.exe** commandline tool.

When not using v1.8.2 of dmdext and/or retries are not enabled, before starting a table/dmdext, make sure to have clicked the **Start DMD** button on the app so that the WebSocket Server is started and ready for connections from dmdext.

By default, the app makes use of a simple LED matrix effect to make the frames look like they are being displayed on a LED matrix display, this can be disabled if required.

## FAQ
1 Do I have to restart the table/dmdext every time I exit out of the app or change an option?
- Since dmdext v1.8.2, there is a new retry option, that when enabled, will support dmdext retrying web socket connections which means the viewer does not need to be running before starting dmdext.

2 Is there a performance overhead of using the LED Matrix effect?
- Yes there is a slight performance hit when using the LED Matrix effect as for each pixel, at least 4 have to be calculated/drawn as a result. On a really low-end Android phone this may be noticeable so disable LED matrix effect if you notice update delays.

3 What third-party libraries are used?
- WebSocket support is provided by the [Java WebSockets](https://github.com/TooTallNate/Java-WebSocket) library.