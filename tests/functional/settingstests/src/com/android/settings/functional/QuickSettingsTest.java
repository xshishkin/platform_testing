/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.settings.functional;

import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Until;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

public class QuickSettingsTest extends InstrumentationTestCase {
    private static final String LOG_TAG = QuickSettingsTest.class.getSimpleName();
    private static final int LONG_TIMEOUT = 2000;
    private static final int SHORT_TIMEOUT = 500;

    private enum QuickSettingTiles {
        WIFI("Wifi"), SIM("SIM"), DND("Do not disturb"), FLASHLIGHT("Flashlight"), SCREEN(
                "Screen"), BLUETOOTH("Bluetooth"), AIRPLANE("Airplane mode"), LOCATION(
                        "Location");

        private final String name;

        private QuickSettingTiles(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    };

    private UiDevice mDevice = null;
    private ContentResolver mResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        getInstrumentation().getContext();
        mResolver = getInstrumentation().getContext().getContentResolver();
        mDevice.wakeUp();
        mDevice.pressHome();
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientaion", e);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        // Need to finish settings activity
        mDevice.pressHome();
        super.tearDown();
    }

    @MediumTest
    public void testQuickSettingDrawDown() throws Exception {
        mDevice.pressHome();
        swipeDown();
        UiObject2 quicksettingsShade = mDevice.wait(
                Until.findObject(By.descContains(QuickSettingTiles.WIFI.getName())),
                LONG_TIMEOUT);
        assertNotNull(quicksettingsShade);
    }

    @MediumTest
    public void testQuickSettingExpand() throws Exception {
        mDevice.pressHome();
        swipeDown();
        swipeDown();
        UiObject2 quicksettingsExpand = mDevice.wait(Until.findObject(By.desc("Settings")),
                LONG_TIMEOUT);
        assertNotNull(quicksettingsExpand);
    }

    @MediumTest
    public void testQuickSettingCollapse() throws Exception {
        mDevice.pressHome();
        swipeDown();
        swipeDown();
        mDevice.wait(Until.findObject(By.res("com.android.systemui:id/expand_indicator")),
                LONG_TIMEOUT).click();
        UiObject2 quicksettingsShade = mDevice.wait(
                Until.findObject(By.descContains(QuickSettingTiles.WIFI.getName())),
                LONG_TIMEOUT);
        assertNotNull(quicksettingsShade);
    }

    @MediumTest
    public void testQuickSettingDismiss() throws Exception {
        mDevice.pressHome();
        swipeDown();
        swipeDown();
        swipeUp();
        UiObject2 quicksettingsShade = mDevice.wait(
                Until.findObject(By.res("com.android.systemui:id/expand_indicator")),
                SHORT_TIMEOUT);
        assertNull(quicksettingsShade);
    }

    @MediumTest
    public void testQuickSettingTiles() throws Exception {
        mDevice.pressHome();
        swipeDown();
        swipeDown();
        Thread.sleep(LONG_TIMEOUT);
        for (QuickSettingTiles tile : QuickSettingTiles.values()) {
            UiObject2 quickSettingTile = mDevice.wait(
                    Until.findObject(By.descContains(tile.getName())),
                    SHORT_TIMEOUT);
            assertNotNull(String.format("%s did not load correctly", tile.getName()),
                    quickSettingTile);
        }
    }

    @MediumTest
    public void testQuickSettingWifiEnabled() throws Exception {
        verifyWiFiOnOrOff(true);
    }

    @MediumTest
    public void testQuickSettingWifiDisabled() throws Exception {
        verifyWiFiOnOrOff(false);
    }

    private void verifyWiFiOnOrOff(boolean verifyOn) throws Exception {
        String airPlaneMode = Settings.Global.getString(
                mResolver,
                Settings.Global.AIRPLANE_MODE_ON);
        WifiManager wifiManager = (WifiManager) getInstrumentation().getContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(!verifyOn);
        launchQuickSetting();
        mDevice.wait(Until.findObject(By.descContains(QuickSettingTiles.WIFI.getName())),
                LONG_TIMEOUT).click();
        if (verifyOn) {
            mDevice.pressBack();
        } else {
            mDevice.wait(Until.findObject(By.res("android:id/toggle")), LONG_TIMEOUT).click();
        }
        Thread.sleep(LONG_TIMEOUT);
        String wifiValue = Settings.Global.getString(mResolver, Settings.Global.WIFI_ON);
        if (verifyOn) {
            if (airPlaneMode.equals("1")) {
                assertEquals("2", wifiValue);
            } else {
                assertFalse(wifiValue.equals("0"));
            }
        } else {
            assertEquals("0", wifiValue);
        }
    }

    @MediumTest
    public void testQuickSettingBluetoothEnabled() throws Exception {
        verifyBluetoothOnOrOff(true);
    }

    @MediumTest
    public void testQuickSettingBluetoothDisabled() throws Exception {
        verifyBluetoothOnOrOff(false);
    }

