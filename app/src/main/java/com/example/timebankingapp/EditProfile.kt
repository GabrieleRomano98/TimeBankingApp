package com.example.timebankingapp

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView


class EditProfile : Fragment() {
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
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

        if(savedInstanceState != null) {
            restoreData(savedInstanceState)
            vm.getProfileImage().observe(viewLifecycleOwner){
                view.findViewById<ShapeableImageView>(R.id.profileImage).setImageBitmap(it)
            }
        }
        else vm.userAuth.observe(viewLifecycleOwner) {
            if(it != null) { // if there is no profile data static information is used
                view.findViewById<EditText>(R.id.name).setText(it.name)
                view.findViewById<EditText>(R.id.surname).setText(it.surname)
                view.findViewById<EditText>(R.id.username).setText(it.username)
                view.findViewById<EditText>(R.id.location).setText(it.location)
                view.findViewById<EditText>(R.id.description).setText(it.description)
                val cg = chipList
                cg.removeAllViews()
                it.skills.forEach { t-> addChip(t) }
            }
            vm.getProfileImage().observe(viewLifecycleOwner){ img ->
                view.findViewById<ShapeableImageView>(R.id.profileImage).setImageBitmap(img)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, true){
            if(!popupError()) {
                val skills = mutableListOf<String>()
                chipList.children.forEach {
                    skills.add((it as Chip).text.toString())
                }
                vm.updateProfile(
                    surname.text.toString(),
                    name.text.toString(),
                    username.text.toString(),
                    location.text.toString(),
                    description.text.toString(),
                    skills
                )
                findNavController().popBackStack()
            }
        }
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

    private fun restoreData(savedInstanceState: Bundle) {
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
        outState.putString("name", name.text.toString())
        outState.putString("surname", surname.text.toString())
        outState.putString("username", username.text.toString())
        outState.putString("location", location.text.toString())
        outState.putString("description", description.text.toString())
        val skills = arrayListOf<String>()
        chipList.forEach { skills.add((it as Chip).text.toString()) }
        outState.putStringArrayList("skills",skills)
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
            vm.updateProfileImage(imageBitmap)
        }
        else if(requestCode == requestImageGallery && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, data.data)
            image.setImageBitmap(imageBitmap)
            vm.updateProfileImage(imageBitmap)
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
        if(fieldList.filter{ it == ""}.count() == 0)
            return false

        AlertDialog.Builder(requireContext())
            .setTitle("Warning")
            .setMessage("Some mandatory fields are empty!\nFill them before proceeding")
            .setNegativeButton("Close"){ dialog: DialogInterface, _: Int ->
                Toast.makeText(requireContext(),"Canceled",Toast.LENGTH_LONG).show()
                dialog.cancel()
            }
            .create().show()
        return true
    }

}