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

package xyz.klinker.messenger;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import xyz.klinker.messenger.data.DataSource;

@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 21, constants = BuildConfig.class)
public abstract class MessengerRobolectricSuite {

    @Before
    public final void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public final void teardown() {
        DataSource.forceCloseImmediate();
    }

    /**
     * Helper for starting a fragment inside a FragmentActivity.
     *
     * @param fragment the fragment to start.
     */
    public static <T extends Fragment> T startFragment(T fragment) {
        return startFragment(fragment, FragmentActivity.class);
    }

    public static <T extends Fragment> T startFragment(T fragment, Class<? extends FragmentActivity> activityClass) {
        FragmentActivity activity = Robolectric.buildActivity(activityClass)
                .create()
                .start()
                .get();

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(fragment, null);
        fragmentTransaction.commit();

        return fragment;
    }

}

