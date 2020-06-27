package com.android.chatapp

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sendbird.android.SendBird
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_connect.setOnClickListener {
            if (etUserId.text.toString().trim().isNotEmpty() && etFriendId.text.toString().trim()
                    .isNotEmpty()
            ) {
                progress_bar.visibility = View.VISIBLE
                SendBird.init("75E567D1-DFB4-4435-805A-FC2828A01A2E", this)
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("user_id",etUserId.text.toString().trim()).apply()
                SendBird.connect(etUserId.text.toString().trim(),
                    SendBird.ConnectHandler { user, e ->
                        if (e != null) {    // Error.
                            return@ConnectHandler
                        }
                        progress_bar.visibility = View.GONE
                        val intent = Intent(this@LoginActivity, ChatActivity::class.java)
                        intent.putExtra("friendId", etFriendId.text.toString().trim())
                        startActivity(intent)
                    })
            }
            else
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show()
        }
    }
}