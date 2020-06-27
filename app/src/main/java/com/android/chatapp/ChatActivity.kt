package com.android.chatapp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sendbird.android.*
import com.sendbird.android.BaseChannel.SendUserMessageHandler
import com.sendbird.android.BaseMessageParams.MentionType
import com.sendbird.android.BaseMessageParams.PushNotificationDeliveryOption
import com.sendbird.android.OpenChannel.*
import com.sendbird.android.SendBird.ChannelHandler
import kotlinx.android.synthetic.main.activity_chat.*
import org.json.JSONObject
import kotlin.math.roundToInt


class ChatActivity : AppCompatActivity() {

    lateinit var send: Button
    lateinit var startLoc: EditText
    lateinit var endLoc: EditText
    lateinit var name: EditText
    var latLng1 = Location("")
    var latLng2 = Location("")
    var msgList = ArrayList<BaseMessage>()
    lateinit var chatRecyclerAdapter: ChatRecyclerAdapter
    lateinit var mChannel: OpenChannel
    lateinit var friendId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)


        chatRecyclerAdapter = ChatRecyclerAdapter(arrayListOf(), this)
        chatRecyclerAdapter.setSharedPref(PreferenceManager.getDefaultSharedPreferences(this))
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = chatRecyclerAdapter
        friendId = intent.getStringExtra("friendId")


        getChannel("sendbird_open_channel_8129_8e9e154c24c87f85dbce83a2dd2ebcfd82de41c5",
            OpenChannelGetHandler { openChannel, e ->
                if (e != null) {    // Error.
                    return@OpenChannelGetHandler
                }
                openChannel.enter(OpenChannelEnterHandler { e ->
                    if (e != null) {    // Error.
                        return@OpenChannelEnterHandler
                    }
                    mChannel = openChannel
                })
            })


        SendBird.addChannelHandler("abcde", object : ChannelHandler() {
            override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                when (message) {
                    is UserMessage -> {
                        msgList.add(message)
                        chatRecyclerAdapter.updateList(message)
                        rv.scrollToPosition(msgList.size - 1)
                    }
                    is FileMessage -> {
                    }
                    is AdminMessage -> {
                    }
                }
            }
        })

        etType.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (!etType.text.toString().trim().isEmpty()) {
                        sendMessage(etType.text.toString(), "")
                        etType.setText("")
                    }
                }
                return true
            }
        })

        ivAdd.setOnClickListener {
            if (rlAddMileage.visibility == View.GONE) {
                rlAddMileage.visibility = View.VISIBLE
                ivAdd.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                rlAddMileage.visibility = View.GONE
                ivAdd.setImageResource(android.R.drawable.ic_input_add)
            }
        }

        tvAddMileage.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val popUpLayout = LayoutInflater.from(this).inflate(R.layout.dialog_mileage, null)
            dialog.setContentView(popUpLayout)
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.width = 900
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT

            name = popUpLayout.findViewById<EditText>(R.id.etName)
            startLoc = popUpLayout.findViewById<EditText>(R.id.etStart)
            endLoc = popUpLayout.findViewById<EditText>(R.id.etEnd)
            val cancel = popUpLayout.findViewById<Button>(R.id.btnCancel)
            send = popUpLayout.findViewById<Button>(R.id.btnSend)
            name.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    enableSend()
                }

            })
            startLoc.setOnClickListener {
                startActivityForResult(Intent(this, MapsActivity::class.java), 1)
            }
            endLoc.setOnClickListener {
                startActivityForResult(Intent(this, MapsActivity::class.java), 2)
            }
            cancel.setOnClickListener {
                dialog.dismiss()
            }
            send.setOnClickListener {
                val jsonObject = JSONObject()
                jsonObject.put("start", startLoc.text.toString().trim())
                jsonObject.put("end", endLoc.text.toString().trim())
                jsonObject.put("name", name.text.toString().trim())
                val miles = latLng1.distanceTo(latLng2) * 0.00062
                jsonObject.put("miles", miles.roundToInt())
                sendMessage("Mileage", jsonObject.toString())
                dialog.dismiss()
            }
            dialog.show()
            dialog.window!!.attributes = lp
            rlAddMileage.visibility = View.GONE
            ivAdd.setImageResource(android.R.drawable.ic_input_add)
        }

    }

    fun enableSend() {
        if (name.text.toString().trim().isNotEmpty() && startLoc.text.toString().trim()
                .isNotEmpty() && endLoc.text.toString().trim().isNotEmpty()
        ) {
            send.isEnabled = true
            send.setBackgroundResource(R.drawable.bg_btn_blue)
        } else {
            send.isEnabled = false
            send.setBackgroundResource(R.drawable.bg_btn_grey)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            startLoc.setText(data?.getStringExtra("address"))
            latLng1.latitude = data!!.getDoubleExtra("lat", 0.0)
            latLng1.longitude = data!!.getDoubleExtra("long", 0.0)
            enableSend()

        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            endLoc.setText(data?.getStringExtra("address"))
            latLng2.latitude = data!!.getDoubleExtra("lat", 0.0)
            latLng2.longitude = data!!.getDoubleExtra("long", 0.0)
            enableSend()
        }
    }


    public fun sendMessage(msg: String, data: String) {

        val userIDsToMention: MutableList<String> = ArrayList()
        userIDsToMention.add(friendId)

        val params = UserMessageParams()
            .setMessage(msg)
            .setMentionType(MentionType.USERS)
            .setData(data)
            .setMentionedUserIds(userIDsToMention)// Either USERS or CHANNEL // Or .setMentionedUsers(LIST_OF_USERS_TO_MENTION)
            .setPushNotificationDeliveryOption(PushNotificationDeliveryOption.DEFAULT) // Either DEFAULT or SUPPRESS


        mChannel.sendUserMessage(params,
            SendUserMessageHandler { userMessage, e ->
                if (e != null) {    // Error.
                    return@SendUserMessageHandler
                }
                msgList.add(userMessage)
                chatRecyclerAdapter.updateList(userMessage)
                rv.scrollToPosition(msgList.size - 1)
            })
    }

    fun finishActivity(view: View) {
        finish();
    }


}