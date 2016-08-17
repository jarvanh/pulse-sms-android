/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.view.ColorPreviewButton;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ColorPickerAdapterTest extends MessengerRobolectricSuite {

    @Mock
    private View.OnClickListener onClickListener;

    @Mock
    private View view;

    @Mock
    private ViewGroup viewGroup;

    @Mock
    private ColorPreviewButton colorPreviewButton;

    @Mock
    private FrameLayout frameLayout;

    private ColorSet color1;
    private ColorSet color2;
    private ColorSet color3;

    @Before
    public void setUp() {
        color1 = ColorSet.RED(RuntimeEnvironment.application);
        color2 = ColorSet.PINK(RuntimeEnvironment.application);
        color3 = ColorSet.PURPLE(RuntimeEnvironment.application);
    }

    @Test
    public void test_noItems() {
        ColorPickerAdapter adapter = new ColorPickerAdapter(RuntimeEnvironment.application,
                new ArrayList<ColorSet>(), onClickListener);
        assertEquals(0, adapter.getCount());
    }

    @Test
    public void test_getItems() {
        ColorPickerAdapter adapter = initAdapter();
        assertEquals(3, adapter.getCount());
    }

    @Test
    public void test_getView_notDefaultTheme() {
        ColorPickerAdapter adapter = Mockito.spy(initAdapter());
        doReturn(colorPreviewButton).when(adapter).getColorPreviewButton();
        doReturn(frameLayout).when(adapter).getFrameLayout();

        adapter.getView(1, view, viewGroup);

        verify(colorPreviewButton).setInnerColor(color2.color);
        verify(colorPreviewButton).setOuterColor(color2.colorAccent);
        verify(frameLayout, times(1)).addView(any(View.class));
    }

    @Test
    public void test_getView_defaultTheme() {
        ColorPickerAdapter adapter = Mockito.spy(initAdapter());
        doReturn(colorPreviewButton).when(adapter).getColorPreviewButton();
        doReturn(frameLayout).when(adapter).getFrameLayout();

        adapter.getView(0, view, viewGroup);

        verify(colorPreviewButton).setInnerColor(color1.color);
        verify(colorPreviewButton).setOuterColor(color1.colorAccent);
        verify(frameLayout, times(1)).addView(any(View.class));
    }

    public ColorPickerAdapter initAdapter() {
        List<ColorSet> themes = new ArrayList<ColorSet>();
        themes.add(color1);
        themes.add(color2);
        themes.add(color3);
        return new ColorPickerAdapter(RuntimeEnvironment.application, themes, onClickListener);
    }

}