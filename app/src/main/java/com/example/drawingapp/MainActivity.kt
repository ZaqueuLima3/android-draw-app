package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var currentColor: Int = Color.BLACK
    private var mSizeBrush: Int = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(mSizeBrush.toFloat())

        ib_brush.setOnClickListener {
            showDialogBrushSize()
        }

        ib_gallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent, GALLERY_CODE)
            } else {
                requestPermission()
            }
        }

        ib_colors.setOnClickListener {
            showColorPicker()
        }

        ib_undo.setOnClickListener {
            drawing_view.undo()
        }

        ib_remake.setOnClickListener {
            drawing_view.remake()
        }

        ib_save.setOnClickListener {
            if (isReadStorageAllowed()) {
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {
                requestPermission()
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted now you can read the storage",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Oops you just denied the permission", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_CODE) {
                try {
                    data?.data?.let {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(it)
                    }
                } catch (err: Exception) {
                    err.printStackTrace()
                }
            }
        }
    }

    private fun showColorPicker() {
        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose color")
            .initialColor(currentColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .setPositiveButton("choose") { dialog: DialogInterface, lastSelectedColor: Int, allColors: Array<Int> ->
                currentColor = lastSelectedColor
                drawing_view.setColor(currentColor)
            }
            .build()
            .show()
    }

    private fun showDialogBrushSize() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size)
        val window = brushDialog.window
        window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val seek = brushDialog.sk_brush_size

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                mSizeBrush = progress
                seek.progress = mSizeBrush
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                seek.progress = mSizeBrush
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                drawing_view.setSizeForBrush(mSizeBrush.toFloat())
                seek.progress = mSizeBrush
                brushDialog.dismiss()
            }
        })

        brushDialog.show()
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Need permission to ad a Background", Toast.LENGTH_SHORT).show()
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), STORAGE_PERMISSION_CODE
        )
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {
        override fun doInBackground(vararg params: Any?): String {
            var result = ""

            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val file =
                        File(
                            externalCacheDir!!.absoluteFile.toString()
                                    + File.separator + "DrawApp_"
                                    + System.currentTimeMillis() / 1000
                                    + ".png"
                        )

                    val fos = FileOutputStream(file)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = file.absolutePath
                } catch (err: java.lang.Exception) {
                    result = ""
                    err.printStackTrace()
                }
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result?.isNotEmpty()!!) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully : $result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY_CODE = 2
    }
}