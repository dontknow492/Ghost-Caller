package com.ghost.caller.viewmodel.contact

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ghost.caller.repository.ContactRepository

class ContactViewModelFactory(
    private val application: Application,
    private val contactRepository: ContactRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactViewModel(application, contactRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}