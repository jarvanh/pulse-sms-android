package xyz.klinker.messenger.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.iid.FirebaseInstanceId
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.entity.LoginResponse
import xyz.klinker.messenger.api.implementation.AccountEncryptionCreator
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.LoginActivity
import java.io.IOException
import java.util.*

class ActivateWearActivity : Activity() {

    private val codeText: TextView by lazy { findViewById<View>(R.id.code) as TextView }
    private val code: String by lazy { generateActivationCode() }

    private val api = ApiUtils.api
    private val handler: Handler by lazy { Handler() }
    private var attempts = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_activate)
        codeText.text = code

        queryEndpoint()
    }

    private fun queryEndpoint() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            Thread {
                Log.v(TAG, "checking activate response")

                val response: LoginResponse? = try {
                    api.activate().check(code).execute().body()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }

                Log.v(TAG, "finished checking activation")

                if (response == null) {
                    Log.v(TAG, "not activated")
                    if (attempts < RETRY_ATTEMPTS) {
                        attempts++
                        queryEndpoint()
                    } else {
                        runOnUiThread {
                            setResult(RESULT_FAILED)
                            finish()
                        }
                    }
                } else {
                    Log.v(TAG, "activated")
                    runOnUiThread { activated(response) }
                }
            }.start()
        }, RETRY_INTERVAL.toLong())
    }

    private fun activated(response: LoginResponse) {
        findViewById<View>(R.id.waiting_to_activate).visibility = View.GONE
        findViewById<View>(R.id.password_confirmation).visibility = View.VISIBLE
        val password = findViewById<View>(R.id.password) as EditText

        password.text = null
        password.requestFocus()

        findViewById<View>(R.id.confirm).setOnClickListener {
            Toast.makeText(this, R.string.verifying_password, Toast.LENGTH_SHORT).show()
            checkPassword(response, password.text.toString())
        }
    }

    private fun checkPassword(response: LoginResponse, password: String?) {
        if (password == null || password.isEmpty()) {
            Toast.makeText(this, R.string.api_no_password, Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val encryptionCreator = AccountEncryptionCreator(this@ActivateWearActivity, password)
            val utils = encryptionCreator.createAccountEncryptionFromLogin(response)

            try {
                val bodies = api.conversation().list(response.accountId).execute().body()
                if (bodies!!.isNotEmpty()) {
                    val decrypted = utils.decrypt(bodies[0].title)
                    if (decrypted!!.isEmpty()) {
                        throw IllegalStateException("failed the decryption. Account password is incorrect.")
                    }
                } else {
                    val contacts = api.contact().list(response.accountId).execute().body()
                    if (contacts!!.isNotEmpty()) {
                        val decrypted = utils.decrypt(contacts[0].name)
                        if (decrypted!!.isEmpty()) {
                            throw IllegalStateException("failed the decryption. Account password is incorrect.")
                        }
                    }
                }

                ApiUtils.registerDevice(response.accountId,
                        Build.MANUFACTURER + ", " + Build.MODEL, Build.MODEL,
                        false, FirebaseInstanceId.getInstance().token)

                setResult(LoginActivity.RESULT_START_NETWORK_SYNC)
                finish()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ActivateWearActivity, xyz.klinker.messenger.api.implementation.R.string.api_wrong_password,
                            Toast.LENGTH_LONG).show()
                    activated(response)
                }
            }
        }.start()
    }

    companion object {

        private val TAG = "ActivateWearActivity"

        /**
         * Retry the activation request every 5 seconds.
         */
        private val RETRY_INTERVAL = 5000

        /**
         * Number of times to try getting the activation details before giving up.
         * 5 seconds * 60 = 5 minutes.
         */
        private val RETRY_ATTEMPTS = 60

        val RESULT_FAILED = 6666

        private fun generateActivationCode(): String {
            val r = Random()
            val sb = StringBuilder()
            while (sb.length < 8) {
                sb.append(Integer.toHexString(r.nextInt()))
            }

            return sb.toString().substring(0, 8).toUpperCase(Locale.getDefault())
        }
    }
}