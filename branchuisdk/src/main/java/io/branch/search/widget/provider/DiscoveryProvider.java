package io.branch.search.widget.provider;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.branch.search.widget.R;
import io.branch.search.widget.task.AsyncTask;
import io.branch.search.widget.task.DiscoveryTaskManager;
import io.branch.search.widget.ui.decorations.SpacingDecoration;

/**
 * Base class for providers.
 * Most implementations should subclass {@link SimpleDiscoveryProvider} instead of this class.
 * Subclassing the simple provider will be much simpler for providers that have a single section.
 *
 * Subclasses of this class are supposed to:
 * - load results by implementing {@link #loadResults(String, int, int)}
 * - provide the UI for results by implementing {@link #onCreateSection(DiscoverySection.Builder)}
 * - open a clicked item by implementing {@link #launchResult(Object, Object, int)}
 *
 * Optionally, subclasses can:
 * - define a dedicated view model in {@link #getViewModelClass()}
 * - define which query they support in {@link #isQueryValid(String, int, boolean)}
 * - do something on invalid query {@link #onInvalidQuery(String, int, boolean)}
 * - do something on valid query {@link #onValidQuery(String, int, boolean)}
 *
 * See {@link DiscoverySection} about what can be done for splitting results into different
 * sections and relevant APIs.
 *
 * Extra functions:
 * - {@link #showsLoadingIndicator()} to tell if the class shows loading indicators or not
 * - {@link #notifyExactMatch(String, int)} to notify that, among results being loaded, there is one
 *   that can be considered an exact match
 *
 */
