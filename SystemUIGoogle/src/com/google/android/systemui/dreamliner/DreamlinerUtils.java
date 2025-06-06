/*
 * Copyright (C) 2022 The PixelExperience Project
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

package com.google.android.systemui.dreamliner;

import android.content.Context;
import android.text.TextUtils;
import com.google.android.systemui.res.R;

public final class DreamlinerUtils {
    public static WirelessCharger getInstance(Context context) {
        if (context == null) {
            return null;
        }
        String string = context.getString(R.string.config_dockComponent);
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        try {
            return (WirelessCharger) context.getClassLoader().loadClass(string).newInstance();
        } catch (Throwable unused) {
            return null;
        }
    }
}
