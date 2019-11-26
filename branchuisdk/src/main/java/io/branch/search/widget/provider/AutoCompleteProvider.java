package io.branch.search.widget.provider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.branch.referral.util.BranchEvent;
import io.branch.search.BranchQueryResult;
import io.branch.search.BranchSearch;
import io.branch.search.BranchSearchError;
import io.branch.search.BranchSearchRequest;
import io.branch.search.IBranchQueryResults;
import io.branch.search.widget.R;
import io.branch.search.widget.model.AutoComplete;
import io.branch.search.widget.model.SearchEngine;
import io.branch.search.widget.ui.DiscoveryViewHolder;
import io.branch.search.widget.ui.AutoCompleteViewHolder;
import io.branch.search.widget.ui.SearchEngineViewHolder;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.ui.decorations.SpacingDecoration;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 * <p>
 * NOTE: Do not rename without changing the array.
 */
@Keep
@SuppressWarnings("unused")
public class AutoCompleteProvider extends
        BaseBranchProvider<AutoComplete,
                DiscoveryViewModel<AutoComplete>> {
    private static final String TAG = "Branch::AutoComplete";
    private static final int MIN_QUERY = 2;

    private String mLastClickedQuery;
    private boolean mIsLastClickedQuery;

    private SearchEngineAdapter mSearchEngineAdapter;
    private RecyclerView mRecyclerView;

    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        mIsLastClickedQuery = query.equals(mLastClickedQuery);
        boolean result = super.isQueryValid(query, token, confirmed)
                && query.length() >= MIN_QUERY
                && !confirmed // Only show suggestions while typing
                && !mIsLastClickedQuery; // Don't show suggestions for the clicked suggestion
        if (mIsLastClickedQuery) {
            // Reset the lastClickedQuery once it's clicked and
            // we know we will return false here.
            mLastClickedQuery = null;
        }
        // Valid or not, pass the query to the view model. The default behavior is to only pass
        // when valid but this provider shows extra information (the search engines) even when
        // not valid (see onHideView). And the search engines adapter relies on the VM query.
        viewModel.setCurrentQuery(query, token);
        // Also ensure that search engine results are updated.
        mSearchEngineAdapter.notifyDataSetChanged();
        return result;
    }

    @Override
    protected void onHideView(@NonNull String query) {
        // If isLastClickedQuery, hide only the recycler - not the full view.
        // This way we will keep showing the search engines.
        if (mIsLastClickedQuery) {
            mRecyclerView.setVisibility(View.GONE);
        } else {
            super.onHideView(query);
        }
    }

    @Override
    protected void onShowView(@NonNull String query) {
        super.onShowView(query);
        // Ensure recycler is visible.
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onExactMatch(@NonNull String query, int token,
                             @NonNull Set<IDiscoveryProvider> providers) {
        super.onExactMatch(query, token, providers);
        if (!providers.contains(this)) {
            taskManager.abort(query, token);
            hideView(query, true);
        }
    }

    @NonNull
    @Override
    protected List<AutoComplete> loadResults(@NonNull final String query, int token, int capacity) {
        final TaskCompletionSource<List<AutoComplete>> source
                = new TaskCompletionSource<>();
        BranchSearchRequest request = createSearchRequest(query);
        BranchSearch.getInstance().autoSuggest(request, new IBranchQueryResults() {

            @Override
            public void onQueryResult(BranchQueryResult result) {
                Log.d(TAG, "Got server results: " + result.getQueryResults());
                List<AutoComplete> items = AutoComplete.parseResults(result);
                Iterator<AutoComplete> iterator = items.iterator();
                // Quick loop to avoid showing results that have the exact same of our query
                while (iterator.hasNext()) {
                    AutoComplete item = iterator.next();
                    if (item.getName().equalsIgnoreCase(query)) {
                        iterator.remove();
                    }
                }
                source.trySetResult(items);
            }

            @Override
            public void onError(BranchSearchError error) {
                Log.d(TAG, "Error with Branch AutoSuggest. " + error.getErrorMsg());
                source.trySetException(new RuntimeException(error.toString()));
            }
        });

        try {
            return Tasks.await(source.getTask());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void launchResult(@NonNull AutoComplete item, @Nullable Object payload, int position) {
        mLastClickedQuery = item.getName();
        callback.requestDiscovery(this, item.getName());

        position++; // 1 based
        new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                        getClass().getSimpleName())
                .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                        String.valueOf(position))
                .addCustomDataProperty(BranchEvents.ResultClick.EXTRA, item.getName())
                .logEvent(requireContext());
    }

    @Override
    protected void onCreateSection(@NonNull DiscoverySection.Builder<AutoComplete> builder) {
        builder.adapter = new Adapter();
        builder.layoutRes = R.layout.branch_section_autocomplete;
        builder.orientation = RecyclerView.HORIZONTAL;
    }

    @Override
    protected void onSectionCreated(@NonNull DiscoverySection<AutoComplete> section,
                                    @NonNull View view) {
        super.onSectionCreated(section, view);
        int spacing = getResources().getDimensionPixelSize(R.dimen.branch_autocomplete_spacing);
        section.setSpacing(spacing);
        RecyclerView engines = view.findViewById(R.id.engines);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(requireContext(),
                RecyclerView.HORIZONTAL, false);
        engines.addItemDecoration(new SpacingDecoration(spacing, 0));
        mSearchEngineAdapter = new SearchEngineAdapter();
        engines.setLayoutManager(manager);
        engines.setAdapter(mSearchEngineAdapter);
        mRecyclerView = view.findViewById(R.id.recycler);
    }

    private class Adapter extends DiscoveryAdapter<AutoComplete> {

        private static final int MAX_ITEMS = 5;

        private Adapter() {
            super(requireContext(), AutoCompleteProvider.this,
                    null /* R.string.branch_autocomplete_provider_title */);
        }

        @Override
        protected int getCapacity(int columns) {
            return MAX_ITEMS;
        }

        @NonNull
        @Override
        protected DiscoveryViewHolder<AutoComplete> onCreateItemViewHolder(
                @NonNull LayoutInflater inflater,
                @NonNull ViewGroup parent,
                int viewType) {
            return new AutoCompleteViewHolder(inflater, parent, this);
        }

        @Override
        protected void onBindItem(@NonNull DiscoveryViewHolder<AutoComplete> viewHolder,
                                  @NonNull AutoComplete item) {
            viewHolder.bind(item, viewModel.getCurrentQuery(), null);
        }
    }

    /** Google implementation of {@link SearchEngine} */
    private final static SearchEngine GOOGLE = new SearchEngine() {
        @Nullable
        @Override
        public Drawable getIcon(@NonNull Context context) {
            return ContextCompat.getDrawable(context, R.drawable.branch_google);
        }

        @NonNull
        @Override
        public Uri getUri(@NonNull String encodedQuery) {
            return Uri.parse("http://www.google.com/#q=" + encodedQuery);
        }
    };

    /** Google Maps implementation of {@link SearchEngine} */
    private final static SearchEngine MAPS = new SearchEngine() {
        @Nullable
        @Override
        public Drawable getIcon(@NonNull Context context) {
            return ContextCompat.getDrawable(context, R.drawable.branch_google_maps);
        }

        @NonNull
        @Override
        public Uri getUri(@NonNull String encodedQuery) {
            return Uri.parse("https://www.google.com/maps/search/?api=1&query=" + encodedQuery);
        }
    };

    /** Play Store implementation of {@link SearchEngine} */
    private final static SearchEngine PLAY_STORE = new SearchEngine() {
        @Nullable
        @Override
        public Drawable getIcon(@NonNull Context context) {
            return ContextCompat.getDrawable(context, R.drawable.branch_google_play_store);
        }

        @NonNull
        @Override
        public Uri getUri(@NonNull String encodedQuery) {
            return Uri.parse("http://play.google.com/store/search?q=" + encodedQuery + "&c=apps");
        }
    };

    /**
     * Adapter for a list of {@link SearchEngine}s.
     */
    private class SearchEngineAdapter extends RecyclerView.Adapter<SearchEngineViewHolder> {
        private final List<SearchEngine> mEngines = Arrays.asList(GOOGLE, MAPS /* , PLAY_STORE */);

        private final DiscoveryViewHolder.Callback<SearchEngine> mCallback
                = new DiscoveryViewHolder.Callback<SearchEngine>() {
            @Override
            public void onClick(@NonNull SearchEngine item,
                                @Nullable Object payload,
                                int position) {
                mEngines.get(position).launch(requireContext(), viewModel.getCurrentQuery());

                new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                        .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                                AutoCompleteProvider.this.getClass().getSimpleName())
                        .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                                "-1")
                        .addCustomDataProperty(BranchEvents.ResultClick.EXTRA,
                                item.getClass().getSimpleName())
                        .logEvent(requireContext());
            }
        };

        @Override
        public int getItemCount() {
            return mEngines.size();
        }

        @NonNull
        @Override
        public SearchEngineViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new SearchEngineViewHolder(LayoutInflater.from(viewGroup.getContext()),
                    viewGroup, mCallback);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchEngineViewHolder holder,
                                     int position) {
            String query = viewModel.getCurrentQuery();
            SearchEngine engine = mEngines.get(position);
            holder.bind(engine, query, null);
        }
    }

    @Nullable
    @Override
    protected Drawable getDividerTop() {
        return null;
    }
}
