/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.wb.cpedemo.dash;

import android.annotation.TargetApi;
import android.text.TextUtils;

import com.google.android.exoplayer.drm.ExoMediaDrm;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.util.Util;

//import net.flixster.android.model.flixmodel.StreamAsset;

import java.util.UUID;

/**
 * A {@link MediaDrmCallback} for Widevine test content.
 */
@TargetApi(18)
public class WidevineMediaDrmCallback implements MediaDrmCallback {

  private String WIDEVINE_GTS_DEFAULT_BASE_URI =
      "https://proxy.uat.widevine.com/proxy";

  private final String defaultUri;
  private CPEDemoMovieFragment_Dash.DashContent dashContent;

  /*
  public WidevineMediaDrmCallback(String contentId, String provider) {
    String params = "?video_id=" + contentId + "&provider=" + provider;
    defaultUri = WIDEVINE_GTS_DEFAULT_BASE_URI + params;
  }*/



  public WidevineMediaDrmCallback(CPEDemoMovieFragment_Dash.DashContent dashContent) {

    this.dashContent = dashContent;
    WIDEVINE_GTS_DEFAULT_BASE_URI = dashContent.drmProxyURL;
    String params = "?video_id=" + dashContent.dashContentID + "&provider=" + dashContent.dashProvider;
    defaultUri = WIDEVINE_GTS_DEFAULT_BASE_URI + params;
  }

  @Override
  public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
    String url = request.getDefaultUrl() + "&signedRequest=" + new String(request.getData());
    return Util.executePost(url, null, null);
  }

  @Override
  public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
    String url = request.getDefaultUrl();
    if (TextUtils.isEmpty(url)) {
      url = defaultUri;
    }
    return Util.executePost(url, request.getData(), null);
  }

}
