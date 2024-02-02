package com.reymon.marvelapp.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast

import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.Snackbar
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.reymon.marvelapp.R
import com.reymon.marvelapp.databinding.ActivityMainBinding
import com.reymon.marvelapp.ui.viewmodels.MainViewModel
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    //Permite ejecutar aplicaciones en segundo plano por hilos
    private lateinit var executor: Executor

    //Permite manejar los eventos del biometrico
    private lateinit var biometricPrompt: BiometricPrompt

    //Cuadro de dialogo en pantalla
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        initListener()
        initObservables()
        AuthenticationDialog()
        //mainViewModel.checkBiometric(this)
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.imgLogo.visibility=View.VISIBLE
            binding.txtUser.visibility=View.GONE
            binding.txtPassword.visibility=View.GONE





        } else {
            binding.imgLogo.visibility = View.GONE
            binding.txtDontHaveAccount.text = "No user"

        }
    }
//Crear una pantalla para el login y signup
    private fun initListener() {

        binding.imgLogo.setOnClickListener {

            biometricPrompt.authenticate(promptInfo)
        }
        binding.btnIngresaUsuario.setOnClickListener{
            createNewUsers(binding.txtUser.text.toString(),binding.txtPassword.text.toString())
        }
        binding.btnLogIn.setOnClickListener {
            signInUsers(binding.txtUser.text.toString(), binding.txtPassword.text.toString())
        }
    }

    private fun AuthenticationDialog() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    startActivity(Intent(this@MainActivity, MarvelActivity::class.java))
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e("FingerPrint", "Authentication failed!")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("FingerPrint", "Authentication error!")
                }
            })
        promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            //.setNegativeButtonText("Use account password")
            //DEfino si quiero usar la huella o la credencial del dispositivo para ingresar
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            //Con esta línea defino el texto para cancelar la operación porque solo puedo ingresar con huella
            //SOLO PODEMOS EJECUTAR CUANDO PERMITIMOS EL BIOMETRIC STRONG O CREDENTIAL
            .setNegativeButtonText("Cancel").build()


    }

    private fun initObservables() {
        mainViewModel.resultCheckBiometric.observe(this) { code ->
            when (code) {
                BiometricManager.BIOMETRIC_SUCCESS -> {

                    Snackbar.make(
                        this,
                        binding.txtSignUp,
                        "Log In successfully!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    Log.d("MY_APP_TAG", "App can authenticate using biometrics.")

                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Snackbar.make(
                        this,
                        binding.txtSignUp,
                        "Your device don't have fingerprint sensor!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    Log.e("MY_APP_TAG", "No biometric features available on this device.")
                }

                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Snackbar.make(
                        this,
                        binding.txtSignUp,
                        "Fingerprint sensor no available!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Snackbar.make(this, binding.txtSignUp, "Error!", Snackbar.LENGTH_LONG).show()
                    // Prompts the user to create credentials that your app accepts.
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    }
                    startActivityForResult(enrollIntent, 100)
                }


            }
        }
    }

    private fun createNewUsers(user:String, password:String) {
        auth.createUserWithEmailAndPassword(
           user,password
        )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "createUserWithEmail:success")
                    val user = auth.currentUser
                    Snackbar.make(
                        this, binding.txtUser, "createUsersWithEmail:success!",
                        Snackbar.LENGTH_LONG
                    ).show()

                    binding.txtUser.text.clear()

                } else {

                    Snackbar.make(
                        this, binding.txtUser,
                        task.exception!!.message.toString(),
                        Snackbar.LENGTH_LONG
                    ).show()
                    Log.d("TAG", task.exception!!.stackTraceToString())
                }
            }
    }

    private fun signInUsers(email:String, password: String){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
             //Con este user puedo hacer lo que desee: enviar a una api, db, etc
                    val user = auth.currentUser
                    startActivity(Intent(this,MarvelActivity::class.java))
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Snackbar.make(
                        this,
                        binding.txtUser,
                        "signInWithEmail:failure",
                        Snackbar.LENGTH_SHORT,
                    ).show()

                }
            }
    }
}