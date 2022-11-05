package com.example.timebankingapp


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class SignUpFragment : Fragment() {

    private lateinit var emailRegister: EditText
    private lateinit var passwordRegister: EditText
    private lateinit var passwordConfirmed: EditText
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        emailRegister = view.findViewById(R.id.emailRegister)
        passwordRegister = view.findViewById(R.id.passwordRegister)
        passwordConfirmed = view.findViewById(R.id.passwordConfirmed)

        view.findViewById<Button>(R.id.createAccountBtn).setOnClickListener{
            createUser()
        }

    }
    private fun notEmpty(): Boolean =
         emailRegister.text.toString().isNotEmpty() &&
         passwordRegister.text.toString().isNotEmpty() &&
         passwordConfirmed.text.toString().isNotEmpty()

    private fun identicalPassword(): Boolean {
        var identical = false
        if (notEmpty() &&
            passwordRegister.text.toString() == passwordConfirmed.text.toString() &&
            passwordRegister.length() > 6
        ) {
            identical = true
        } else if (!notEmpty()) {
                view?.findViewById<TextView>(R.id.matchPassword)?.visibility = View.GONE
                view?.findViewById<TextView>(R.id.shortPassword)?.visibility = View.GONE
                view?.findViewById<TextView>(R.id.emptyFields)?.visibility = View.VISIBLE
        } else if(passwordRegister.text.toString() != passwordConfirmed.text.toString()) {
                view?.findViewById<TextView>(R.id.emptyFields)?.visibility = View.GONE
                view?.findViewById<TextView>(R.id.shortPassword)?.visibility = View.GONE
                view?.findViewById<TextView>(R.id.matchPassword)?.visibility = View.VISIBLE
        } else {
            view?.findViewById<TextView>(R.id.emptyFields)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.matchPassword)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.shortPassword)?.visibility = View.VISIBLE
        }
        return identical
    }

    private fun createUser() {
        val email = emailRegister.text.toString()
        val password = passwordRegister.text.toString()
        if(identicalPassword()){
            val b = bundleOf("email" to email, "password" to password)
            findNavController().navigate(R.id.action_signUpFragment_to_completeRegistration, b)
        }
    }
}

