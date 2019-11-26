package io.branch.search.widget.util;

/**
 * Contains event keys and names for Branch custom event tracking.
 */
public class BranchEvents {

    /**
     * Event representing changes in preferences.
     * See {@link Settings}.
     */
    public final static String TYPE_SETTINGS = "settings";

    /**
     * Event representing an open action of some UI
     * component. See {@link Open}.
     */
    public final static String TYPE_OPEN = "open";

    /**
     * Event representing the click action on one of
     * the search results. See {@link ResultClick}.
     */
    public final static String TYPE_RESULT_CLICK = "result_click";

    /**
     * Event representing a search action. This means that the
     * query was typed and is about to be passed to providers to get results.
     * See {@link SearchExecuted}.
     */
    public final static String TYPE_SEARCH_EXECUTED = "search_executed";

    /**
     * Event representing the return of search results. This follows
     * {@link #TYPE_SEARCH_EXECUTED} after all of the providers have returned.
     * This means that this might not be fired for all queries, for example
     * it might skip transient queries that are fired as you type.
     * See {@link SearchResults}.
     */
    public final static String TYPE_SEARCH_RESULTS = "search_results";

    /**
     * Constants for {@link #TYPE_SETTINGS}.
     */
    public static class Settings {
        /**
         * Key: the name of the preference being changed.
         */
        public final static String PREFERENCE_NAME = "preference_name";

        /**
         * Key: the new value of the preference being changed.
         */
        public final static String PREFERENCE_NEW_VALUE = "preference_new_value";
    }

    /**
     * Constants for {@link #TYPE_OPEN}.
     */
    public static class Open {

        /**
         * Key: the open action target.
         */
        public final static String TARGET = "target";

        /**
         * Value for {@link #TARGET}: the search tab was opened.
         */
        public final static String TARGET_SEARCH_TAB = "search_tab";

        /**
         * Value for {@link #TARGET}: the app list was opened.
         */
        public final static String TARGET_APP_LIST = "app_list";

        /**
         * Value for {@link #TARGET}: the settings screen was opened.
         */
        public final static String TARGET_SEARCH_SETTINGS = "app_list";
    }

    /**
     * Constants for {@link #TYPE_RESULT_CLICK}.
     */
    public static class ResultClick {

        /**
         * Key: the result provider.
         */
        public final static String PROVIDER = "provider";

        /**
         * Key: the 1-based position of the clicked result
         * in the provider array.
         */
        public final static String POSITION = "position";

        /**
         * Key: extra information about the clicked result.
         * The contents of this key are provider-dependent.
         */
        public final static String EXTRA = "extra";

        /**
         * Value for {@link #EXTRA}: a contact was clicked.
         */
        public final static String CONTACT_OPEN = "contact_open";

        /**
         * Value for {@link #EXTRA}: a contact call button was clicked.
         */
        public final static String CONTACT_CALL = "contact_call";

        /**
         * Value for {@link #EXTRA}: a contact SMS button was clicked.
         */
        public final static String CONTACT_MESSAGE = "contact_message";
    }

    /**
     * Constants for {@link #TYPE_SEARCH_EXECUTED}.
     */
    public static class SearchExecuted {

        /**
         * Key: the query.
         */
        public final static String QUERY = "query";

        /**
         * Key: the input source.
         */
        public final static String SOURCE = "source";


        /**
         * Value for {@link #SOURCE}: text coming from keyboard.
         */
        public final static String SOURCE_KEYBOARD = "keyboard";

        /**
         * Value for {@link #SOURCE}: text coming from voice input.
         */
        public final static String SOURCE_VOICE = "voice";

        /**
         * Value for {@link #SOURCE}: text coming from autosuggest click.
         */
        public final static String SOURCE_AUTOSUGGEST = "autosuggest";
    }

    /**
     * Constants for {@link #TYPE_SEARCH_RESULTS}.
     */
    public static class SearchResults {

        /**
         * Key: the query.
         */
        public final static String QUERY = "query";

        /**
         * Key: a comma-separated list of providers that returned results.
         *
         * Example value: "MediaProvider,ShortcutProider,AppsProvider".
         */
        public final static String PROVIDERS = "providers";

        /**
         * Key: a comma-separated list of the number of results from {@link #PROVIDERS}.
         * If the provider encountered an error, we'll send a -1.
         *
         * Example value: "5,0,3".
         */
        public final static String RESULTS = "results";
    }
}