public abstract class DiscoveryProvider<T, VM extends DiscoveryViewModel<T>>
        extends Fragment
        implements IDiscoveryProvider, DiscoveryAdapter.Callback<T> {
    private static final String TAG = "Branch::Discovery";

    private boolean mIsActive = true;
    private final List<DiscoverySection<T>> mSections = new ArrayList<>();
    private final List<Integer> mSectionTypes = new ArrayList<>();
    private RecyclerView.Adapter mSectionAdapter;

    // protected members
    protected DiscoveryTaskManager<T> taskManager;
    protected VM viewModel;
    protected IDiscoveryProviderCallback callback;

    @Override
    public boolean initialize(@NonNull Context context,
                              @NonNull IDiscoveryProviderCallback callback,
                              @Nullable Object payload) {
        this.callback = callback;
        this.taskManager = new DiscoveryTaskManager<>(this, callback);
        return true;
    }

    /**
     * Returns the ViewModel class, extending {@link DiscoveryViewModel}
     * or ViewModel itself.
     * @return the viewModel class
     */
    @NonNull
    protected Class<VM> getViewModelClass() {
        //noinspection unchecked,RedundantCast
        return (Class<VM>) (Class) DiscoveryViewModel.class;
    }

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(getViewModelClass());
    }

    /**
     * Returns the {@link DiscoverySection}s that this provider will be showing. If present,
     * this can be based on the latest results that this provider has loaded.
     * The returned list should contain the section types in the order they should appear.
     *
     * The meaning of section types is that of RecyclerView.Adapter's viewType.
     * Sections that share the same type might be reused to avoid recreation.
     *
     * @param latestResults latest results
     * @return section types
     */
    @NonNull
    protected List<Integer> computeSections(@Nullable List<T> latestResults) {
        return Collections.singletonList(0);
    }

    /**
     * Creates the section for a certain section type.
     * This means setting the builder fields.
     * It is mandatory at this point to set the builder adapter.
     */
    protected abstract void onCreateSection(@NonNull DiscoverySection.Builder<T> builder);

    /**
     * Can be called by subclasses to invalidate the current sections.
     * This will trigger a {@link #computeSections(List)} call and, if requested,
     * the results will be applied again.
     *
     * The results will be split between the created sections, as explained in the
     * {@link DiscoverySection} documentation.
     */
    @SuppressLint("UseSparseArrays")
    protected final void invalidateSections(boolean applyResults) {
        List<T> latestResults = viewModel.getItems().getValue();
        mSectionTypes.clear();
        mSectionTypes.addAll(computeSections(latestResults));

        List<DiscoverySection<T>> oldSections = new ArrayList<>(mSections);
        List<DiscoverySection<T>> newSections = new ArrayList<>();
        for (int i = 0; i < mSectionTypes.size(); i++) {
            int type = mSectionTypes.get(i);
            // Can we recycle an old section for this type ?
            DiscoverySection<T> cached = null;
            for (int j = 0; j < oldSections.size(); j++) {
                DiscoverySection<T> section = oldSections.get(j);
                if (section.getType() == type) {
                    cached = section;
                }
            }
            // If we can, use the cached section. If not, create a new one.
            // Caching greatly improves performance (avoids inflating) and also avoids
            // useless animations that would happen if we set same old content to new views.
            if (cached != null) {
                oldSections.remove(cached);
                newSections.add(cached);
            } else {
                DiscoverySection.Builder<T> builder = new DiscoverySection.Builder<>(type);
                onCreateSection(builder);
                DiscoverySection<T> section = builder.build();
                onSectionCreated(section, section.getView());
                newSections.add(section);
            }
        }
        mSections.clear();
        mSections.addAll(newSections);
        mSectionAdapter.notifyDataSetChanged();

        if (applyResults && latestResults != null) {
            applyResults(viewModel.getCompletedQuery(), latestResults);
        }
    }

    protected int getCapacity() {
        int capacity = 0;
        for (DiscoverySection<T> section : mSections) {
            int count = section.getCapacity();
            if (count == Integer.MAX_VALUE) {
                return count;
            }
            capacity += count;
        }
        return capacity;
    }

    /**
     * Called after a {@link DiscoverySection} has been created and its view is now available
     * to be inspected, though it is not added to a window yet.
     * @param section new section
     * @param view its view
     */
    protected void onSectionCreated(@NonNull DiscoverySection<T> section, @NonNull View view) {
        // No-op
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.branch_provider, container, false);
    }

    @CallSuper
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setActive(mIsActive); // Enforce the current isActive state, in case it was called before
        viewModel.getItems().observe(getViewLifecycleOwner(), new Observer<List<T>>() {
            @SuppressWarnings("StatementWithEmptyBody")
            @Override
            public void onChanged(@Nullable List<T> data) {
                if (data == null) data = new ArrayList<>();
                String query = viewModel.getCompletedQuery();
                String currQuery = viewModel.getCurrentQuery();
                if (data.isEmpty()) {
                    // Apply this anyway. This was a clear request and we want to obey.
                    applyResults(query, data);
                } else if (currQuery.equals(query)) {
                    // Query has not changed: apply these results.
                    applyResults(query, data);
                } else {
                    // Do nothing. A newer query was set recently, so do not
                    // bother updating the UI with now-old results.
                }
            }
        });
        RecyclerView recycler = view.findViewById(R.id.recycler);
        recycler.setNestedScrollingEnabled(false);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(requireContext(),
                RecyclerView.VERTICAL, false);
        recycler.setLayoutManager(manager);
        mSectionAdapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                              int viewType) {
                FrameLayout layout = new FrameLayout(viewGroup.getContext());
                return new RecyclerView.ViewHolder(layout) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int position) {
                FrameLayout frame = (FrameLayout) viewHolder.itemView;
                View section = mSections.get(position).getView();
                if (frame.getChildCount() == 0 || frame.getChildAt(0) != section) {
                    frame.removeAllViews();
                    if (section.getParent() != null) {
                        ((ViewGroup) section.getParent()).removeView(section);
                    }
                    // Respect the section width/height being either wrap or match parent.
                    frame.setLayoutParams(section.getLayoutParams());
                    frame.addView(section);
                }
            }

            @Override
            public int getItemCount() {
                return mSections.size();
            }
        };
        recycler.setAdapter(mSectionAdapter);

        // Set up dividers
        View dividerTopView = view.findViewById(R.id.divider_top);
        View dividerBottomView = view.findViewById(R.id.divider_bottom);
        if (dividerTopView != null) {
            Drawable divider = getDividerTop();
            dividerTopView.setBackground(divider);
            dividerTopView.setVisibility(divider == null ? View.GONE : View.VISIBLE);
        }
        if (dividerBottomView != null) {
            Drawable divider = getDividerBottom();
            dividerBottomView.setBackground(divider);
            dividerBottomView.setVisibility(divider == null ? View.GONE : View.VISIBLE);
        }
        Drawable sectionDivider = getSectionDivider();
        if (sectionDivider != null) {
            recycler.addItemDecoration(new SpacingDecoration(sectionDivider.getIntrinsicHeight(),
                    0, sectionDivider));
        }

        // Add the 0-state sections. In most cases this will add the first and only
        // section and this value will never change.
        invalidateSections(false);
    }

    /**
     * Called on a background thread for this provider to load its results.
     * Implementors should fetch results for the given query and throw an exception
     * if something went wrong.
     *
     * @param query the query
     * @param token the query token
     * @return the results
     */
    @NonNull
    @WorkerThread
    protected abstract List<T> loadResults(@NonNull String query, int token, int capacity);

    /**
     * New results were just retrieved or restored. The default implementation will pass
     * this data to the provider's sections.
     *
     * @param query the query that created these results
     * @param results results, possibly empty
     */
    @CallSuper
    protected void applyResults(@NonNull String query, @NonNull List<T> results) {
        List<T> copy = new ArrayList<>(results);
        for (DiscoverySection<T> section : mSections) {
            copy = section.apply(copy);
        }
    }

    /**
     * A {@link IDiscoveryProvider} method implementation.
     * @return the required permissions
     */
    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return new String[0];
    }

    @Override
    public void onPermissionResults(boolean[] granted) {
        if (getRequiredPermissions().length > 0) {
            callback.requestDiscovery(this, null);
        }
    }

    /**
     * A {@link IDiscoveryProvider} method implementation.
     * @return this
     */
    @NonNull
    @Override
    public final Fragment getFragment() {
        return this;
    }

    /**
     * A {@link IDiscoveryProvider} method implementation.
     */
    @Override
    public void setActive(boolean active) {
        mIsActive = active;
        View view = getView();
        if (view != null) {
            view.setVisibility(active ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * A {@link IDiscoveryProvider} method implementation.
     */
    @Override
    public boolean isActive() {
        return mIsActive;
    }

    /**
     * Determines if permissions have been granted already.
     * @return true if all permissions required have been granted.
     */
    protected final boolean hasPermissions() {
        Context context = getContext();
        if (context == null) return false;
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission Denied: " + permission);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether this query is valid and actionable.
     * The simple implementation will simply return false for empty string.
     *
     * @param query the query
     * @param token the query token
     * @param confirmed whether this search is confirmed (by clicking search) as opposed to typing
     * @return whether it is valid or not
     */
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return hasPermissions() && !TextUtils.isEmpty(query);
    }

    /**
     * Base implementation of startDiscovery which will simply check for validity of the query
     * by calling {@link #isQueryValid(String, int, boolean)} and then dispatch to either
     * {@link #onValidQuery(String, int, boolean)} or
     * {@link #onInvalidQuery(String, int, boolean)}.
     */
    @Override
    public final void startDiscovery(@NonNull String query, int token, boolean confirmed) {
        if (isQueryValid(query, token, confirmed)) {
            onValidQuery(query, token, confirmed);
        } else {
            onInvalidQuery(query, token, confirmed);
        }
    }

    /**
     * Called when a valid query is being requested. The default behavior is to
     * dispatch a background thread query request to {@link #loadResults(String, int, int)},
     * then post results to the view model.
     *
     * Once results are posted successfully, subclasses will get a
     * {@link #applyResults(String, List)} call.
     */
    @CallSuper
    protected void onValidQuery(@NonNull final String query, final int token, boolean confirmed) {
        viewModel.setCurrentQuery(query, token);
        taskManager.execute(query, token, new AsyncTask.Action<String, List<T>>() {
            @NonNull
            @Override
            public List<T> execute(@NonNull String s) {
                return loadResults(s, token, getCapacity());
            }
        }).addOnSuccessListener(new OnSuccessListener<List<T>>() {
            @Override
            public void onSuccess(List<T> list) {
                viewModel.setItems(query, token, list);
                if (list.size() > 0) {
                    showView(query, true);
                } else {
                    hideView(query, true);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                viewModel.setItems(query, token, new ArrayList<T>());
                hideView(query, true);
            }
        });
        //noinspection StatementWithEmptyBody
        if (showsLoadingIndicator()) {
            // read comments in showsLoadingIndicator
            showView(query, true);
        } else {
            // We might be showing - if we had previous results -
            // or not showing - if previous query had no results.
            // In both cases, we are OK with current state. If we
            // have results, even if belonging to an old query, we prefer
            // to show them, as the user can see the results changing while
            // he types, instead of seeing providers disappear and appear
            // at any character.
        }
    }

    /**
     * Called when an invalid query was requested. The default behavior is to clear
     * current data and dispatch a {@link java.util.concurrent.CancellationException}
     * to our callback (through the task manager).
     */
    @CallSuper
    protected void onInvalidQuery(@NonNull String query, int token, boolean confirmed) {
        viewModel.clear();
        taskManager.abort(query, token);
        // On invalid query, it is not passed to viewModel.setCurrentQuery().
        // So checking if it's still the current query would fail.
        hideView(query, false);
    }

    /**
     * Shows this provider. This checks that the query is still the current query
     * and dispatches to {@link #onShowView(String)}.
     *
     * @param query the query
     * @param checkIsCurrent whether to check or not
     */
    @SuppressWarnings("SameParameterValue")
    protected final void showView(@NonNull String query, boolean checkIsCurrent) {
        if (mIsActive && (!checkIsCurrent || query.equals(viewModel.getCurrentQuery()))) {
            onShowView(query);
        }
    }

    /**
     * Called to show this provider, typically after a
     * {@link #onValidQuery(String, int, boolean)} call.
     *
     * @param query the query, if available
     */
    protected void onShowView(@NonNull final String query) {
        View view = getView();
        if (view != null && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides this provider. This checks that the query is still the current query
     * and dispatches to {@link #onHideView(String)}.
     *
     * @param query the query
     * @param checkIsCurrent whether to check or not
     */
    protected final void hideView(@NonNull final String query, boolean checkIsCurrent) {
        if (mIsActive && (!checkIsCurrent || query.equals(viewModel.getCurrentQuery()))) {
            onHideView(query);
        }
    }

    /**
     * Called to hide this provider, typically after a
     * {@link #onInvalidQuery(String, int, boolean)} call.
     *
     * @param query the query, if available
     */
    protected void onHideView(@NonNull final String query) {
        View view = getView();
        if (view != null && view.getVisibility() != View.GONE) {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Whether this provider shows a loading indicator.
     * This means that we will show the provider's View when a query is in progress,
     * even if the provider is currently blank.
     * If the provider is not blank - has results from previous queries - it will always
     * be shown during new queries, as we prefer to show the old results rather than nothing.
     *
     * @return true if this provider shows a loading indicator
     */
    protected boolean showsLoadingIndicator() {
        return false;
    }

    /**
     * Subclasses can call this method to notify that an exact match was found for
     * the given query. This can be later passed as a hint to other providers in the group
     * so they can modify their behavior (for example, hide themselves).
     *
     * Currently this can be called from any thread and at any moment.
     *
     * @param query the query
     * @param token the query token
     */
    protected final void notifyExactMatch(@NonNull String query, int token) {
        callback.onExactMatch(this, query, token);
    }

    /**
     * Includes a list of providers that have notified they found an exact match for the
     * given query. This can be used, for example, for some providers to hide themselves
     * when some others have a match.
     *
     * Subclasses should probably check that:
     * - the query is still equal to viewModel.getCurrentQuery()
     * - that !providers.contains(this)
     * @param query the query
     * @param token the query token
     * @param providers non-null, non-empty set of providers
     */
    @UiThread
    @Override
    public void onExactMatch(@NonNull String query, int token,
                             @NonNull Set<IDiscoveryProvider> providers) {

    }

    @Override
    public final void onItemClick(@NonNull T item, @Nullable Object payload, int position) {
        callback.onResultClick(this,
                viewModel.getCompletedQuery(),
                viewModel.getCompletedToken(),
                item);
        launchResult(item, payload, position);
    }

    /**
     * Should be implemented to open the results that has just been clicked.
     * @param item clicked result
     * @param payload payload
     * @param position position
     */
    protected abstract void launchResult(@NonNull T item, @Nullable Object payload, int position);

    @Nullable
    protected Drawable getSectionDivider() {
        return ContextCompat.getDrawable(requireContext(), R.drawable.branch_section_divider);
    }

    @Nullable
    protected Drawable getDividerTop() {
        return ContextCompat.getDrawable(requireContext(), R.drawable.branch_provider_divider);
    }

    @Nullable
    protected Drawable getDividerBottom() {
        return null;
    }
}
