package com.shaowei.streaming.camera.camera2api

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shaowei.streaming.R
import com.shaowei.streaming.camera.utils.GenericListAdapter

class SelectorFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        RecyclerView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view as RecyclerView
        view.apply {
            layoutManager = LinearLayoutManager(requireContext())
            val cameraManger = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraList = enumerateCameras(cameraManger)

            val layoutId = android.R.layout.simple_list_item_1
            adapter = GenericListAdapter(cameraList, itemLayoutId = layoutId) { view, item, _ ->
                view.findViewById<TextView>(android.R.id.text1).text = item.title
                view.setOnClickListener {
                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                        .navigate(SelectorFragmentDirections.actionSelectorToCamera(item.cameraId, item.format))
                }
            }
        }
    }


    companion object {
        /** Helper class used as a data holder for each selectable camera format item */
        private data class FormatItem(val title: String, val cameraId: String, val format: Int)

        private fun lensOrientationString(value: Int) = when (value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        /** Helper function used to list all compatible cameras and supported pixel formats */
        private fun enumerateCameras(cameraManager: CameraManager): List<FormatItem> {
            val availableCameras = mutableListOf<FormatItem>()

            // Get list of all compatible cameras
            val cameraIds = cameraManager.cameraIdList.filter {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
            }

            // Iterate over the list of cameras and return all the compatible ones
            cameraIds.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation =
                    lensOrientationString(characteristics.get(CameraCharacteristics.LENS_FACING)!!)

                // Query the available capabilities and output formats
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val outputFormats =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

                // All cameras must support JPEG output so we don't need to check characteristics
                availableCameras.add(FormatItem("$orientation JPEG($id)", id, ImageFormat.JPEG))

                // Return cameras that support RAW capability
                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) && outputFormats.contains(
                        ImageFormat.RAW_SENSOR
                    )
                ) {
                    availableCameras.add(FormatItem("$orientation RAM($id)", id, ImageFormat.RAW_SENSOR))
                }

                // Return cameras that support JPEG DEPTH capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
                    ) &&
                    outputFormats.contains(ImageFormat.DEPTH_JPEG)
                ) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation DEPTH ($id)", id, ImageFormat.DEPTH_JPEG
                        )
                    )
                }
            }

            return availableCameras
        }
    }

}