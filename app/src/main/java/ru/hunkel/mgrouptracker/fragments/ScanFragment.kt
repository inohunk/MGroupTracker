package ru.hunkel.mgrouptracker.fragments


import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.fragment_scan.view.*
import ru.hunkel.mgrouptracker.R

class ScanFragment : Fragment() {
    lateinit var detector: BarcodeDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_scan, container, false)
        view.scan_button.setOnClickListener {
            dispatchTakePictureIntent()
            detector = BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.DATA_MATRIX or Barcode.QR_CODE)
                .build()
            if (!detector.isOperational) {
                Toast.makeText(context, "Could not set up the detector!", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private val REQUEST_IMAGE_CAPTURE = 1

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(context?.packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val frame = Frame.Builder().setBitmap(imageBitmap).build()
            val barcodes = detector.detect(frame)

            try {
                val thisCode = barcodes.valueAt(0)
                Toast.makeText(context, thisCode.rawValue, Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
            }
            findNavController().navigate(ScanFragmentDirections.actionGoToMainFragment())
        }
    }

}
