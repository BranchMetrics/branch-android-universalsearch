package io.branch.search.widget.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.BranchSearchController;
import io.branch.search.widget.BranchSearchResultsView;
import io.branch.search.widget.BranchSearchCallback;
import io.branch.search.widget.R;
import io.branch.search.widget.provider.BaseBranchProvider;
import io.branch.search.widget.provider.BaseContactsProvider;
import io.branch.search.widget.provider.IDiscoveryProvider;
import io.branch.search.widget.provider.InAppSearchProvider;
import io.branch.search.widget.provider.MediaProvider;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.query.BranchQueryMetadata;
import io.branch.search.widget.query.BranchQuerySource;

/**
 * One of the pages inside a {@link BranchTabsFragment}. This holds the actual Branch
 * Discovery code.
 */
public class BranchTabDiscoveryFragment extends Fragment {

    private static final String TAG = BranchTabDiscoveryFragment.class.getSimpleName();
    private static final int SPEECH_REQUEST_CODE = 409;

    private BranchSearchController mSearchController;
    private BranchQuerySource mQuerySource = BranchQuerySource.KEYBOARD;
    private long mLastClickTime = 0;

    @NonNull
    private BranchSearchFragment getSearchFragment() {
        //noinspection ConstantConditions
        return (BranchSearchFragment) getParentFragment().getParentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.branchapp_tab_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final BranchSearchResultsView resultsView = view.findViewById(R.id.branchapp_results_view);
        final EditText editTextView = view.findViewById(R.id.branchapp_edit_text);
        final View cancelView = view.findViewById(R.id.branchapp_cancel);
        final View micView = view.findViewById(R.id.branchapp_microphone);
        final View settingsView = view.findViewById(R.id.branchapp_settings);
        final View searchView = view.findViewById(R.id.branchapp_search_icon);
        final ProgressBar loadingView = view.findViewById(R.id.branchapp_loading_indicator);

        // Make sure Branch search controller receives the editor action events
        editTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return mSearchController.onEditorAction(v, actionId, event);
            }
        });

        // Make sure Branch search controller receives the text change events
        editTextView.addTextChangedListener(new TextWatcher() {
            boolean mSelfChange = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mSelfChange) return;
                mSelfChange = true;
                // Applying metadata can modify the Editable resulting in a onTextChanged loop,
                // so we must keep track of self changes with a boolean flag.
                s = BranchQueryMetadata.set(s, BranchQueryMetadata.SOURCE, mQuerySource);
                mSearchController.onTextChanged(s);
                mSelfChange = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                cancelView.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                micView.setVisibility(s.length() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        // When cancel is clicked, remove query.
        cancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTextView.setText("");
            }
        });

        // When microphone is clicked, start recording.
        micView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                try {
                    startActivityForResult(intent, SPEECH_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(requireContext(),
                            R.string.branchapp_speech_unavailable,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Initialize the Branch search controller with all of the above
        mSearchController = getSearchFragment().initializeSearchController(
                getChildFragmentManager(), resultsView);
        mSearchController.addCallback(new BranchSearchCallback() {
            @Override
            public void onQueryUpdateRequested(@NonNull CharSequence newQuery) {
                mQuerySource = BranchQuerySource.AUTO_COMPLETE;
                editTextView.setText(newQuery);
                mQuerySource = BranchQuerySource.KEYBOARD;
            }

            @Override
            public void onProviderResultClicked(@NonNull IDiscoveryProvider provider,
                                                @NonNull String query,
                                                @NonNull Object result) {
                super.onProviderResultClicked(provider, query, result);
                mLastClickTime = System.currentTimeMillis();
            }

            @Override
            public void onLoadingStateChanged(boolean loading) {
                super.onLoadingStateChanged(loading);
                loadingView.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
                searchView.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
            }
        });

        // Activate/deactivate providers based on preferences.
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(requireContext());
        final boolean hasBranchSearch = preferences.getBoolean("provider_branch",
                true);
        boolean hasFilesSearch = preferences.getBoolean("provider_files", true);
        boolean hasContactsSearch = preferences.getBoolean("provider_contacts", true);
        boolean showNonInstalledApps = preferences.getBoolean("non_installed_apps",
                true);
        mSearchController.setActive(BaseBranchProvider.class, hasBranchSearch);
        mSearchController.setActive(MediaProvider.class, hasFilesSearch);
        mSearchController.setActive(BaseContactsProvider.class, hasContactsSearch);
        mSearchController.getProvider(InAppSearchProvider.class)
                .setShowNonInstalledAppResults(showNonInstalledApps);

        // Request permissions if needed
        mSearchController.checkPermissions(this);

        // Make sure we start with a "" value. At this point the EditText should already be attached
        // to the window, because we are using a ViewPager. In other cases, a
        // View.OnAttachStateChangeListener should be preferred.
        if (savedInstanceState == null) {
            if (editTextView.isAttachedToWindow()) {
                editTextView.setText("");
            } else {
                editTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View view) {
                        editTextView.setText("");
                        editTextView.removeOnAttachStateChangeListener(this);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View view) {
                    }
                });
            }
        }

        // Open settings if needed.
        settingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(requireContext(), v);
                menu.inflate(R.menu.branchapp_search_options);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.branchapp_options_settings) {
                            getSearchFragment().navigateToSettings();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                menu.show();
            }
        });

        // Change search icon color if requested.
        editTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                searchView.setEnabled(hasBranchSearch);
            }
        });
    }

    /**
     * Show and hide the keyboard when we are made visible or hidden.
     * Note: this callback is used because we are stored inside a {@link ViewPager}.
     *
     * If we weren't, we could probably use {@link #onViewCreated(View, Bundle)}
     * and {@link #onDestroyView()} to show and hide keyboard.
     *
     * @param isVisibleToUser true if visible
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        onVisible(isVisibleToUser);
        if (isVisibleToUser && getContext() != null) {
            new BranchEvent(BranchEvents.TYPE_OPEN)
                    .addCustomDataProperty(BranchEvents.Open.TARGET,
                            BranchEvents.Open.TARGET_SEARCH_TAB)
                    .logEvent(requireContext());
        }
    }

    private void onVisible(boolean visible) {
        View view = getView();
        Activity activity = getActivity();
        if (view == null || activity == null) {
            Log.d(TAG, "onVisible:" + visible + ", view or activity is null!" +
                    " view:" + view +
                    " activity:" + activity);
            return;
        }
        EditText edit = view.findViewById(R.id.branchapp_edit_text);
        InputMethodManager input = (InputMethodManager) activity
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (visible) {
            edit.requestFocus();
            input.showSoftInput(edit, 0);
        } else {
            if (input.isActive(edit)) {
                View decorView = activity.getWindow().getDecorView();
                input.hideSoftInputFromWindow(decorView.getWindowToken(), 0);
            }
            // This must be after input.isActive(), or that one returns false.
            edit.clearFocus();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getSearchFragment().tearDownSearchController(mSearchController);
        // Hiding keyboard is needed - if we open Settings with keyboard open, it stays open.
        // It's not clear why this happens.
        onVisible(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mSearchController.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * If present, parse speech-to-text results and pass to the editText.
     * @param requestCode request
     * @param resultCode result
     * @param data data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && getView() != null) {
                String result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                EditText view = getView().findViewById(R.id.branchapp_edit_text);
                mQuerySource = BranchQuerySource.VOICE;
                view.append(result);
                mQuerySource = BranchQuerySource.KEYBOARD;
            }
        }
    }

    @SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions"})
    @Override
    public void onStop() {
        super.onStop();
        // Should we clear the query?
        if (requireActivity().isChangingConfigurations()) {
            // Rotation change. Do not clear.
        } else if (System.currentTimeMillis() - mLastClickTime < 2000) {
            // Leaving drawer because of a click. Do not clear.
            // 2000 might seem a lot but between the click and onStop() being invoked,
            // there can be a very large delay due to e.g. in/out animations.
        } else {
            // Leaving the app. Clear the query.
            EditText view = getView().findViewById(R.id.branchapp_edit_text);
            view.setText("");
        }
    }
}
