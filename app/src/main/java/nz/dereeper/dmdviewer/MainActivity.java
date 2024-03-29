/*
 * Copyright 2020-2021 Mark de Reeper
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import timber.log.Timber;

import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.inflate;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;
import static java.lang.Integer.parseInt;


public class MainActivity extends AppCompatActivity {

    static final String DMD_WS_PORT = "WS_PORT";
    static final String DMD_ENABLED = "DMD_ENABLED";
    static final String DMD_ROUND_PIXEL = "PIXEL_ROUND";

    private final int PERMISSIONS_REQUEST_ACCESS_WIFI_STATE = 0;
    private final int PERMISSIONS_REQUEST_INTERNET = 1;
    private boolean wifiEnabled = false;
    private boolean permissionsGrantedWifi = false;
    private boolean permissionsGrantedInternet = false;

    /**
     * Called as the result of the user hitting the start button.
     * @param view The {@link View} that triggered this call.
     */
    public void startDMDViewer(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final int port = parseInt(prefs.getString("websocket_port", "9090"));
        final boolean enabled = prefs.getBoolean("dmd_effect", true);
        final boolean round = prefs.getBoolean("round_pixel", false);
        final Intent intent = new Intent(this, DmdActivity.class);
        intent.putExtra(DMD_WS_PORT, port);
        intent.putExtra(DMD_ENABLED, enabled);
        intent.putExtra(DMD_ROUND_PIXEL, round);
        Timber.i("Opening DMD screen, passing port: %s enable DMD effect: %s, round: %s",
                port, enabled, round);
        startActivity(intent);
    }

    public void showSettingsActivity() {
        final Intent intent = new Intent(this, SettingsActivity.class);
        Timber.i("Opening Settings screen");
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about_menu_item) {
            showAboutDialog();
            return true;
        }
        if (item.getItemId() == R.id.settings_menu_item) {
            showSettingsActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_WIFI_STATE:
                permissionsGrantedWifi = grantResults.length > 0 &&
                                         grantResults[0] == PERMISSION_GRANTED;
                break;
            case PERMISSIONS_REQUEST_INTERNET:
                permissionsGrantedInternet = grantResults.length > 0 &&
                                             grantResults[0] == PERMISSION_GRANTED;
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        updateLabels();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean autoStart = prefs.getBoolean("auto_start", false);
        Timber.i("Auto start set to %s", autoStart);
        if (autoStart) {
            startDMDViewer(null);
        }
    }

    @Override
    protected void onResume() {
        updateLabels();
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.i("Closing MainActivity...");
    }

    private void showAboutDialog() {
        final View aboutView = inflate(this, R.layout.about_page, null);
        final TextView aboutText = aboutView.findViewById(R.id.aboutText);
        // This might show as a error in Studio but it is a valid format string, builds/runs OK.
        aboutText.setText(getString(R.string.about_text, BuildConfig.VERSION_NAME));
        final AlertDialog.Builder aboutWindow = new AlertDialog.Builder(this);//creates a new instance of a dialog box
        aboutWindow.setIcon(R.mipmap.dmd_gameover_round);
        aboutWindow.setTitle(R.string.app_name);
        aboutWindow.setView(aboutView);
        aboutWindow.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        aboutWindow.show();
    }

    private void updateLabels() {
        Timber.i("Updating labels");
        final String ipValue = getWifiIp();
        if (permissionsGrantedInternet && permissionsGrantedWifi && wifiEnabled) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String port = prefs.getString("websocket_port", "9090");
            final TextView wsUrl = findViewById(R.id.wsUrl);
            wsUrl.setText(getString(R.string.ws_url, ipValue, port));
        } else {
            Button startButton = findViewById(R.id.startButton);
            startButton.setEnabled(false);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_WIFI_STATE) != PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_WIFI_STATE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{ACCESS_WIFI_STATE},
                        PERMISSIONS_REQUEST_ACCESS_WIFI_STATE);
            }
        } else {
            permissionsGrantedWifi = true;
        }
        if (ContextCompat.checkSelfPermission(this, INTERNET) != PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, INTERNET)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{INTERNET},
                        PERMISSIONS_REQUEST_INTERNET);
            }
        } else {
            permissionsGrantedInternet = true;
        }
    }

    private String getWifiIp() {
        if (permissionsGrantedWifi) {
            final Object service = getApplicationContext().getSystemService(WIFI_SERVICE);
            if (service instanceof WifiManager) {
                WifiManager wifiManager = (WifiManager) service;
                if (wifiManager.isWifiEnabled()) {
                    wifiEnabled = true;
                    return Formatter.formatIpAddress((wifiManager.getConnectionInfo()
                                                                 .getIpAddress()));
                }
                wifiEnabled = false;
                makeText(this.getApplicationContext(), R.string.wifi_not_enabled, LENGTH_LONG).show();
            }
        } else {
            makeText(this.getApplicationContext(), R.string.wifi_needed, LENGTH_LONG).show();
        }
        return getString(R.string.wifi_ip_unknown);
    }
}
