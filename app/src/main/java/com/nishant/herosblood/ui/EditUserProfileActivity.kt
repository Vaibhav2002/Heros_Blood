package com.nishant.herosblood.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.google.firebase.storage.FirebaseStorage
import com.nishant.herosblood.R
import com.nishant.herosblood.data.UserData
import com.nishant.herosblood.databinding.ActivityEditUserProfileBinding
import com.nishant.herosblood.util.Resource
import com.nishant.herosblood.viewmodels.DataViewModel
import com.theartofdev.edmodo.cropper.CropImage

class EditUserProfileActivity : AppCompatActivity() {

    private val cropActivityResultContract = object : ActivityResultContract<Any?, Uri?>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .setAspectRatio(4, 4)
                .getIntent(this@EditUserProfileActivity)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }

    }
    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Any?>
    private var user: UserData = UserData()
    private lateinit var dataViewModel: DataViewModel
    private lateinit var binding: ActivityEditUserProfileBinding
    private lateinit var animation: AnimationDrawable
    private var isProfilePictureUpdated = false
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_user_profile)
        dataViewModel = ViewModelProvider(this).get(DataViewModel::class.java)
        animation = binding.progressBar.drawable as AnimationDrawable

        user = intent.getSerializableExtra("UserData") as UserData
        binding.currentUser = user
        binding.imgProfilePicture.load(user.profilePictureUrl) {
            this.placeholder(R.drawable.profile_none)
        }

        cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract) {
            it?.let { uri ->
                binding.imgProfilePicture.setImageURI(uri)
                isProfilePictureUpdated = true
                photoUri = uri
            }
        }

        binding.changeProfilePicture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            } else {
                cropActivityResultLauncher.launch(null)
            }
        }

        binding.btnSave.setOnClickListener {

            if (isProfilePictureUpdated) {
                dataViewModel.uploadProfilePicture(user.userId!!, photoUri!!)
            } else if (isDataChange()) {
                user.apply {
                    this.name = binding.etName.text.toString()
                    this.fullAddress = binding.etAddress.text.toString()
                    this.email = binding.etEmail.text.toString()
                    this.phoneNumber = binding.etPhone.text.toString()
                }
                dataViewModel.saveUserData(user)
            }
        }

        dataViewModel.getProfilePictureStatus.observe(this, { response ->
            when (response) {
                is Resource.Loading -> {
                    showLoadingBar()
                }
                is Resource.Success -> {
                    hideLoadingBar()
                    FirebaseStorage.getInstance().reference.child("ProfilePicture")
                        .child(user.userId!!).downloadUrl.addOnSuccessListener { uri ->
                            updateUserData(uri)
                        }
                }
                is Resource.Error -> {
                    hideLoadingBar()
                    Toast.makeText(
                        this,
                        "Check Internet Connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
        dataViewModel.saveUserDataStatus.observe(this, { response ->
            when (response) {
                is Resource.Loading -> {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                    showLoadingBar()
                }
                is Resource.Success -> {
                    if (response.data == true) {
                        hideLoadingBar()
                        startActivity(Intent(this, UserProfileActivity::class.java))
                        finish()
                    } else {
                        hideLoadingBar()
                        Toast.makeText(
                            this,
                            "Check Internet Connection",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                is Resource.Error -> {
                    hideLoadingBar()
                    Toast.makeText(
                        this,
                        "Check Internet Connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun updateUserData(uri: Uri) {
        user.apply {
            this.name = binding.etName.text.toString()
            this.fullAddress = binding.etAddress.text.toString()
            this.email = binding.etEmail.text.toString()
            this.phoneNumber = binding.etPhone.text.toString()
            this.profilePictureUrl = uri.toString()
        }
        dataViewModel.saveUserData(user)
    }

    private fun isDataChange(): Boolean {
        if (binding.etName.text.toString() == user.name && binding.etAddress.text.toString() == user.fullAddress && binding.etEmail.text.toString() == user.email && binding.etPhone.text.toString() == user.phoneNumber) {
            return false
        }
        return true
    }

    private fun showLoadingBar() {
        binding.layoutBackground.alpha = 0.1F
        binding.progressBar.visibility = View.VISIBLE
        animation.start()
    }

    private fun hideLoadingBar() {
        binding.layoutBackground.alpha = 1F
        binding.progressBar.visibility = View.GONE
        animation.stop()
    }
}