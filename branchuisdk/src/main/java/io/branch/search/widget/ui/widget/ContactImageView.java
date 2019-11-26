package io.branch.search.widget.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;

/**
 * A View that can be used for contact images. Shows a proper shadow
 * and is rounded.
 */
public class ContactImageView extends SimpleDraweeView {

    public ContactImageView(Context context) {
        super(context);
        initialize();
    }

    public ContactImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ContactImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        getHierarchy().setRoundingParams(new RoundingParams().setRoundAsCircle(true));
        setScaleType(ScaleType.CENTER_CROP);
    }

    private void initialize() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        setOutlineProvider(new ViewOutlineProvider() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
    }
}
