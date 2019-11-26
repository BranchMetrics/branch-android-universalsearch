package io.branch.search.widget.util;

import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.TextView;

/**
 * Helps in identifying matches and applying text Spans to them.
 *
 * This implements a common match behavior, currently used for App search and Contacts search,
 * where we just want to return a match if the query is at the beginning of the word.
 *
 * Given "John Mark Smith":
 * - "joh" is a match
 * - "ohn" is not
 * - "smit" is a match
 * - "john ma" is a match
 * - "mark sm" is a match
 * - "john mark sm" is a match
 */
public class WordMatcher {

    /**
     * Interface used for splitting the input string in 'words'.
     * Typically we'd split by whitespaces but this functionality can be extended.
     */
    public interface Splitter {
        boolean splitsBy(int character);
        int splitOffset(int character);
    }

    /**
     * A {@link Splitter} that splits by whitespaces.
     * For example, "John Smith" becomes ["John", "Smith"].
     */
    public static class SpaceSplitter implements Splitter {
        @Override
        public boolean splitsBy(int character) {
            return Character.isWhitespace(character);
        }

        @Override
        public int splitOffset(int character) {
            return 1;
        }
    }

    /**
     * A {@link Splitter} that splits by the given characters.
     * For example, if character is 'S', "MySpace" becomes ["My", "Space"].
     */
    public static class CharSplitter implements Splitter {
        private int mChar;

        public CharSplitter(int character) {
            this.mChar = character;
        }

        @Override
        public boolean splitsBy(int character) {
            return character == mChar;
        }

        @Override
        public int splitOffset(int character) {
            return 0;
        }
    }

    /**
     * A {@link Splitter} that splits by upper or lower case characters.
     * For example, for upper case, "WhatsApp" becomes ["Whats", "App"].
     */
    public static class CaseSplitter implements Splitter {
        private boolean mUpper;

        public CaseSplitter(boolean upper) {
            this.mUpper = upper;
        }

        @Override
        public boolean splitsBy(int character) {
            return mUpper ? Character.isUpperCase(character) : Character.isLowerCase(character);
        }

        @Override
        public int splitOffset(int character) {
            return 0;
        }
    }

    /**
     * A {@link Splitter} that splits by digits.
     * For example, "Foo2Bar" becomes ["Foo", "2Bar"].
     */
    public static class NumberSplitter implements Splitter {

        @Override
        public boolean splitsBy(int character) {
            return Character.isDigit(character);
        }

        @Override
        public int splitOffset(int character) {
            return 0;
        }
    }

    /**
     * A {@link Splitter} that splits by anything that's not
     * a lowercase character.
     */
    public static class NonLowerCaseSplitter implements Splitter {

        @Override
        public boolean splitsBy(int character) {
            return !Character.isLowerCase(character);
        }

        @Override
        public int splitOffset(int character) {
            return Character.isLetterOrDigit(character) ? 0 : 1;
        }
    }

    private Splitter[] mSplitters;

    public WordMatcher(@NonNull Splitter... splitters) {
        mSplitters = splitters;
    }

    public WordMatcher() {
        this(new SpaceSplitter());
    }

    /**
     * Returns true if a match is found.
     */
    public boolean matches(@NonNull String text, @NonNull String query) {
        if (text.isEmpty() || query.isEmpty()) return false;
        if (!containsIgnoreCase(text, query)) return false;

        for (Splitter splitter : mSplitters) {
            if (find(text, query, splitter) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies the given decoration to [text] based on [query] matching,
     * then sets this result to [view].
     */
    public void decorate(@NonNull String text,
                         @NonNull String query,
                         @NonNull TextView view,
                         @NonNull Object decoration) {
        if (text.isEmpty() || query.isEmpty()) return;
        if (!containsIgnoreCase(text, query)) return;

        for (Splitter splitter : mSplitters) {
            int index = find(text, query, splitter);
            if (index >= 0) {
                Spannable spannable = new SpannableString(text);
                spannable.setSpan(decoration, index, index + query.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                view.setText(spannable);
                return;
            }
        }
    }

    /**
     * Returns the index of the first character in text that matches, or -1,
     * based on the given splitter.
     * @param text full text
     * @param query query
     * @param splitter splitter
     * @return index or -1
     */
    private int find(@NonNull String text, @NonNull String query,
                            @NonNull Splitter splitter) {
        // Split text not in words, but in substrings where we sequentially remove the first word.
        // So John Mark Smith becomes "John Mark Smith", "Mark Smith" and "Smith".
        // Then check if this startsWith the given query. We also want to do this without extra
        // String allocations since this function is called very often.
        int index = -1;
        int indexWithOffset;
        while (true) {
            indexWithOffset = index < 0 ? 0
                    : index + splitter.splitOffset(text.charAt(index));
            if (startsWithIgnoreCase(text, indexWithOffset, query)) {
                return indexWithOffset;
            }
            // This index didn't work. Advance and check.
            index++;
            index = indexOf(text, index, splitter);
            if (index < 0) {
                return -1;
            }
        }
    }

    private int indexOf(@NonNull String text, int from, @NonNull Splitter splitter) {
        if (from >= text.length()) return -1;
        for (int i = from; i < text.length(); i++) {
            if (splitter.splitsBy(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    // Something like Kotlin startsWith(s, ignoreCase = true), we want to avoid String allocations
    private static boolean startsWithIgnoreCase(@NonNull String string,
                                                int startIndex,
                                                @NonNull String prefix) {
        return string.regionMatches(true, startIndex, prefix, 0, prefix.length());
    }

    // Something like Kotlin contains(s, ignoreCase = true), we want to avoid String allocations
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean containsIgnoreCase(@NonNull String string, @NonNull String match) {
        if (match.isEmpty()) return true;
        int length = match.length();
        for (int i = string.length() - length; i >= 0; i--) {
            if (string.regionMatches(true, i, match, 0, length))
                return true;
        }
        return false;
    }
}
