package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import kotlinx.android.synthetic.main.dialog_brush_size.*

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
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted now you can read the storage", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Oops you just denied the permission", Toast.LENGTH_SHORT).show()
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
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

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

    private fun isReadStorageAllowed() : Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY_CODE = 2
    }
}