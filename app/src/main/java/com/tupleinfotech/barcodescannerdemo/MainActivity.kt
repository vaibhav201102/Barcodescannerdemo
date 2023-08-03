package com.tupleinfotech.barcodescannerdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.tupleinfotech.barcodescannerdemo.databinding.ActivityMainBinding
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding?= null
    private val binding get() = _binding!!

    private val CAMERA_REQUEST_CODE = 10
    private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>?= null

    private var imageAnalysis: ImageAnalysis?= null
    private var cameraProvider: ProcessCameraProvider?= null
    private var previewView: PreviewView?= null
    private var barcodeText: String?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraPermission()

    }
    override fun onBackPressed(){
        this.finishAffinity()
    }

    private fun cameraPermission(){

        if (!hasCameraPermission()) {
            requestPermission()
        }

        previewView = binding.camPreview

        cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
        cameraProviderFuture?.addListener(Runnable {
            try {
                cameraProvider = cameraProviderFuture!!.get()
                bindPreview(cameraProvider!!)
            } catch (_: ExecutionException) {
            } catch (_: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this@MainActivity))

    }



    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView?.surfaceProvider)

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //keep the latest
            .build()

        val barcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS, Barcode.FORMAT_ALL_FORMATS
            ).build()

        val scanner = BarcodeScanning.getClient(barcodeScannerOptions)
        imageAnalysis!!.setAnalyzer(
            Executors.newSingleThreadExecutor()
        ) { imageProxy ->
            @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError") val mediaImage =
                imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val result = scanner.process(image)
                result.addOnSuccessListener { barcodes -> processBarcode(barcodes) }
                    .addOnFailureListener {
                        Toast.makeText(this@MainActivity,"Could not detect barcode!",
                            Toast.LENGTH_SHORT).show()}
                    .addOnCompleteListener {
                        mediaImage.close()
                        imageProxy.close()
                    }
            }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
    }

    private fun processBarcode(barcodes: List<Barcode>) {
        Log.e("barcodes:", barcodes.toString())
        for (barcode in barcodes) {
            barcodeText = barcode.rawValue
            println(barcodeText)
//            barcodetext = barcodeText!!
//            println(barcodetext)

            if (barcodeText?.isNotEmpty() == true){
                cameraProvider?.unbindAll()

                val bundle = Bundle()
                bundle.putString("Scanner", barcodeText)

                binding.scannedtext.text = barcodeText.toString()

                /*val controller = Navigation.findNavController(requireView())
                //setFragmentResult("Scanner",bundle)
                controller.previousBackStackEntry?.savedStateHandle?.set("Scanner",barcodeText)
                controller.popBackStack(R.id.scannerFragment, true)*/

                break
            }
        }
    }



    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.CAMERA),
            10
        )
    }
}