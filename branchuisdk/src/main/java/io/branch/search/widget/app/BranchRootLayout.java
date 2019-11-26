package io.branch.search.widget.app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Used in the app package as the root of UIs.
 * Different branches can use this layout for example to draw decorations.
 */
public class BranchRootLayout extends FrameLayout {

    public BranchRootLayout(Context context) {
        super(context);
    }

    public BranchRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BranchRootLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
