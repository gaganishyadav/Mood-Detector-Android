package com.example.mooddetector

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mooddetector.databinding.ActivityMainBinding
import com.example.mooddetector.ml.MoodDetector
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ColorSpaceType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var tvOutput: TextView
    private lateinit var textView2: TextView

    private val clientId = "ce372c9b921b4a2999b688a60dcfdd1d"
    private val redirectUri = "http://com.example.mooddetector/callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        imageView = binding.imageView
        button = binding.btnCaptureImage
        tvOutput = binding.tvOutput
        textView2 = binding.textView2
        val buttonLoad = binding.btnLoadImage
        Log.e("TAG", "Hello")

        button.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                takePicturePreview.launch(null)
                onStart()
            } else {
                requestPermission.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

        private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicturePreview.launch(null)
            } else {
                Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show()
            }
        }

        private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->
             if(bitmap!=null){
                 imageView.setImageBitmap(bitmap)
                 outputGenerator(bitmap)
                 }
            }


    private fun outputGenerator(bitmap: Bitmap){
        val model = MoodDetector.newInstance(this)

        val newBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
        val tfimage = TensorImage(DataType.FLOAT32)
        tfimage.load(newBitmap)
        val tfimagegrayscale = TransformToGrayscaleOp().apply(tfimage)
        val tensorbuffr=tfimagegrayscale.tensorBuffer
        val tensorimg = TensorImage(DataType.FLOAT32)
        tensorimg.load(tensorbuffr,ColorSpaceType.GRAYSCALE)
        val byteBuffer = tensorimg.buffer

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 48, 48, 1), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

//        val output = outputFeature0.floatArray.joinToString(",","[","]")
//        Log.d("TAG", output)

        val pattern = "#.0#" // rounds to 2 decimal places if needed
        val locale = Locale.ENGLISH
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(locale))
        formatter.roundingMode = RoundingMode.HALF_EVEN // this is the default rounding mode anyway

//        val output = outputFeature0.floatArray.joinToString(", ", "[", "]") { value ->
//            formatter.format(value)
//        }

        val output = outputFeature0.floatArray
        if(output[0].toInt()==1){
            tvOutput.text = "Angry"
        }
        else if(output[1].toInt()==1){
            tvOutput.text = "Disgust"
        }
        else if(output[2].toInt()==1){
            tvOutput.text = "Fear"
        }
        else if(output[3].toInt()==1){
            tvOutput.text = "Happy"
        }
        else if(output[4].toInt()==1){
            tvOutput.text = "Sad"
        }
        else if(output[5].toInt()==1){
            tvOutput.text = "Surprise"
        }
        else if(output[6].toInt()==1){
            tvOutput.text = "Neutral"
        }
        else{
            tvOutput.text = "Error"
        }

        //Log.d("TAG", output)

        //tvOutput.text = output


        //tvOutput.text = outputFeature0.toString()
        Log.d("TAG", outputs.toString())
        Log.d("TAG", outputFeature0.toString())

//        val data1 = outputFeature0.floatArray
//        Log.d("TAG2", outputFeature0.dataType.toString())
//        Log.d("TAG2", data1[0].toString())

//        val probabilityBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.UINT8)

        // Releases model resources if no longer used.
        model.close()

    }

    override fun onStart() {
        super.onStart()
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.e("TAG", "Connected! Yay!")
                // Now you can start interacting with App Remote
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("TAG", "Error in connecting to Spotify")
                Log.e("TAG", throwable.message, throwable)
                // Something went wrong when attempting to connect! Handle errors here
            }
        })

    }

    private fun connected() {
        spotifyAppRemote?.let {
            // Play a playlist
            if (tvOutput.text == "Happy") {
                textView2.text = "Playing Music on Spotify"
                val playlistURI = "spotify:track:4IWZsfEkaK49itBwCTFDXQ"
                it.playerApi.play(playlistURI)
            }
            // Subscribe to PlayerState
//            it.playerApi.subscribeToPlayerState().setEventCallback {
//                val track: Track = it.track
//                Log.d("MainActivity", track.name + " by " + track.artist.name)
//            }
        }
    }


    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }

    }


}