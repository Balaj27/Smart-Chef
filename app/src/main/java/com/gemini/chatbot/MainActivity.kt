package com.gemini.chatbot

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gemini.chatbot.adapter.GeminiAdapter
import com.gemini.chatbot.model.DataResponse
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var image: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuButton: ImageView
    private lateinit var auth: FirebaseAuth

    private var bitmap: Bitmap? = null
    private lateinit var imageUri: String
    private var responseData = arrayListOf<DataResponse>()
    private lateinit var adapter: GeminiAdapter

    // Memory for conversation history
    private val conversationHistory = StringBuilder()

    // Handle media picking from gallery
    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            image.setImageURI(uri)
        } else {
            Log.d("Photopicker", "No media selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if the user is logged in; if not, redirect to LoginActivity
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()  // Close MainActivity to prevent access
            return
        }

        setContentView(R.layout.activity_main)  // Only set the content view if the user is authenticated

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        menuButton = findViewById(R.id.menu_button)
        editText = findViewById(R.id.ask_edit_text)
        button = findViewById(R.id.ask_button)
        image = findViewById(R.id.select_iv)
        recyclerView = findViewById(R.id.recycler_view_id)

        // Initialize RecyclerView and Adapter
        adapter = GeminiAdapter(this, responseData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Set up the menu button to open the navigation drawer
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set up NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        // Set the user email in the navigation header
        val headerView: View = navigationView.getHeaderView(0)
        val navEmailTextView: TextView = headerView.findViewById(R.id.nav_header_subtitle)
        navEmailTextView.text = currentUser.email

        // Set up the image picker
        image.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Handle the user input when the send button is clicked
        button.setOnClickListener {
            val prompt = editText.text.toString()

            if (prompt.isNotEmpty()) {
                editText.setText("")
                handlePrompt(prompt)
            }
        }
    }

    // Handle NavigationView item selection
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                // Handle logout
                auth.signOut()
                Log.d("MainActivity", "User signed out")
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Close MainActivity to prevent back navigation
            }
            // Handle other items here if needed
        }

        // Close the drawer after the item is selected
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Handle the prompt and generate a response using the AI model
    private fun handlePrompt(prompt: String) {
        if (isFoodRelated(prompt)) {
            responseData.add(DataResponse(prompt, 0, prompt))
            adapter.notifyDataSetChanged()
            scrollToBottom()

            conversationHistory.append("User: $prompt\n")

            // AI Model Implementation
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = getString(R.string.api_key)
            )

            GlobalScope.launch {
                try {
                    val aiPrompt = """
                        You are a smart chef assistant. The user is asking a food-related question.
                        Always respond with helpful information regarding recipes, cooking techniques, ingredient replacements, or other culinary advice.
                        
                        User asked: $prompt
                        
                        AI response:
                    """.trimIndent()

                    val aiResponse = generativeModel.generateContent(conversationHistory.toString() + aiPrompt).text

                    runOnUiThread {
                        processAIResponse(aiResponse ?: "Sorry, I couldn't generate a response.")
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "Error fetching AI response: ", e)
                }
            }
        } else {
            processAIResponse("Please ask something related to cooking, recipes, ingredients, or food preparation.")
        }
    }

    // Process and display the AI response
    private fun processAIResponse(responseText: String) {
        Log.d("PROCESS_RESPONSE", "AI Response: $responseText")

        // Replace special symbols with HTML tags
        val formattedResponse = formatResponse(responseText)

        // Wrap the formatted response in a TextView tag with a specific color
        val coloredResponse = "<font color='#000000'>$formattedResponse</font>"

        responseData.add(DataResponse(coloredResponse, 1, coloredResponse))
        adapter.notifyDataSetChanged()
        scrollToBottom()
    }

    // Format the AI response to include HTML elements for display
    private fun formatResponse(responseText: String): String {
        var htmlText = responseText
            .replace("**", "<b>")   // Bold
            .replace("__", "<i>")    // Italics
            .replace("~~", "<strike>")  // Strikethrough
            .replace(Regex("""\* (.+)"""), "<ul><li>$1</li></ul>")  // Bullets
            .replace("\n", "<br>")  // Line breaks

        // Ensure proper closing tags for bold, italics, and strikethrough
        htmlText = htmlText.replace("<b>(.*?)(?!</b>)".toRegex(), "$1</b>")
        htmlText = htmlText.replace("<i>(.*?)(?!</i>)".toRegex(), "$1</i>")
        htmlText = htmlText.replace("<strike>(.*?)(?!</strike>)".toRegex(), "$1</strike>")
        htmlText = htmlText.replace("</ul><ul>".toRegex(), "")

        return htmlText
    }

    // Scroll to the bottom of the RecyclerView to show the latest messages
    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    // Check if the user's prompt is related to food
    private fun isFoodRelated(prompt: String): Boolean {
        val foodKeywords = listOf(
            "food", "recipe", "ingredient", "dish", "cook", "bake", "grill", "boil", "fry",
            "meal", "dinner", "lunch", "breakfast", "snack", "dessert", "cuisine", "kitchen",
            "prepare", "how to", "replace", "substitute", "menu", "restaurant", "eat", "dine",
            "make", "chicken", "mutton", "beef", "sauce"
        )
        return foodKeywords.any { prompt.contains(it, ignoreCase = true) }
    }

    // Handle the back button press to close the drawer if it's open
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
