package com.ghost.caller.di

import com.ghost.caller.repository.CallLogRepository
import com.ghost.caller.repository.ContactRepository
import com.ghost.caller.viewmodel.call.CallViewModel
import com.ghost.caller.viewmodel.contact.ContactViewModel
import com.ghost.caller.viewmodel.contact.add.AddEditContactViewModel
import com.ghost.caller.viewmodel.recent.CallLogViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val repositoryModule = module {
    single<CallLogRepository> { CallLogRepository(context = get()) }
    single<ContactRepository> { ContactRepository(context = get()) }
}

val viewModel = module {
    factory<CallLogViewModel> {
        CallLogViewModel(
            application = androidApplication(),
            callLogRepository = get()
        )
    }
    factory<ContactViewModel> {
        ContactViewModel(
            application = androidApplication(),
            contactRepository = get()
        )
    }
    viewModel { (contactId: String?, name: String?) ->
        AddEditContactViewModel(
            application = get(),
            contactRepository = get(),
            contactId = contactId,
            name = name
        )
    }
    factory<CallViewModel> {
        CallViewModel(
            application = androidApplication(),
            contactRepository = get()
        )
    }
}

val appModule = module {
    includes(repositoryModule, viewModel)
}