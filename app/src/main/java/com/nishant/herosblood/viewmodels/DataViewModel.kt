package com.nishant.herosblood.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nishant.herosblood.models.UserData
import com.nishant.herosblood.repositories.DataRepository
import com.nishant.herosblood.util.DifferentDonorLists
import com.nishant.herosblood.util.Resource
import kotlinx.coroutines.launch

class DataViewModel(
    private val dataRepository: DataRepository = DataRepository()
) : ViewModel() {

    private val _saveUserDataStatus: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    val saveUserDataStatus: LiveData<Resource<Boolean>> = _saveUserDataStatus
    fun saveUserData(user: UserData) {
        _saveUserDataStatus.postValue(Resource.Loading())
        dataRepository.saveUserData(user, { task ->
            if (task.isSuccessful) {
                _saveUserDataStatus.postValue(Resource.Success(true))
            } else {
                _saveUserDataStatus.postValue(Resource.Success(false))
            }
        }, {
            _saveUserDataStatus.postValue(Resource.Error(it.message.toString()))
        })
    }

    private val _readUserDataStatus: MutableLiveData<Resource<UserData>> = MutableLiveData()
    val readUserDataStatus: LiveData<Resource<UserData>> = _readUserDataStatus
    fun readUserData(
        userId: String
    ) {
        _readUserDataStatus.postValue(Resource.Loading())
        dataRepository.readUserData(userId, { document ->
            if (document.exists()) {
                val user = document.toObject(UserData::class.java)
                user?.let {
                    _readUserDataStatus.postValue(Resource.Success(user))
                }
            } else {
                _readUserDataStatus.postValue(Resource.Success(UserData()))
            }
        }, {
            _readUserDataStatus.postValue(Resource.Error(it.message.toString()))
        })
    }

    private val _getAllDonorsStatus: MutableLiveData<Resource<DifferentDonorLists>> =
        MutableLiveData()
    val getAllDonorsStatus: LiveData<Resource<DifferentDonorLists>> = _getAllDonorsStatus
    fun getAllDonors(currentUserId: String) = viewModelScope.launch {
        _getAllDonorsStatus.postValue(Resource.Loading())
        val donorsList = DifferentDonorLists()

        dataRepository.getAllDonors(currentUserId, {
            for (document in it) {
                val user = document.toObject(UserData::class.java)
                if (user.bloodGroup.equals("O+")) {
                    donorsList.oPositiveDonors.add(user)
                }
                if (user.bloodGroup.equals("A+")) {
                    donorsList.aPositiveDonors.add(user)
                }
                if (user.bloodGroup.equals("O-")) {
                    donorsList.oNegativeDonors.add(user)
                }
                if (user.bloodGroup.equals("AB+")) {
                    donorsList.abPositiveDonors.add(user)
                }
                if (user.bloodGroup.equals("AB-")) {
                    donorsList.abNegativeDonors.add(user)
                }
            }
            _getAllDonorsStatus.postValue(Resource.Success(donorsList))
        }, {
            _getAllDonorsStatus.postValue(Resource.Error(it.message.toString()))
        })
    }

    private val _getDonorListStatus: MutableLiveData<Resource<ArrayList<UserData>>> =
        MutableLiveData()
    val getDonorListStatus: LiveData<Resource<ArrayList<UserData>>> = _getDonorListStatus
    fun getDonorList(
        userId: String,
        bloodType: String
    ) = viewModelScope.launch {
        val donorList: ArrayList<UserData> = ArrayList()
        _getDonorListStatus.postValue(Resource.Loading())
        dataRepository.getDonorList(userId, bloodType, { snapshot ->
            if (!snapshot.isEmpty) {
                for (donors in snapshot) {
                    val donor: UserData = donors.toObject(UserData::class.java)
                    donorList.add(donor)
                }
                _getDonorListStatus.postValue(Resource.Success(donorList))
            } else {
                _getDonorListStatus.postValue(Resource.Success(donorList))
            }
        }, {
            Log.d("Error: ", it.message.toString())
            _getDonorListStatus.postValue(Resource.Error("Some Error Occurred"))
        })
    }

    private val _getProfilePictureStatus: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    val getProfilePictureStatus: LiveData<Resource<Boolean>> = _getProfilePictureStatus
    fun uploadProfilePicture(
        userId: String,
        file: Uri
    ) = viewModelScope.launch {
        _getProfilePictureStatus.postValue(Resource.Loading())
        dataRepository.uploadProfilePicture(userId, file, { task ->
            if (task.isSuccessful) {
                _getProfilePictureStatus.postValue(Resource.Success(true))
            } else {
                _getProfilePictureStatus.postValue(Resource.Success(false))
            }
        }, { exception ->
            _getProfilePictureStatus.postValue(Resource.Error(exception.message.toString()))
        })
    }
}