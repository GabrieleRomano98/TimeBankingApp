package com.example.timebankingapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.imageview.ShapeableImageView
import java.util.*

class TimeslotDetails_Edit : Fragment() {

    private val vm:MyViewModel by activityViewModels()

    private val requestImageCapture = 101
    private val requestImageGallery = 102

    private lateinit var title: EditText
    private lateinit var date: TextView
    private lateinit var time: TextView
    private lateinit var hours: TextView
    private lateinit var place: EditText
    private lateinit var description: EditText
    private lateinit var category: TextView
    private lateinit var image: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_timeslot_details__edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.timeslot_details_edit_title)
        date = view.findViewById(R.id.timeslot_details_edit_date)
        time = view.findViewById(R.id.timeslot_details_edit_time)
        hours = view.findViewById(R.id.timeslot_details_edit_hours)
        place = view.findViewById(R.id.timeslot_details_edit_location)
        description = view.findViewById(R.id.timeslot_details_edit_description)
        category = view.findViewById(R.id.timeslot_details_edit_category)
        image = view.findViewById(R.id.imageView2)
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

        if(savedInstanceState != null) restoreData(savedInstanceState)
        else vm.myTimeslots.observe(viewLifecycleOwner){list->
            val ts = list.find{ it.id == requireArguments().getString("id")}?: TimeslotModel()
            title.setText(ts.title)
            date.text = ts.date
            time.text = ts.time
            place.setText(ts.place)
            hours.text = ts.hours.toString()
            description.setText(ts.description)
            category.text = ts.category
            vm.getTimeslotImage(ts.id).observe(viewLifecycleOwner) {
                image.setImageBitmap(it)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, true){
            if(!popupError()) {
                vm.update(
                    arguments?.getString("id")!!,
                    title.text.toString(),
                    hours.text.toString().toInt(),
                    place.text.toString(),
                    time.text.toString(),
                    date.text.toString(),
                    description.text.toString(),
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
        time.text = savedInstanceState?.getString("time", "")
        date.text = savedInstanceState?.getString("date", "")
        category.text = savedInstanceState?.getString("category", "")
        hours.text = savedInstanceState?.getString("hours", "")
        place.setText(savedInstanceState?.getString("place", ""))
        description.setText(savedInstanceState?.getString("description", ""))
        title.setText(savedInstanceState?.getString("title", ""))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(image.drawable != null)
            outState.putParcelable("image", image.drawable.toBitmap())
        outState.putString("time", time.text.toString())
        outState.putString("date", date.text.toString())
        outState.putString("category", category.text.toString())
        outState.putString("hours", hours.text.toString())
        outState.putString("place", place.text.toString())
        outState.putString("description", description.text.toString())
        outState.putString("title", title.text.toString())
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
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
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
        if (month == 0) return "JAN"
        if (month == 1) return "FEB"
        if (month == 2) return "MAR"
        if (month == 3) return "APR"
        if (month == 4) return "MAY"
        if (month == 5) return "JUN"
        if (month == 6) return "JUL"
        if (month == 7) return "AUG"
        if (month == 8) return "SEP"
        if (month == 9) return "OCT"
        if (month == 10) return "NOV"
        return "DEC"
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