    private void verifyBluetoothOnOrOff(boolean verifyOn) throws Exception {
        BluetoothManager bluetoothManager = (BluetoothManager) getInstrumentation().getContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (!verifyOn) {
            bluetoothManager.getAdapter().enable();
        } else {
            bluetoothManager.getAdapter().disable();
        }
        launchQuickSetting();
        mDevice.wait(Until.findObject(By.textContains(QuickSettingTiles.BLUETOOTH.getName())),
                LONG_TIMEOUT).click();
        if (verifyOn) {
            mDevice.pressBack();
        } else {
            mDevice.wait(Until.findObject(By.res("android:id/toggle")), LONG_TIMEOUT).click();
        }
        Thread.sleep(LONG_TIMEOUT);
        String bluetoothVal = Settings.Global.getString(
                mResolver,
                Settings.Global.BLUETOOTH_ON);
        if (verifyOn) {
            assertEquals("1", bluetoothVal);
        } else {
            assertEquals("0", bluetoothVal);
        }
    }

    @MediumTest
    public void testQuickSettingFlashLight() throws Exception {
        String lightOn = "Flashlight on.";
        String lightOff = "Flashlight off.";
        boolean verifyOn = false;
        launchQuickSetting();
        UiObject2 flashLight = mDevice.wait(
                Until.findObject(By.descContains(QuickSettingTiles.FLASHLIGHT.getName())),
                LONG_TIMEOUT);
        if (flashLight.getContentDescription().equals(lightOn)) {
            verifyOn = true;
        }
        mDevice.wait(Until.findObject(By.textContains(QuickSettingTiles.FLASHLIGHT.getName())),
                LONG_TIMEOUT).click();
        Thread.sleep(LONG_TIMEOUT);
        flashLight = mDevice.wait(
                Until.findObject(By.descContains(QuickSettingTiles.FLASHLIGHT.getName())),
                LONG_TIMEOUT);
        if (verifyOn) {
            assertTrue(flashLight.getContentDescription().equals(lightOff));
        } else {
            assertTrue(flashLight.getContentDescription().equals(lightOn));
            mDevice.wait(Until.findObject(By.textContains(QuickSettingTiles.FLASHLIGHT.getName())),
                    LONG_TIMEOUT).click();
        }
    }

    @MediumTest
    public void testQuickSettingDND() throws Exception {
        int onSetting = Settings.Global.getInt(mResolver, "zen_mode");
        launchQuickSetting();
        mDevice.wait(Until.findObject(By.descContains(QuickSettingTiles.DND.getName())),
                LONG_TIMEOUT).click();
        if (onSetting == 0) {
            mDevice.pressBack();
        }
        Thread.sleep(LONG_TIMEOUT);
        int changedSetting = Settings.Global.getInt(mResolver, "zen_mode");
        assertFalse(onSetting == changedSetting);
    }

    @MediumTest
    public void testQuickSettingAirplaneMode() throws Exception {
        int onSetting = Integer.parseInt(Settings.Global.getString(
                mResolver,
                Settings.Global.AIRPLANE_MODE_ON));
        try {
            launchQuickSetting();
            mDevice.wait(Until.findObject(By.descContains(QuickSettingTiles.AIRPLANE.getName())),
                    LONG_TIMEOUT).click();
            Thread.sleep(LONG_TIMEOUT);
            int changedSetting = Integer.parseInt(Settings.Global.getString(
                    mResolver,
                    Settings.Global.AIRPLANE_MODE_ON));
            assertTrue((1 - onSetting) == changedSetting);
        } finally {
            Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, Integer.toString(onSetting));
        }
    }

    @MediumTest
    public void testQuickSettingOrientation() throws Exception {
        launchQuickSetting();
        mDevice.wait(Until.findObject(By.descContains(QuickSettingTiles.SCREEN.getName())),
                LONG_TIMEOUT).click();
        Thread.sleep(LONG_TIMEOUT);
        String rotation = Settings.System.getString(mResolver,
                Settings.System.ACCELEROMETER_ROTATION);
        assertEquals("1", rotation);
    }

    @MediumTest
    public void testQuickSettingLocation() throws Exception {
        LocationManager service = (LocationManager) getInstrumentation().getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        boolean onSetting = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        try {
            launchQuickSetting();
            mDevice.wait(Until.findObject(By.descContains(QuickSettingTiles.LOCATION.getName())),
                    LONG_TIMEOUT).click();
            Thread.sleep(LONG_TIMEOUT);
            boolean changedSetting = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
            assertTrue(onSetting == !changedSetting);
        } finally {
            mDevice.wait(Until.findObject(By.descContains(QuickSettingTiles.LOCATION.getName())),
                    LONG_TIMEOUT).click();
        }
    }

    private void launchQuickSetting() throws Exception {
        mDevice.pressHome();
        swipeDown();
        Thread.sleep(LONG_TIMEOUT);
        swipeDown();
    }

    private void swipeUp() throws Exception {
        mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight(),
                mDevice.getDisplayWidth() / 2, 0, 30);
        Thread.sleep(SHORT_TIMEOUT);
    }

    private void swipeDown() throws Exception {
        mDevice.swipe(mDevice.getDisplayWidth() / 2, 0, mDevice.getDisplayWidth() / 2,
                mDevice.getDisplayHeight() / 2 + 50, 20);
        Thread.sleep(SHORT_TIMEOUT);
    }

    private void swipeLeft() {
        mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight() / 2, 0,
                mDevice.getDisplayHeight() / 2, 5);
    }
}
