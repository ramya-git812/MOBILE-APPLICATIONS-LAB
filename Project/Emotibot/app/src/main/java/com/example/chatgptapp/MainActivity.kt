package com.example.chatgptapp

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    lateinit var txtResponse: TextView
    lateinit var idTVQuestion: TextView
    lateinit var etQuestion: TextInputEditText

    // Create a list to store the conversation history with both user and bot messages.
    private val conversationHistory = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etQuestion = findViewById(R.id.etQuestion)
        idTVQuestion = findViewById(R.id.idTVQuestion)
        txtResponse = findViewById(R.id.txtResponse)

        // Handle the Send action from the keyboard.
        etQuestion.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val question = etQuestion.text.toString().trim()
                if (question.isNotEmpty()) {
                    // Append the user's question to the conversation history.
                    appendToConversationHistory("user", question)

                    // Show the current conversation history in the TextView.
                    txtResponse.text = getConversationHistory()

                    // Clear the text input field after sending the message.
                    etQuestion.setText("")

                    // Call the API to get a response.
                    getResponse(question) { response ->
                        runOnUiThread {
                            // Append the bot's response to the conversation history.
                            appendToConversationHistory("bot", response)

                            // Update the TextView to show the updated conversation.
                            txtResponse.text = getConversationHistory()
                        }
                    }
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    // Function to append each message to the conversation history.
    private fun appendToConversationHistory(role: String, message: String) {
        // Add the message to the conversation history.
        conversationHistory.add(mapOf("role" to role, "content" to message))
    }

    // Function to get the conversation history as a formatted string.
    private fun getConversationHistory(): String {
        val history = StringBuilder()
        for (message in conversationHistory) {
            val content = message["content"]
            history.append(content).append("\n") // Append only the content
        }
        return history.toString()
    }


    // Function to make the API request.
        fun getResponse(question: String, callback: (String) -> Unit) {
        val url = "https://walleye-relevant-informally.ngrok-free.app/api/chat"

        // Create a JSONArray to hold messages
        val messages = JSONArray()

        // Add the existing conversation history to the messages list.
        conversationHistory.forEach {
            val message = JSONObject().put("role", it["role"]).put("content", it["content"])
            messages.put(message)  // Add each message as a JSONObject to the JSONArray
        }

        // Add the user's current question to the messages list.
        val newMessage = JSONObject().put("role", "user").put("content", question)
        messages.put(newMessage)

        // Create the request body with the model and conversation history.
        val requestBody = JSONObject().apply {
            put("model", "rohitrajt/emotibot")
            put("messages", messages)  // Pass the messages as a JSONArray, not as a string
            put("stream", false)
        }

        // Print the request body to check the format.
        Log.d("API Request", "Request Body: ${requestBody.toString(4)}") // Pretty print JSON with indentation

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        // Send the request to the API and handle the response.
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API call failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    try {
                        val jsonObject = JSONObject(body)
                        // Extract the message content from the response.
                        val messageContent = jsonObject.getJSONObject("message").getString("content")
                        callback(messageContent)
                    } catch (e: Exception) {
                        Log.e("error", "Failed to parse response", e)
                    }
                } else {
                    Log.v("data", "empty response")
                }
            }
        })
    }
}
