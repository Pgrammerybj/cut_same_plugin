package com.ola.editor.kit.download.callback;

import java.io.File;

import io.reactivex.disposables.Disposable;

public interface DownloadCallback {
    void onStart(Disposable d);

    void onProgress(long totalByte, long currentByte, int progress);

    void onFinish(File file);

    void onError(String msg);
}
