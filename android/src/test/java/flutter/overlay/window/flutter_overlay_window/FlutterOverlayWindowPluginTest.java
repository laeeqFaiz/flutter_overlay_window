package flutter.overlay.window.flutter_overlay_window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

import java.lang.reflect.Field;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FlutterOverlayWindowPluginTest {
    private FlutterOverlayWindowPlugin plugin;
    private RecordingResult result;
    private RecordingLauncher launcher;

    @Before
    public void setUp() throws Exception {
        plugin = new FlutterOverlayWindowPlugin();
        result = new RecordingResult();
        launcher = new RecordingLauncher();

        setField("context", RuntimeEnvironment.getApplication());
        setField("mActivity", new ComponentActivity());
        setField("overlayPermissionLauncher", launcher);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void requestPermissionBelowApi23ReturnsTrueWithoutLaunchingSettings() {
        requestPermission();

        assertEquals(true, result.successValue);
        assertNull(launcher.launchedIntent);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void requestPermissionOnApi23LaunchesOverlaySettings() {
        assertOverlaySettingsLaunched();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N)
    public void requestPermissionOnApi24LaunchesOverlaySettings() {
        assertOverlaySettingsLaunched();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void requestPermissionOnApi25LaunchesOverlaySettings() {
        assertOverlaySettingsLaunched();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void requestPermissionOnApi26LaunchesOverlaySettings() {
        assertOverlaySettingsLaunched();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void requestPermissionAboveApi26LaunchesOverlaySettings() {
        assertOverlaySettingsLaunched();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void returningFromSettingsReportsCurrentOverlayPermission() {
        ShadowSettings.setCanDrawOverlays(false);
        requestPermission();
        plugin.completeOverlayPermissionRequest();
        assertEquals(false, result.successValue);

        result = new RecordingResult();
        ShadowSettings.setCanDrawOverlays(true);
        requestPermission();
        plugin.completeOverlayPermissionRequest();
        assertEquals(true, result.successValue);
    }

    private void assertOverlaySettingsLaunched() {
        requestPermission();

        assertNull(result.successValue);
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, launcher.launchedIntent.getAction());
        Context context = RuntimeEnvironment.getApplication();
        assertEquals("package:" + context.getPackageName(), launcher.launchedIntent.getDataString());
    }

    private void requestPermission() {
        plugin.onMethodCall(new MethodCall("requestPermission", null), result);
    }

    private void setField(String name, Object value) throws Exception {
        try {
            Field field = FlutterOverlayWindowPlugin.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(plugin, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class RecordingLauncher extends ActivityResultLauncher<Intent> {
        private Intent launchedIntent;

        @Override
        public void launch(Intent input, @Nullable ActivityOptionsCompat options) {
            launchedIntent = input;
        }

        @Override
        public void unregister() {}

        @NonNull
        @Override
        public ActivityResultContract<Intent, ?> getContract() {
            return new ActivityResultContracts.StartActivityForResult();
        }
    }

    private static final class RecordingResult implements MethodChannel.Result {
        private Object successValue;

        @Override
        public void success(@Nullable Object result) {
            successValue = result;
        }

        @Override
        public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {
            throw new AssertionError(errorCode + ": " + errorMessage);
        }

        @Override
        public void notImplemented() {
            throw new AssertionError("Method was not implemented");
        }
    }
}
