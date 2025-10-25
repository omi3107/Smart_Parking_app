package com.example.parkkar.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.parkkar.R

object MarkerUtils {

    fun createBitmapFromView(context: Context, text: String): Bitmap {
        val inflater = LayoutInflater.from(context)
        val markerView = inflater.inflate(R.layout.marker_view, null)

        val markerText = markerView.findViewById<TextView>(R.id.marker_text)
        markerText.text = text

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return bitmap
    }
}
