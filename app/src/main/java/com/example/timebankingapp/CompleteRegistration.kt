package com.example.timebankingapp

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class CompleteRegistration : Fragment() {
    private val vm:MyViewModel by activityViewModels()

    private val requestImageCapture = 101
    private val requestImageGallery = 102

    private lateinit var name: EditText
    private lateinit var surname: EditText
    private lateinit var username: EditText
    private lateinit var location: EditText
    private lateinit var chipList: ChipGroup
    private lateinit var description: EditText
    private lateinit var skill: EditText
    private lateinit var image: ShapeableImageView

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_complete_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        name = view.findViewById(R.id.name)
        surname = view.findViewById(R.id.surname)
        username = view.findViewById(R.id.username)
        location = view.findViewById(R.id.location)
        description = view.findViewById(R.id.description)
        chipList = view.findViewById(R.id.skills)
        skill = view.findViewById(R.id.skill)
        image = view.findViewById(R.id.profileImage)

        auth = Firebase.auth
        if(savedInstanceState != null)
            restoreData(savedInstanceState)

        registerForContextMenu(view)
        view.findViewById<ShapeableImageView>(R.id.cameraButton).setOnClickListener {
            requireActivity().openContextMenu(it)
        }

        view.findViewById<Button>(R.id.chip_button).setOnClickListener {
            if (skill.text.toString().isNotEmpty()) {
                addChip(skill.text.toString())
                skill.setText("")
            }
        }

        view.findViewById<Button>(R.id.buttonComplete).setOnClickListener {
            if(!popupError()) {
                val googleToken = requireArguments().getString("googleToken")
                if(googleToken != null)
                    firebaseAuthWithGoogle(googleToken)
                else {
                    auth.createUserWithEmailAndPassword(
                        requireArguments().getString("email")!!,
                        requireArguments().getString("password")!!
                    ).addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            // Sign up success, end registration
                            endRegistration()
                            findNavController().popBackStack()
                        } else {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Warning")
                                .setMessage(task.exception?.message)
                                .setNegativeButton("Close"){ dialog: DialogInterface, _: Int ->
                                    Toast.makeText(requireContext(),"Canceled", Toast.LENGTH_LONG).show()
                                    dialog.cancel()
                                }
                                .create().show()
                            // If sign in fails, display a message to the user.
                            println("RegisterWithEmail:failure" + task.exception)
                        }
                    }
                }
            }
        }
    }

    private fun restoreData(savedInstanceState: Bundle) {
        val img = savedInstanceState.getParcelable<Bitmap>("image")
        if(img != null)
            image.setImageBitmap(img)
        name.setText(savedInstanceState.getString("name", ""))
        surname.setText(savedInstanceState.getString("surname", ""))
        username.setText(savedInstanceState.getString("username", ""))
        location.setText(savedInstanceState.getString("location", ""))
        description.setText(savedInstanceState.getString("description", ""))
        val skills = savedInstanceState.getStringArrayList("skills")
        skills?.forEach {
            addChip(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(image.drawable != null)
            outState.putParcelable("image", image.drawable.toBitmap())
        outState.putString("name", name.text.toString())
        outState.putString("surname", surname.text.toString())
        outState.putString("username", username.text.toString())
        outState.putString("location", location.text.toString())
        outState.putString("description", description.text.toString())
        val skills = arrayListOf<String>()
        chipList.forEach { skills.add((it as Chip).text.toString()) }
        outState.putStringArrayList("skills",skills)
    }

    private fun endRegistration() {
        val skills = mutableListOf<String>()
        chipList.children.forEach {
            skills.add((it as Chip).text.toString())
        }
        vm.addProfile(
            surname.text.toString(),
            name.text.toString(),
            username.text.toString(),
            location.text.toString(),
            description.text.toString(),
            skills,
            0
        )
        vm.updateProfileImage(image.drawable.toBitmap())
        vm.initMyViewModel()
        findNavController().popBackStack()
        findNavController().popBackStack()
    }

    private fun addChip(chipText: String){
        val chip = Chip(context)
        chip.text = chipText
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipList.removeView(chip)
        }
        chipList.addView(chip)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if(activity != null)
            requireActivity().menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.camera -> {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePictureIntent, requestImageCapture)
                true
            }
            R.id.gallery -> {
                val takeGalleryIntent = Intent()
                takeGalleryIntent.type = "image/*"
                takeGalleryIntent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(takeGalleryIntent, requestImageGallery)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestImageCapture && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val imageBitmap = data.extras?.get("data") as Bitmap
            image.setImageBitmap(imageBitmap)
        }
        else if(requestCode == requestImageGallery && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, data.data)
            image.setImageBitmap(imageBitmap)
        }
    }

    private fun popupError(): Boolean {

        val fieldList = listOf(
            name.text.toString(),
            surname.text.toString(),
            username.text.toString(),
            location.text.toString(),
            description.text.toString()
        )
        if(fieldList.none{ it == "" } && image.drawable != null)
            return false


        AlertDialog.Builder(requireContext())
            .setTitle("Warning")
            .setMessage("Some mandatory fields are empty!\nFill them before proceeding")
            .setNegativeButton("Close"){ dialog: DialogInterface, _: Int ->
                Toast.makeText(requireContext(),"Canceled", Toast.LENGTH_LONG).show()
                dialog.cancel()
            }
            .create().show()
        return true
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    endRegistration()
                } else {
                    // If sign in fails, display a message to the user.
                    println( "signInWithCredential:failure" + task.exception)
                }
            }
    }

}