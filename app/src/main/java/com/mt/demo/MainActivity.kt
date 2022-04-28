package com.mt.demo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mt.ihttp.utils.download.DownloadBean
import com.mt.ihttp.utils.download.IDownloader
import com.mt.ihttp.utils.download.OnDownloadListener
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
        }


        var list = arrayListOf<DownloadBean>()
        list.add(
            DownloadBean(
                "https://cos.mylu.net/bg/12416840825c886f2a8a772_240.svga",
                "04e0065877cbb02f72db135281cf22aa",
                null
            )
        )

        IDownloader(this).setFilePath("" + filesDir + File.separator).setIsSync(true)
            .startDownTask(list, object : OnDownloadListener {
                override fun onSuccess(
                    uri: Uri,
                    md5: String,
                    extraMap: MutableMap<String, String>
                ) {
                }

                override fun onError(e: Throwable, isRetryError: Boolean) {
                }

                override fun onProcess(currentLength: Long, length: Long, process: Float) {
                }

            })
    }
}