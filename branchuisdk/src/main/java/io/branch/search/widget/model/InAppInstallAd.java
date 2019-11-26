package io.branch.search.widget.model;

import android.support.annotation.NonNull;

import io.branch.search.BranchAppResult;
import io.branch.search.BranchLinkResult;
import io.branch.search.widget.provider.InAppSearchProvider;

/**
 * One of the model objects for {@link InAppSearchProvider}.
 * A sponsored result that leads to install.
 */
public class InAppInstallAd {
    private final static String RANKING_HINT = "featured";

    public final BranchAppResult appResult;
    public final BranchLinkResult linkResult;

    public InAppInstallAd(@NonNull BranchAppResult appResult) {
        if (!isInstallAd(appResult)) {
            throw new IllegalArgumentException("Not an install ad!");
        }
        this.appResult = appResult;

        BranchLinkResult linkResult = null;
        for (BranchLinkResult candidaate : appResult.getDeepLinks()) {
            if (isInstallAd(candidaate)) {
                linkResult = candidaate;
                break;
            }
        }
        if (linkResult == null) {
            throw new IllegalArgumentException("Install ad has no featured link results!");
        } else {
            this.linkResult = linkResult;
        }
    }

    public static boolean isInstallAd(@NonNull BranchAppResult appResult) {
        return RANKING_HINT.equalsIgnoreCase(appResult.getRankingHint());
    }

    public static boolean isInstallAd(@NonNull BranchLinkResult linkResult) {
        return RANKING_HINT.equalsIgnoreCase(linkResult.getRankingHint());
    }
}
