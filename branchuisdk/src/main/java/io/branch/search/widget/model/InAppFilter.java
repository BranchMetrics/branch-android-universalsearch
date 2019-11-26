package io.branch.search.widget.model;

import android.support.annotation.Nullable;


import io.branch.search.BranchAppResult;

/**
 * Represents a filter or, if null, the absence of a filter.
 */
public class InAppFilter {
    public final BranchAppResult filter;

    public InAppFilter(@Nullable BranchAppResult filter) {
        this.filter = filter;
    }
}
