package com.cutsame.ui.gallery

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ss.android.ugc.cut_log.LogUtil
private const val TAG = "cutui.PermissionActivity"
abstract class PermissionActivity : AppCompatActivity() {

    val PERMISSION_REQUEST_CODE = 1000

    open fun onPermissionGranted() {
    }

    open fun onPermissionDenied() {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.d(TAG, "onRequestPermissionsResult requestCode $requestCode")
        if (PERMISSION_REQUEST_CODE == requestCode) {
            grantResults.filter {
                it != PackageManager.PERMISSION_GRANTED
            }.apply {
                LogUtil.d(TAG, "PERMISSION_REQUEST_CODE ${isEmpty()}")
                if (isEmpty()) {
                    runOnUiThread { onPermissionGranted() }
                } else {
                    runOnUiThread { onPermissionDenied() }
                }
            }
        }
    }

    fun checkPermission(permissions: Array<String>) {
        LogUtil.e(TAG, "checkPermission")
        permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.apply {
            if (isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this@PermissionActivity,
                    this.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                runOnUiThread { onPermissionGranted() }
            }
        }
    }
}