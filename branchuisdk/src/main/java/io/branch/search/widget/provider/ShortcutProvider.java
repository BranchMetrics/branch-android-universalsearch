package io.branch.search.widget.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.R;
import io.branch.search.widget.model.Shortcut;
import io.branch.search.widget.ui.DiscoveryViewHolder;
import io.branch.search.widget.ui.ShortcutViewHolder;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.util.WordMatcher;

import static android.provider.Settings.*;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 *
 * NOTE: Do not rename without changing the array.
 */
@Keep
@SuppressWarnings("unused")
public class ShortcutProvider
        extends SimpleDiscoveryProvider<Shortcut, DiscoveryViewModel<Shortcut>> {
    private static final String TAG = "Branch::Shortcut";
    private static final int NUM_COLUMNS = 2;

    private static List<Shortcut> sShortcuts;
    private static final List<Shortcut> ALL_SHORTCUTS;
    static {
        ArrayList<Shortcut> items = new ArrayList<>(Arrays.asList(
                // In alphabetical order:
                new Shortcut(ACTION_ACCESSIBILITY_SETTINGS, R.string.shortcut_accessibility),
                new Shortcut(ACTION_ADD_ACCOUNT, R.string.shortcut_add_account),
                new Shortcut(ACTION_AIRPLANE_MODE_SETTINGS, R.string.shortcut_airplane_mode,
                        R.drawable.branch_ic_airplanemode_24dp),
                // new Shortcut(ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                // ^ Developer options - works but not sure we want this.
                new Shortcut(ACTION_APPLICATION_SETTINGS, R.string.shortcut_applications),
                new Shortcut(ACTION_BLUETOOTH_SETTINGS, R.string.shortcut_bluetooth,
                        R.drawable.branch_ic_bluetooth_24dp),
                new Shortcut(ACTION_CAPTIONING_SETTINGS, R.string.shortcut_video_captions),
                new Shortcut(ACTION_CAST_SETTINGS, R.string.shortcut_cast),
                new Shortcut(ACTION_DATA_ROAMING_SETTINGS, R.string.shortcut_data_roaming),
                // ^ Useful duplicate of ACTION_NETWORK_OPERATOR_SETTINGS
                new Shortcut(ACTION_DATE_SETTINGS, R.string.shortcut_date_and_time),
                new Shortcut(ACTION_DEVICE_INFO_SETTINGS, R.string.shortcut_device_info),
                new Shortcut(ACTION_DISPLAY_SETTINGS, R.string.shortcut_display),
                new Shortcut(ACTION_DREAM_SETTINGS, R.string.shortcut_screen_saver),
                // new Shortcut(ACTION_FINGERPRINT_ENROLL)
                // ^ Not sure - won't work on my device
                new Shortcut(ACTION_INPUT_METHOD_SETTINGS, R.string.shortcut_virtual_keyboards),
                // new Shortcut(ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
                // ^ Really low level, let's avoid
                new Shortcut(ACTION_INTERNAL_STORAGE_SETTINGS, R.string.shortcut_storage),
                new Shortcut(ACTION_LOCALE_SETTINGS, R.string.shortcut_languages),
                new Shortcut(ACTION_LOCATION_SOURCE_SETTINGS, R.string.shortcut_location),
                // new Shortcut(ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
                // ^ Duplicate of ACTION_APPLICATION_SETTINGS
                // new Shortcut(ACTION_MANAGE_APPLICATIONS_SETTINGS),
                // ^ Duplicate of ACTION_APPLICATION_SETTINGS
                // new Shortcut(ACTION_MANAGE_OVERLAY_PERMISSION),
                // ^ Apps allowed to overlay. Really low level
                // new Shortcut(ACTION_MANAGE_UNKNOWN_APP_SOURCES),
                // ^ Apps allowed to install apps. Really low level
                // new Shortcut(ACTION_MANAGE_WRITE_SETTINGS),
                // ^ Apps allowed to modify system settings. Really low level
                new Shortcut(ACTION_MEMORY_CARD_SETTINGS, R.string.shortcut_memory_card),
                // ^ Useful duplicate of ACTION_INTERNAL_STORAGE_SETTINGS
                new Shortcut(ACTION_NETWORK_OPERATOR_SETTINGS,
                        R.string.shortcut_mobile_network),
                // new Shortcut(ACTION_NFC_PAYMENT_SETTINGS),
                // ^ On my device this does not work.
                new Shortcut(ACTION_NFC_SETTINGS, R.string.shortcut_nfc),
                new Shortcut(ACTION_NFCSHARING_SETTINGS, R.string.shortcut_android_beam),
                // new Shortcut(ACTION_NOTIFICATION_LISTENER_SETTINGS),
                // ^ Apps allowed to access notifications. Really low level
                // new Shortcut(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                // ^ Apps allowed to use Do Not Disturb feature. Really low level
                new Shortcut(ACTION_PRINT_SETTINGS, R.string.shortcut_printing),
                // new Shortcut(ACTION_PRIVACY_SETTINGS, R.string.shortcut_printing),
                // ^ This leaves me to Google Backup settings but that's not what the docs say.
                // new Shortcut(ACTION_QUICK_LAUNCH_SETTINGS),
                // ^ On my device this does not work.
                new Shortcut(ACTION_SEARCH_SETTINGS, R.string.shortcut_google_search),
                // ^ This should always open Google -> Search settings.
                new Shortcut(ACTION_SECURITY_SETTINGS, R.string.shortcut_security),
                new Shortcut(ACTION_SETTINGS, R.string.shortcut_settings),
                // new Shortcut(ACTION_SHOW_REGULATORY_INFO), // Pretty useless I would say.
                new Shortcut(ACTION_SOUND_SETTINGS, R.string.shortcut_sound_and_vibration),
                // new Shortcut(ACTION_STORAGE_VOLUME_ACCESS_SETTINGS),
                // ^ Apps allowed to access storage volume directories. Really low level
                new Shortcut(ACTION_SYNC_SETTINGS, R.string.shortcut_account_sync),
                // new Shortcut(ACTION_USAGE_ACCESS_SETTINGS),
                // ^ Apps allowed to access usage information. Really low level
                new Shortcut(ACTION_USER_DICTIONARY_SETTINGS,
                        R.string.shortcut_personal_dictionary),
                // new Shortcut(ACTION_VOICE_CONTROL_AIRPLANE_MODE),
                // ^ Voice control - not our case
                // new Shortcut(ACTION_VOICE_CONTROL_BATTERY_SAVER_MODE),
                // ^ Voice control - not our case
                // new Shortcut(ACTION_VOICE_CONTROL_DO_NOT_DISTURB_MODE),
                // ^ Voice control - not our case
                // new Shortcut(ACTION_VOICE_INPUT_SETTINGS),
                // ^ Apps -> Default Apps -> Assist & Voice Input. Quite low level
                // new Shortcut(ACTION_VR_LISTENER_SETTINGS),
                // ^ Virtual Reality app services, I'd remove this
                // new Shortcut(ACTION_WEBVIEW_SETTINGS),
                // ^ Chooses the WebView implementation. Extremely low-level.
                new Shortcut(ACTION_WIFI_SETTINGS, R.string.shortcut_wifi),
                new Shortcut(ACTION_WIFI_IP_SETTINGS, R.string.shortcut_wifi_preferences),
                new Shortcut(ACTION_WIRELESS_SETTINGS, R.string.shortcut_wireless,
                        R.drawable.branch_ic_network_wifi_24dp)
        ));

        // ACTION_ZEN_MODE_SETTINGS is marked as @hide but seems to work.
        items.add(new Shortcut("android.settings.ZEN_MODE_SETTINGS",
                R.string.shortcut_do_not_disturb));

        // Default Apps seems to have two different possible actions.
        @SuppressLint("InlinedApi")
        String defaultApps = (Build.VERSION.SDK_INT >= 24)
                ? ACTION_MANAGE_DEFAULT_APPS_SETTINGS
                : ACTION_HOME_SETTINGS; // Looks like pre 24 this does the same.
        items.add(new Shortcut(defaultApps, R.string.shortcut_default_apps));

        if (Build.VERSION.SDK_INT >= 22) {
            items.add(new Shortcut(ACTION_BATTERY_SAVER_SETTINGS,
                    R.string.shortcut_battery_saver));
        }
        if (Build.VERSION.SDK_INT >= 24) {
            items.add(new Shortcut(ACTION_HARD_KEYBOARD_SETTINGS,
                    R.string.shortcut_hard_keyboard));
            items.add(new Shortcut(ACTION_VPN_SETTINGS, R.string.shortcut_vpn));
        }
        if (Build.VERSION.SDK_INT >= 28) {
            items.add(new Shortcut(ACTION_DATA_USAGE_SETTINGS, R.string.shortcut_data_usage));
        }
        ALL_SHORTCUTS = items;
    }

    private final WordMatcher mWordMatcher = new WordMatcher();

    @Override
    public boolean initialize(@NonNull Context context,
                              @NonNull IDiscoveryProviderCallback callback,
                              @Nullable Object payload) {
        if (super.initialize(context, callback, payload)) {
            if (sShortcuts == null) {
                // We want to filter out the ALL_SHORTCUTS that can not be handled by any activity.
                // For this we need a PackageManager so it must be done here with a context handle.
                PackageManager manager = context.getPackageManager();
                ArrayList<Shortcut> results = new ArrayList<>();
                for (Shortcut item : ALL_SHORTCUTS) {
                    if (new Intent(item.getIntentAction()).resolveActivity(manager) != null) {
                        results.add(item);
                    } else {
                        Log.d(TAG, "Dropping shortcut " + item.getIntentAction()
                                + " because it does not resolve to any activity.");
                    }
                }
                sShortcuts = results;
            }
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    protected List<Shortcut> loadResults(@NonNull String query, int token, int capacity) {
        Context context = requireContext();
        ArrayList<Shortcut> results = new ArrayList<>();
        for (Shortcut item : sShortcuts) {
            if (mWordMatcher.matches(item.getLabel(context), query)) {
                results.add(item);
            }
        }
        return results;
    }

    @Override
    protected void launchResult(@NonNull Shortcut item, @Nullable Object payload, int position) {
        // This should never crash because we check with resolveActivity()
        // before getting here, so it should always work.
        startActivity(new Intent(item.getIntentAction()));

        position++; // 1 based
        new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                        getClass().getSimpleName())
                .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                        String.valueOf(position))
                .logEvent(requireContext());
    }

    @Override
    protected float getAdapterSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.branch_shortcut_vertical_spacing);
    }

    @Override
    protected int getAdapterColumns() {
        return NUM_COLUMNS;
    }

    @Nullable
    @Override
    protected CharSequence getAdapterHeader() {
        return getResources().getString(R.string.branch_shortcut_provider_title);
    }

    @Override
    protected DiscoveryViewHolder<Shortcut> createAdapterViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @NonNull DiscoveryViewHolder.Callback<Shortcut> callback) {
        return new ShortcutViewHolder(inflater, parent, callback);
    }
}