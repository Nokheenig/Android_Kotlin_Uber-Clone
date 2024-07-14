package com.example.uberclone

import android.R.attr
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.uberclone.models.DriverInfoModel
import com.example.uberclone.ui.HomeActivity
import com.example.uberclone.utils.Constants
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*



class SplashScreenActivity : AppCompatActivity() {
    companion object {
        private val LOGIN_REQUEST_CODE = 214321
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var getResult: ActivityResultLauncher<Intent>
    private lateinit var progressBar: ProgressBar

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference
    private var fbUser: FirebaseUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("testDebug", "[SSA-onCreate] - onCreate-Start0")
        super.onCreate(savedInstanceState)
        Log.d("testDebug", "[SSA-onCreate] - onCreate-Start")
        setContentView(R.layout.activity_splash_screen)
        Log.d("testDebug", "[SSA-onCreate] - Launching init ...")
        init()

        progressBar = findViewById(R.id.progress_bar)
        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if (result.resultCode == Activity.RESULT_OK){
                // That means everything worked fine
            } else {

            }
        }
        Log.d("testDebug", "[SSA-onCreate] - onCreate-End")
    }

    override fun onStart() {
        Log.d("testDebug", "[SSA-onStart] - onStart-Start0")
        super.onStart()
        Log.d("testDebug", "[SSA-onStart] - onStart-Start")
        Log.d("testDebug", "[SSA-onStart] - Launching displaySplashScreen...")
        displaySplashScreen()
        Log.d("testDebug", "[SSA-onStart] - onStart-End")
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener)
        }
        super.onStop()

    }

    private fun displaySplashScreen(){
        Log.d("testDebug", "[SSA-displaySplashScreen] displaySplashScreen-Start, starting delayed actions...")
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread()).subscribe{
            Log.d("testDebug", "[SSA-displaySplashScreen] ProgressBar has been displayed for 3 seconds, continueing startup sequence: checking if a user is logged in...")
            Log.d("testDebug", "[SSA-displaySplashScreen] Attaching firebaseAuth.addAuthStateListener(listener)...")
            firebaseAuth.addAuthStateListener(listener)
            Log.d("testDebug", "[SSA-displaySplashScreen] Attached? firebaseAuth.addAuthStateListener(listener)...")

            /*
            //Duplicate logic with listener attached above or in init function
            fbUser = FirebaseAuth.getInstance().currentUser
            if (fbUser != null) {
                Log.d("testDebug", "[SSA-displaySplashScreen] FirebaseAuth.AuthStateListener: user is not null: CheckingUserFromFirebase...")
                Log.d("testDebug", "[SSA-displaySplashScreen] Launching checkUserFromFirebase...")
                checkUserFromFirebase()
            } else {
                Log.d("testDebug", "[SSA-displaySplashScreen] FirebaseAuth.AuthStateListener: user is null:")
                Log.d("testDebug", "[SSA-displaySplashScreen] Launching showLoginLayout ...")
                showLoginLayout()
            }
             */
        }
        Log.d("testDebug", "[SSA-displaySplashScreen] displaySplashScreen-End")
    }

    private fun init(){
        Log.d("testDebug", "[SSA-init] init-Start")
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        driverInfoRef = database.getReference(Constants.DRIVER_INFO_REFERENCE)

        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            Log.d("testDebug", "[SSA-init]  FirebaseAuth.AuthStateListener triggered")
            val user = myFirebaseAuth.currentUser
            if (user != null) {
                Log.d("testDebug", "[SSA-init] FirebaseAuth.AuthStateListener: user is not null:")
                Log.d("testDebug", "[SSA-init] Launching checkUserFromFirebase...")
                checkUserFromFirebase()
            } else {
                Log.d("testDebug", "[SSA-init] FirebaseAuth.AuthStateListener: user is null:")
                Log.d("testDebug", "[SSA-init] Launching showLoginLayout ...")
                showLoginLayout()
            }
        }
