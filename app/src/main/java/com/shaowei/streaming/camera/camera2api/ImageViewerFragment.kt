package com.shaowei.streaming.camera.camera2api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.shaowei.streaming.camera.utils.GenericListAdapter
import com.shaowei.streaming.camera.utils.decodeExifOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import kotlin.math.max

class ImageViewerFragment : Fragment() {
    private val args: ImageViewerFragmentArgs by navArgs()

    /** Default Bitmap decoding options */
    private val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        // keep Bitmaps at less than 1 MP
        if (max(outHeight, outWidth) > DOWNSAMPLE_SIZE) {
            val scaleFactorX = outWidth / DOWNSAMPLE_SIZE + 1
            val scaleFactory = outHeight / DOWNSAMPLE_SIZE + 1
            inSampleSize = max(scaleFactorX, scaleFactory)
        }
    }

    /** Bitmap transformation derived from passed arguments */
    private val bitmapTransformation: Matrix by lazy { decodeExifOrientation(args.orientation) }

    /** Flag indicating that there is depth data available for this image */
    private val isDepth: Boolean by lazy { args.depth }

    /** Data backing our Bitmap viewpager */
    private val bitmapList: MutableList<Bitmap> = mutableListOf()

    private fun imageViewFactory() = ImageView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ViewPager2(requireContext()).apply {
            // Populate the ViewPager and implement a cache of two media items
            offscreenPageLimit = 2
            adapter = GenericListAdapter(bitmapList, itemViewFactory = { imageViewFactory() }) { view, item, _ ->
                view as ImageView
                Glide.with(view).load(item).into(view)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view as ViewPager2
        lifecycleScope.launch(Dispatchers.IO) {
            // Load input image file
            val inputBuffer = loadInputBuffer()

            // Load the main JPEG image
            addItemToViewPager(view, decodeBitmap(inputBuffer, 0, inputBuffer.size))

            // If we have depth data attached, attempt to load it
            if (isDepth) {
                try {
                    val depthStart = findNextJpegEndMarker(inputBuffer, 2)
                    addItemToViewPager(view, decodeBitmap(inputBuffer, depthStart, inputBuffer.size - depthStart))

                    val confidenceStart = findNextJpegEndMarker(inputBuffer, depthStart)
                    addItemToViewPager(
                        view,
                        decodeBitmap(inputBuffer, confidenceStart, inputBuffer.size - confidenceStart)
                    )

                } catch (exception: RuntimeException) {
                    Log.e(TAG, "Invalid start marker for depth or confidence data")
                }
            }
        }
    }

    /**
     * Utility function used to find the markers indicating separation between JPEG data chunks
     */
    private fun findNextJpegEndMarker(jpegBuffer: ByteArray, start: Int): Int {
        // Sanitize input arguments
        assert(start >= 0) { "Invalid start marker:$start" }
        assert(jpegBuffer.size > start) { "Buffer size (${jpegBuffer.size}) smaller than start marker ($start)" }

        // Perform a linear search until the delimiter is found
        for (i in start until jpegBuffer.size - 1) {
            if (jpegBuffer[i].toInt() == JPEG_DELIMITER_BYTES[0] &&
                jpegBuffer[i + 1].toInt() == JPEG_DELIMITER_BYTES[1]
            ) {
                return i + 2
            }
        }

        // If we reach this, it means that no marker was found
        throw RuntimeException("Separator marker not found in buffer (${jpegBuffer.size})")
    }

    /** Utility function used to add an item to the viewpager and notify it, in the main thread */
    private fun addItemToViewPager(viewPager: ViewPager2, item: Bitmap) = viewPager.post {
        bitmapList.add(item)
        viewPager.adapter!!.notifyDataSetChanged()
    }

    /** Utility function used to read input file into a byte array */
    private fun loadInputBuffer(): ByteArray {
        val inputFile = File(args.filePath)
        return BufferedInputStream(inputFile.inputStream()).let { stream ->
            ByteArray(stream.available()).also {
                stream.read(it)
                stream.close()
            }
        }
    }

    /** Utility function used to decode a [Bitmap] from a byte array */
    private fun decodeBitmap(buffer: ByteArray, start: Int, length: Int): Bitmap {
        // Load bitmap from given buffer
        val bitmap = BitmapFactory.decodeByteArray(buffer, start, length, bitmapOptions)

        // Transform bitmap orientation using provided metadata
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, bitmapTransformation, true)
    }


    companion object {
        private val TAG = ImageViewerFragment::class.java.simpleName

        /** Maximum size of [Bitmap] decoded */
        private const val DOWNSAMPLE_SIZE: Int = 1024  // 1MP

        /** These are the magic numbers used to separate the different JPG data chunks */
        private val JPEG_DELIMITER_BYTES = arrayOf(-1, -39)
    }
}