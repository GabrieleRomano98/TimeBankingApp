package com.example.timebankingapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import java.util.*

class CreateTimeslot : Fragment() {

    private val vm:MyViewModel by activityViewModels()

    private val requestImageCapture = 101
    private val requestImageGallery = 102

    private lateinit var title: EditText
    private lateinit var date: TextView
    private lateinit var time: TextView
    private lateinit var hours: EditText
    private lateinit var place: TextView
    private lateinit var description: EditText
    private lateinit var category: TextView
    private lateinit var image: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_timeslot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.timeslot_create_title)
        date = view.findViewById(R.id.timeslot_create_date)
        time = view.findViewById(R.id.timeslot_create_time)
        hours = view.findViewById(R.id.timeslot_create_hours)
        place = view.findViewById(R.id.timeslot_create_location)
        description = view.findViewById(R.id.timeslot_create_description)
        category = view.findViewById(R.id.timeslot_create_category)
        image = view.findViewById(R.id.imageView2)
        restoreData(savedInstanceState)
        vm.initMyViewModel()

        val camera = view.findViewById<ShapeableImageView>(R.id.cameraButton)
        registerForContextMenu(camera)
        camera.setOnClickListener {
            requireActivity().openContextMenu(it)
        }

        registerForContextMenu(category)
        category.setOnClickListener {
            requireActivity().openContextMenu(it)
        }

        initDatePicker()
        initTimePicker()

        val addButton = view.findViewById<FloatingActionButton>(R.id.timeslot_create_saveButton)
        addButton.setOnClickListener{
            if(!popupError()) {
                vm.add(
                    title.text.toString(),
                    description.text.toString(),
                    date.text.toString(),
                    time.text.toString(),
                    hours.text.toString().toInt(),
                    place.text.toString(),
                    category.text.toString(),
                    image.drawable.toBitmap()
                )
                findNavController().popBackStack()
            }
        }
    }

    private fun restoreData(savedInstanceState: Bundle?) {
        val img = savedInstanceState?.getParcelable<Bitmap>("image")
        if(img != null)
            image.setImageBitmap(img)
        var t = savedInstanceState?.getString("time")
        if(t != null)
            time.text = t
        t = savedInstanceState?.getString("date")
        if(t != null)
            date.text = t
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(image.drawable != null)
            outState.putParcelable("image", image.drawable.toBitmap())
        if(time.text != null)
            outState.putString("time", time.text.toString())
        if(time.text != null)
            outState.putString("date", date.text.toString())
    }

    private fun initTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, h, m ->
            val t = "${String.format("%02d", h)}:${String.format("%02d", m)}"
            time.text = t
        }
        val cal = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            requireContext(), AlertDialog.THEME_HOLO_LIGHT, timeSetListener,
            cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE], true
        )
        time.setOnClickListener{
            timePickerDialog.show()
        }
    }

    private fun initDatePicker() {
        val dateSetListener = OnDateSetListener { _, year, month, day ->
            val d = "$day ${getMonthFormat(month)} $year"
            date.text = d
        }
        val cal = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(), AlertDialog.THEME_HOLO_LIGHT, dateSetListener,
            cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]
        )
        date.setOnClickListener{ datePickerDialog.show() }
        datePickerDialog.datePicker.minDate =cal.timeInMillis
    }

    private fun getMonthFormat(month: Int): String {
        return when(month) {
            0 -> "JAN"
            1 -> "FEB"
            2 -> "MAR"
            3 -> "APR"
            4 -> "MAY"
            5 -> "JUN"
            6 -> "JUL"
            7 -> "AUG"
             8 -> "SEP"
            9 -> "OCT"
            10 -> "NOV"
            else -> "DEC"
        }
    }

    private fun popupError(): Boolean {

        val fieldList = listOf(
            title.text.toString(),
            hours.text.toString(),
            place.text.toString(),
            category.text.toString(),
            date.text.toString(),
            description.text.toString()
        )
        if(fieldList.filter{ it == ""}.count() == 0 || image.drawable == null)
            return false

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Warning")
            .setMessage(
                "All the fields are mandatory!\nFill them and add an image before proceeding")
            .setNegativeButton("Close"){ dialog: DialogInterface, _: Int ->
                Toast.makeText(requireContext(),"Canceled", Toast.LENGTH_LONG).show()
                dialog.cancel()
            }
            .create().show()
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        val m = if(v.id == R.id.cameraButton) R.menu.context_menu else R.menu.category_menu
        if(activity != null)
            requireActivity().menuInflater.inflate(m, menu)
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
            else -> {
                category.text = item.title
                true
            }
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

}