package com.example.timebankingapp

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Demonstrate Firebase Authentication using a Google ID Token.
 */
class GoogleSignInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var registerBtn: TextView
    private val vm:MyViewModel by activityViewModels()

    // Configure Google Sign In
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("1036630293877-8t5okjhdr3nq69s2i88m255qv01thhh0.apps.googleusercontent.com")
        .requestEmail()
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get off back button
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Initialize Firebase Auth
        auth = Firebase.auth
        vm.initMyViewModel()

        registerBtn = view.findViewById(R.id.RegisterBtn)
        loginEmail = view.findViewById(R.id.emailLogin)
        loginPassword = view.findViewById(R.id.passwordLogin)


        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        view.findViewById<com.google.android.gms.common.SignInButton>(R.id.loginGoogle).setOnClickListener {
            signInGoogle()
        }

        view.findViewById<Button>(R.id.Login).setOnClickListener {
            signIn()
        }
       registerBtn.setOnClickListener {
           findNavController().navigate(R.id.action_googleSignInFragment2_to_signUpFragment)
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, true){
            requireActivity().finish()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                vm.isRegistered(account.email?: ""){
                    if(it) {
                        firebaseAuthWithGoogle(account.idToken!!)
                    }
                    else {
                        val b = bundleOf("googleToken" to account.idToken!!)
                        findNavController().navigate(R.id.action_googleSignInFragment2_to_completeRegistration, b)
                    }
                }
            } catch (e: ApiException) {
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    println( "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    println( "signInWithCredential:failure" + task.exception)
                }
            }
    }

    private fun signInGoogle() {
        //Clear old Google connection
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut()
        //Launch intent
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    private fun signIn() {
        val email = loginEmail.text.toString()
        val password = loginPassword.text.toString()

        if (TextUtils.isEmpty(email)) {
            if(view?.findViewById<TextView>(R.id.Error)?.visibility == View.VISIBLE) {
                view?.findViewById<TextView>(R.id.Error)?.visibility = View.GONE
            }
            view?.findViewById<TextView>(R.id.emptyFields)?.visibility = View.VISIBLE

        } else if (TextUtils.isEmpty(password)) {
            if(view?.findViewById<TextView>(R.id.Error)?.visibility == View.VISIBLE) {
                view?.findViewById<TextView>(R.id.Error)?.visibility = View.GONE
            }
            view?.findViewById<TextView>(R.id.emptyFields)?.visibility = View.VISIBLE
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        println("signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        if(view?.findViewById<TextView>(R.id.emptyFields)?.visibility == View.VISIBLE) {
                            view?.findViewById<TextView>(R.id.emptyFields)?.visibility = View.GONE
                        }
                        view?.findViewById<TextView>(R.id.Error)?.visibility = View.VISIBLE
                        // If sign in fails, display a message to the user.
                        println("signInWithEmail:failure" + task.exception)
                    }
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user!= null) {
            vm.initMyViewModel()
            findNavController().popBackStack()
        }
    }

    companion object {
        private const val RC_SIGN_IN = 1001
    }
}