package io.branch.search.widget.query;

/**
 * Represents the source of a query. Can be added to queries as metadata
 * using {@link BranchQueryMetadata}. Specifically, using {@link BranchQueryMetadata#SOURCE}.
 */
public enum BranchQuerySource {
    KEYBOARD, VOICE, AUTO_COMPLETE
}
