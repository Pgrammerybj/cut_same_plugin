package com.ola.editor.kit.download.callback;

import okhttp3.ResponseBody;


public interface DownloadListener {
    void onStart(ResponseBody responseBody);
}
