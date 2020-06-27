package com.android.chatapp

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sendbird.android.BaseMessage
import kotlinx.android.synthetic.main.list_item_message.view.*
import kotlinx.android.synthetic.main.list_item_message_r.view.*
import kotlinx.android.synthetic.main.list_item_mileage.view.*
import kotlinx.android.synthetic.main.list_item_mileage_r.view.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatRecyclerAdapter(var list: ArrayList<BaseMessage>, var context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_message_r, parent, false)
                ChatViewHolderR(view)
            }
            2 -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_message, parent, false)
                ChatViewHolder(view)
            }
            3 -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_mileage_r, parent, false)
                ChatViewHolderMileageR(view)
            }
            else -> {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_mileage, parent, false)
                ChatViewHolderMileage(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is ChatViewHolderR) {
            with(viewHolder.itemView) {
                tvMsgR.text = list[position].message
                tvTimeR.text = getDate(list[position].createdAt, "hh:mm")
            }
        } else if (viewHolder is ChatViewHolder) {
            with(viewHolder.itemView) {
                tvMsg.text = list[position].message
                tvUser.text = list[position].sender.userId
                tvTime.text = getDate(list[position].createdAt, "hh:mm")
            }
        } else if (viewHolder is ChatViewHolderMileageR) {
            with(viewHolder.itemView) {
                val jsonObject = JSONObject(list[position].data)
                tvTimeMR.text = getDate(list[position].createdAt, "hh:mm")
                etNameR.text = jsonObject.optString("name")
                etStartR.text = jsonObject.optString("start")
                etEndR.text = jsonObject.optString("end")
                tvMilesR.text = jsonObject.optString("miles")+" miles"
            }
        } else if (viewHolder is ChatViewHolderMileage) {
            with(viewHolder.itemView) {
                val jsonObject = JSONObject(list[position].data)
                tvUserM.text = list[position].sender.userId
                tvTimeM.text = getDate(list[position].createdAt, "hh:mm")
                etName.text = jsonObject.optString("name")
                etStart.text = jsonObject.optString("start")
                etEnd.text = jsonObject.optString("end")
                tvMiles.text = jsonObject.optString("miles")+" miles"
            }
        }
    }

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    inner class ChatViewHolderR(view: View) : RecyclerView.ViewHolder(view) {

    }

    inner class ChatViewHolderMileage(view: View) : RecyclerView.ViewHolder(view) {

    }

    inner class ChatViewHolderMileageR(view: View) : RecyclerView.ViewHolder(view) {

    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }

    override fun getItemViewType(position: Int): Int {
        val message = list[position]
        if (message.sender.userId == sharedPreferences.getString(
                "user_id",
                ""
            ) && message.data.isEmpty()
        )
            return 1
        else if (message.sender.userId != sharedPreferences.getString(
                "user_id",
                ""
            ) && message.data.isEmpty()
        )
            return 2
        else if (message.sender.userId == sharedPreferences.getString(
                "user_id",
                ""
            ) && !message.data.isEmpty()
        )
            return 3
        else
            return 4
    }

    public fun setSharedPref(sp: SharedPreferences) {
        sharedPreferences = sp
    }

    fun updateList(msg: BaseMessage) {
        list.add(msg)
        notifyItemInserted(list.size - 1)
    }

    fun clearList() {
        list.clear()
    }
}
