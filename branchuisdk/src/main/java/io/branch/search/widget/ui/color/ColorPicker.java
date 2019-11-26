/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.branch.search.widget.ui.color;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import io.branch.search.widget.R;

/**
 * Picks a color from the input text, always returning the same
 * color for the same text.
 */
public interface ColorPicker {

    /**
     * Returns the color to use for the given string.
     * This method should return the same output for the same input.
     *
     * @param text The text.
     * @return The color value in the format {@code 0xAARRGGBB}.
     */
    @ColorInt
    int pickColor(@NonNull final String text);

}
