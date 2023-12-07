package com.example.voice2text

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.speech.RecognizerIntent
import android.widget.ImageView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import android.widget.Button
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import android.content.Intent
import android.content.SyncStatusObserver
import org.json.JSONArray
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // creating variables on below line.
    lateinit var responseTV: TextView
    lateinit var questionTV: TextView
    lateinit var queryEdt: TextInputEditText
    lateinit var micIV: ImageView
    lateinit var btnSpeak : Button
    lateinit var tts : TextToSpeech
    lateinit var send : ImageView

    val url = "https://09d4-128-210-0-165.ngrok-free.app/v1/chat/completions" // Updated API URL

    private val REQUEST_CODE_SPEECH_INPUT = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // initializing variables on below line.
        responseTV = findViewById(R.id.idTVResponse)
        questionTV = findViewById(R.id.idTVQuestion)
        queryEdt = findViewById(R.id.idEdtQuery)
        micIV = findViewById(R.id.idMic)
        btnSpeak = findViewById(R.id.btn_speak)
        send = findViewById(R.id.idSend)

        // for text to speech
        btnSpeak!!.isEnabled = false
        tts = TextToSpeech(this,this)
        btnSpeak.setOnClickListener {speakOut()}

        // setting the imageView to load the speech
        micIV.setOnClickListener{
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            }catch (e: Exception)
            {
                // on below line we are displaying error message in toast
                Toast
                    .makeText(
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

        send.setOnClickListener{
            responseTV.text = "Please wait.."
            // validating text
            if (queryEdt.text.toString().length > 0) {
                // calling get response to get the response.
                getResponse(queryEdt.text.toString())
            } else {
                Toast.makeText(this, "Please enter your query..", Toast.LENGTH_SHORT).show()
            }
        }

        // adding editor action listener for edit text on below line.
        queryEdt.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                // setting response tv on below line.
                responseTV.text = "Please wait.."
                // validating text
                if (queryEdt.text.toString().length > 0) {
                    // calling get response to get the response.
                    getResponse(queryEdt.text.toString())
                } else {
                    Toast.makeText(this, "Please enter your query..", Toast.LENGTH_SHORT).show()
                }
                return@OnEditorActionListener true
            }
            false
        })
    }

    override fun onInit(status: Int){
        if(status == TextToSpeech.SUCCESS){
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            } else {
                btnSpeak!!.isEnabled = true
            }
        }
    }

    private fun speakOut(){
        val text = responseTV.text.toString()
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    public override fun onDestroy() {
        // shutdown TTS when the actibity is destroyed
        if(tts!=null){
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    private fun getResponse(query: String) {
        // setting text on for question on below line.
        questionTV.text = query
        queryEdt.setText("")
        // creating a queue for request queue.
        val queue: RequestQueue = Volley.newRequestQueue(applicationContext)
        // creating a json object on below line.
        val jsonObject = JSONObject().apply {
            // The request structure remains largely the same
            val messagesArray = JSONArray()
            val messageObject = JSONObject().apply {
                put("role", "user")
                put("content", query) // 'query' should be the user input string
            }
            messagesArray.put(messageObject)
            put("messages", messagesArray)
        }

        val postRequest: JsonObjectRequest = object : JsonObjectRequest(Method.POST, url, jsonObject,
            Response.Listener { response ->
                // Extracting the message content from the new response format
                val choicesArray = response.getJSONArray("choices")
                if (choicesArray.length() > 0) {
                    val firstChoice = choicesArray.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    val content = message.getString("content") // Updated to match new response format

                    // Updating the TextView with the response content
                    responseTV.text = content
                }
            },
            Response.ErrorListener { error ->
                Log.e("TAGAPI", "Error is : " + error.message + "\n" + error)
            }) {
            override fun getHeaders(): kotlin.collections.MutableMap<kotlin.String, kotlin.String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"
                params["Authorization"] = "Bearer YOUR_API_KEY" // Replace with your actual API key
                return params
            }
        }

        // on below line adding retry policy for our request.
        postRequest.setRetryPolicy(object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 50000
            }

            override fun getCurrentRetryCount(): Int {
                return 50000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        })
        // on below line adding our request to queue.
        queue.add(postRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_CODE_SPEECH_INPUT){
            if(resultCode == RESULT_OK && data != null){
                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                queryEdt.setText(Objects.requireNonNull(res)[0])
            }
        }
    }

}


