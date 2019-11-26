package io.branch.search.widget.ui.color;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;

import io.branch.search.widget.R;
import io.branch.search.widget.ui.color.ColorPicker;

/**
 * A {@link ColorPicker} that picks color from a material design palette,
 * caching them in a static field.
 */
public class PaletteColorPicker implements ColorPicker {

    private static int[] sColors;

    public PaletteColorPicker(@NonNull Resources res) {
        if (sColors == null) {
            TypedArray array = res.obtainTypedArray(R.array.branch_material_400);
            int fallback = res.getColor(R.color.branch_contact_actions_background);
            int count = array.length();
            sColors = new int[count];
            for (int i = 0; i < count; i++) {
                sColors[i] = array.getColor(i, fallback);
            }
            array.recycle();
        }
    }

    @Override
    public int pickColor(@NonNull final String text) {
        final int index = Math.abs(text.hashCode()) % sColors.length;
        return sColors[index];
    }
}
