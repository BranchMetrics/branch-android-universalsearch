package io.branch.search.widget.provider;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Discovery providers. As a base feature, it provides and holds
 * a list of results for each provider.
 * Providers can subclass this view model to perform their own tasks.
 */
@SuppressWarnings("WeakerAccess")
public class DiscoveryViewModel<T> extends AndroidViewModel {
    private final MutableLiveData<List<T>> mItems = new MutableLiveData<>();
    private final MutableLiveData<String> mCurrentQuery = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCurrentToken = new MutableLiveData<>();
    private final MutableLiveData<String> mCompletedQuery = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCompletedToken = new MutableLiveData<>();

    /**
     * Creates a ViewModel.
     */
    public DiscoveryViewModel(@NonNull Application application) {
        super(application);
        clear();
    }

    /**
     * Returns an observable list of items.
     * @return the items
     */
    @NonNull
    public LiveData<List<T>> getItems() {
        return mItems;
    }

    /**
     * Sets a list of results for the given query.
     *  @param query the query that started this search
     * @param token the query token
     * @param list the list of results
     */
    public void setItems(@NonNull String query, int token, @NonNull List<T> list) {
        // query first, then items! so we can get the correct query when observing items
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            mCompletedQuery.setValue(query);
            mCompletedToken.setValue(token);
            mItems.setValue(list);
        } else {
            mCompletedQuery.postValue(query);
            mCompletedToken.postValue(token);
            mItems.postValue(list);
        }
    }

    /**
     * Clears the list of current items and the
     * queries information.
     */
    public void clear() {
        setItems("", -1, new ArrayList<T>());
        mCompletedQuery.setValue("");
    }

    /**
     * Notifies that this query will be executed.
     * @param query the query
     * @param token the query token
     */
    public void setCurrentQuery(@NonNull String query, int token) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            mCurrentQuery.setValue(query);
            mCurrentToken.setValue(token);
        } else {
            mCurrentQuery.postValue(query);
            mCurrentToken.postValue(token);
        }
    }

    /**
     * Returns the last query that was executed, though
     * it might not have finished providing the results.
     *
     * @return the current query, even if executing
     */
    @NonNull
    public String getCurrentQuery() {
        String query = mCurrentQuery.getValue();
        return query != null ? query : "";
    }

    /**
     * Returns the last query that has provided results,
     * which is related to the results in {@link #getItems()}.
     *
     * @return the last completed query
     */
    @NonNull
    public String getCompletedQuery() {
        String query = mCompletedQuery.getValue();
        return query != null ? query : "";
    }

    /**
     * Returns the token for {@link #getCurrentQuery()},
     * or -1 if none was set.
     * @return the query token
     */
    public int getCurrentToken() {
        Integer token = mCurrentToken.getValue();
        return token != null ? token : -1;
    }

    /**
     * Returns the token for {@link #getCompletedQuery()},
     * or -1 if none was set.
     * @return the query token
     */
    public int getCompletedToken() {
        Integer token = mCompletedToken.getValue();
        return token != null ? token : -1;
    }
}
