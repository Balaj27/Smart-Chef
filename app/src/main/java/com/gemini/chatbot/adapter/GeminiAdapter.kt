package com.gemini.chatbot.adapter

import android.content.Context
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gemini.chatbot.R
import com.gemini.chatbot.model.DataResponse

class GeminiAdapter(private val context: Context, private val responseData: ArrayList<DataResponse>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userText: TextView = itemView.findViewById(R.id.tv_user_response)
        private val userImage: ImageView = itemView.findViewById(R.id.iv_user_res)

        fun bind(data: DataResponse) {
            userText.text = data.message

            if (data.imageUri.isNotEmpty()) {
                userImage.visibility = View.VISIBLE
                Glide.with(context).load(data.imageUri).into(userImage)
            } else {
                userImage.visibility = View.GONE
            }
        }
    }

    inner class AIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val aiText: TextView = itemView.findViewById(R.id.tv_gemini_response)
        private val aiImage: ImageView = itemView.findViewById(R.id.ai_image)

        fun bind(data: DataResponse) {
            // Convert the HTML string to a Spanned object
            val formattedText: Spanned = HtmlCompat.fromHtml(data.message, HtmlCompat.FROM_HTML_MODE_LEGACY)
            aiText.text = formattedText

            // Enable clickable links if there are any in the HTML
            aiText.movementMethod = LinkMovementMethod.getInstance()

            if (data.imageUri.isNotEmpty()) {
                aiImage.visibility = View.VISIBLE
                Glide.with(context).load(data.imageUri).into(aiImage)
            } else {
                aiImage.visibility = View.GONE
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (responseData[position].isAI == 0) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.gemini_layout, parent, false)
            AIViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is UserViewHolder) {
            holder.bind(responseData[position])
        } else if (holder is AIViewHolder) {
            holder.bind(responseData[position])
        }
    }

    override fun getItemCount(): Int {
        return responseData.size
    }

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
    }
}