//        Log.d("testDebug", "[SSA-init] Attaching firebaseAuth.addAuthStateListener(listener)...")
//        firebaseAuth.addAuthStateListener(listener)
//        Log.d("testDebug", "[SSA-init] Attached? firebaseAuth.addAuthStateListener(listener)...")
        Log.d("testDebug", "[SSA-init] init-End")
    }

    private fun showLoginLayout() {
        Log.d("testDebug", "[SSA-showLoginLayout] showLoginLayout-Start")
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
            .setPhoneButtonId(R.id.button_phone_sign_in)
            .setGoogleButtonId(R.id.button_google_sign_in)
            .build()
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setTheme(R.style.LoginTheme)
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()
        Log.d("testDebug", "[SSA-showLoginLayout] Launching sign in intent...")
        getResult.launch(signInIntent)
//        Log.d("testDebug", "[SSA-showLoginLayout] Launching startAuthActivity...")
//        startAuthActivity()
//        Log.d("testDebug", "[SSA-showLoginLayout] after startAuthActivity()")
        Log.d("testDebug", "[SSA-showLoginLayout] fbUser: ${fbUser.toString()}")
        Log.d("testDebug", "[SSA-showLoginLayout] FirebaseAuth.getInstance().currentUser: ${FirebaseAuth.getInstance().currentUser.toString()}")
        Log.d("testDebug", "[SSA-showLoginLayout] showLoginLayout-End2")
    }

    /*
    private fun startAuthActivity() {
        Log.d("testDebug", "[SSA-startAuthActivity] startAuthActivity-Start")
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
            .setPhoneButtonId(R.id.button_phone_sign_in)
            .setGoogleButtonId(R.id.button_google_sign_in)
            .build()
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setTheme(R.style.LoginTheme)
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()
        Log.d("testDebug", "[SSA-startAuthActivity] starting AuthActivity...")
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN);
        Log.d("testDebug", "[SSA-startAuthActivity] AuthActivity started")
        Log.d("testDebug", "[SSA-startAuthActivity] startAuthActivity-End")
    }
     */

    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("testDebug", "[SSA-onActivityResult] onActivityResult-Start")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === Constants.RC_SIGN_IN) {
            Log.d("testDebug", "[SSA-onActivityResult] SIGN IN CODE: ${requestCode}")
            Log.d("testDebug", "[SSA-onActivityResult] RESULT CODE: $resultCode")
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode === RESULT_OK) {
                fbUser = FirebaseAuth.getInstance().currentUser
                Log.d("testDebug", "[SSA-onActivityResult] Sign In success: User is ${fbUser?.getDisplayName()}")
                Toast.makeText(
                    applicationContext,
                    "Sign In Success: User is " + fbUser?.getDisplayName(),
                    Toast.LENGTH_SHORT
                ).show()

//                val loginToDashboardIntent = Intent(
//                    applicationContext,
//                    Dashboard::class.java
//                )
//                startActivity(loginToDashboardIntent)
                Log.d("testDebug", "[SSA-onActivityResult] Launching checkUserFromFirebase...")
                checkUserFromFirebase()
                Log.d("testDebug", "[SSA-onActivityResult] checkUserFromFirebase launched")
            } else {
                if (response == null) {
                    Log.d("testDebug", "[SSA-onActivityResult] response == null : Sign up cancelled")
                    Toast.makeText(applicationContext, "Sign Up Cancelled", Toast.LENGTH_SHORT)
                        .show()
                } else if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    Log.d("testDebug", "[SSA-onActivityResult] response-error : No internet connection")
                    Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d("testDebug", "[SSA-onActivityResult] response : Unknown sign up error")
                    Toast.makeText(applicationContext, "Unknown Sign Up Error", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            //^ if requestCode === Constants.RC_SIGN_IN
            Log.d("testDebug", "[SSA-onActivityResult] Sign in error : requestCode != Constants.RC_SIGN_IN")
            Toast.makeText(applicationContext, "Sign In Error", Toast.LENGTH_SHORT).show()
        }
        Log.d("testDebug", "[SSA-onActivityResult] onActivityResult-End")
    }
     */

    private fun checkUserFromFirebase(){
        Log.d("testDebug", "[SSA-checkUserFromFirebase] checkUserFromFirebase-Start")

        val currentUserUid: String = FirebaseAuth.getInstance().currentUser!!.uid
        Log.d("testDebug", "[SSA-checkUserFromFirebase] FirebaseAuth.getInstance().currentUser!!.uid: $currentUserUid")
        //val dbRef : DatabaseReference = FirebaseDatabase.getInstance().getReference("DriverInfo")
        Log.d("testDebug", "[SSA-checkUserFromFirebase] driverInfoRef: $driverInfoRef")
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        Log.d("testDebug", "[SSA-checkUserFromFirebase] Listener-onDataChange => User already exists.")
                        val model = snapshot.getValue(DriverInfoModel::class.java)
                        goToHomeActivity(model!!)
                    } else {
                        Log.d("testDebug", "[SSA-checkUserFromFirebase] Listener-onDataChange => User doesn't exist yet, showing RegisterLayout ...")
                        Log.d("testDebug", "[SSA-checkUserFromFirebase] Launching showRegisterUserLayout ...")
                        showRegisterUserLayout()
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("testDebug", "[SSA-checkUserFromFirebase] Listener-onCancelled-Error: ${error}")
                    Toast.makeText(this@SplashScreenActivity, error.toString(), Toast.LENGTH_SHORT).show()
                }
            })
        Log.d("testDebug", "[SSA-checkUserFromFirebase] checkUserFromFirebase-End")
    }

    private fun goToHomeActivity(model: DriverInfoModel) {
        Constants.currentUser = model
        startActivity(Intent(this@SplashScreenActivity, HomeActivity::class.java))
        finish()
    }

    private fun showRegisterUserLayout(){
        Log.d("testDebug", "[SSA-showRegisterUserLayout] showRegisterUserLayout-Start")
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.register_layout, null, false)

        val edit_text_name = itemView.findViewById<View>(R.id.edit_text_first_name) as TextInputEditText
        val edit_text_last_name = itemView.findViewById<View>(R.id.edit_text_last_name) as TextInputEditText
        val edit_text_phone_number = itemView.findViewById<View>(R.id.edit_text_phone_number) as TextInputEditText

        val btnContinue = itemView.findViewById<Button>(R.id.button_register)

        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null
            && !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber)){
            edit_text_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        btnContinue.setOnClickListener {
            if(TextUtils.isDigitsOnly(edit_text_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter a First Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edit_text_last_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter a Last Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(TextUtils.isEmpty(edit_text_phone_number.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter a Phone Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                val model = DriverInfoModel(
                    edit_text_name.text.toString(),
                    edit_text_last_name.text.toString(),
                    edit_text_phone_number.text.toString(),
                    0.0
                )
                
                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{
                        Log.d("testDebug", "[SSA-showRegisterUserLayout] RegisterButton-Error: ${it.message}")
                        Toast.makeText(
                            this@SplashScreenActivity, 
                            "${it.message}", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnSuccessListener {
                        Log.d("testDebug", "[SSA-showRegisterUserLayout] RegisterButton: User registered successfully")
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "Registered Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()

                        goToHomeActivity(model)

                        progressBar.visibility = View.GONE
                    }
            }
        }
        Log.d("testDebug", "[SSA-showRegisterUserLayout] showRegisterUserLayout-End")
    }
}