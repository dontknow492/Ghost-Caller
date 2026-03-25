@file:Suppress("D")

package com.ghost.caller.ui.screens.contact

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ghost.caller.models.AddressType
import com.ghost.caller.models.EmailType
import com.ghost.caller.models.EventType
import com.ghost.caller.models.PhoneNumberType
import com.ghost.caller.viewmodel.contact.add.AddEditContactEvent
import com.ghost.caller.viewmodel.contact.add.AddEditContactSideEffect
import com.ghost.caller.viewmodel.contact.add.AddEditContactViewModel
import com.ghost.caller.viewmodel.contact.add.ContactSection
import com.ghost.caller.viewmodel.contact.add.EditableAddress
import com.ghost.caller.viewmodel.contact.add.EditableEmail
import com.ghost.caller.viewmodel.contact.add.EditableEvent
import com.ghost.caller.viewmodel.contact.add.EditablePhoneNumber
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    contactId: String? = null,
    onNavigateBack: (String?) -> Unit,
    viewModel: AddEditContactViewModel,
) {


    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    rememberScrollState()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendEvent(AddEditContactEvent.UpdatePhoto(it)) }
    }

    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is AddEditContactSideEffect.ShowToast -> {
                    android.widget.Toast.makeText(
                        context,
                        effect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                is AddEditContactSideEffect.ShowError -> {
                    android.widget.Toast.makeText(
                        context,
                        effect.message,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                is AddEditContactSideEffect.NavigateBack -> {
                    onNavigateBack(effect.contactId)
                }

                is AddEditContactSideEffect.ShowImagePicker -> {
                    imagePickerLauncher.launch("image/*")
                }

                is AddEditContactSideEffect.ShowDatePicker -> {
                    selectedEventId = effect.eventId
                    showDatePicker = true
                }

                is AddEditContactSideEffect.ShowDeleteConfirmation -> {
                    // Show delete confirmation
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker && selectedEventId != null) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.sendEvent(AddEditContactEvent.UpdateEventDate(selectedEventId!!, date))
                showDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditMode) "Edit Contact" else "Add Contact")
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.sendEvent(AddEditContactEvent.Cancel) }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.sendEvent(AddEditContactEvent.SaveContact) },
                        enabled = state.saveEnabled
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.saveEnabled && !state.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.sendEvent(AddEditContactEvent.SaveContact) }
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Save")
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic Info Section
                item {
                    BasicInfoSection(
                        displayName = state.displayName,
                        displayNameError = state.displayNameError,
                        photoUri = state.photoUri,
                        isStarred = state.isStarred,
                        onDisplayNameChange = {
                            viewModel.sendEvent(AddEditContactEvent.UpdateDisplayName(it))
                        },
                        onPhotoClick = { viewModel.showImagePicker() },
                        onStarClick = {
                            viewModel.sendEvent(AddEditContactEvent.ToggleStarred(!state.isStarred))
                        }
                    )
                }

                // Phone Numbers Section
                item {
                    ExpandableSection(
                        title = "Phone Numbers",
                        icon = Icons.Default.Phone,
                        isExpanded = state.expandedSections.contains(ContactSection.PHONE_NUMBERS),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.PHONE_NUMBERS,
                                    expanded
                                )
                            )
                        }
                    ) {
                        PhoneNumbersSection(
                            phoneNumbers = state.phoneNumbers,
                            onAddPhone = { viewModel.sendEvent(AddEditContactEvent.AddPhoneNumber) },
                            onRemovePhone = { id ->
                                viewModel.sendEvent(AddEditContactEvent.RemovePhoneNumber(id))
                            },
                            onPhoneNumberChange = { id, number ->
                                viewModel.sendEvent(
                                    AddEditContactEvent.UpdatePhoneNumber(
                                        id,
                                        number
                                    )
                                )
                            },
                            onPhoneTypeChange = { id, type ->
                                viewModel.sendEvent(
                                    AddEditContactEvent.UpdatePhoneNumberType(
                                        id,
                                        type
                                    )
                                )
                            },
                            onPhoneLabelChange = { id, label ->
                                viewModel.sendEvent(
                                    AddEditContactEvent.UpdatePhoneNumberLabel(
                                        id,
                                        label
                                    )
                                )
                            },
                            onSetPrimary = { id ->
                                viewModel.sendEvent(AddEditContactEvent.SetPrimaryPhoneNumber(id))
                            }
                        )
                        if (state.phoneNumbersError != null) {
                            Text(
                                text = state.phoneNumbersError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Email Addresses Section
                item {
                    ExpandableSection(
                        title = "Email Addresses",
                        icon = Icons.Default.Email,
                        isExpanded = state.expandedSections.contains(ContactSection.EMAIL_ADDRESSES),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.EMAIL_ADDRESSES,
                                    expanded
                                )
                            )
                        }
                    ) {
                        EmailsSection(
                            emails = state.emails,
                            onAddEmail = { viewModel.sendEvent(AddEditContactEvent.AddEmail) },
                            onRemoveEmail = { id ->
                                viewModel.sendEvent(AddEditContactEvent.RemoveEmail(id))
                            },
                            onEmailChange = { id, email ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateEmail(id, email))
                            },
                            onEmailTypeChange = { id, type ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateEmailType(id, type))
                            },
                            onEmailLabelChange = { id, label ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateEmailLabel(id, label))
                            },
                            onSetPrimary = { id ->
                                viewModel.sendEvent(AddEditContactEvent.SetPrimaryEmail(id))
                            }
                        )
                        if (state.emailsError != null) {
                            Text(
                                text = state.emailsError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Organization Section
                item {
                    ExpandableSection(
                        title = "Organization",
                        icon = Icons.Default.Business,
                        isExpanded = state.expandedSections.contains(ContactSection.ORGANIZATION),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.ORGANIZATION,
                                    expanded
                                )
                            )
                        }
                    ) {
                        OrganizationSection(
                            organization = state.organization,
                            jobTitle = state.jobTitle,
                            department = state.department,
                            onOrganizationChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateOrganization(it))
                            },
                            onJobTitleChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateJobTitle(it))
                            },
                            onDepartmentChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateDepartment(it))
                            }
                        )
                    }
                }

                // Address Section
                item {
                    ExpandableSection(
                        title = "Address",
                        icon = Icons.Default.LocationOn,
                        isExpanded = state.expandedSections.contains(ContactSection.ADDRESS),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.ADDRESS,
                                    expanded
                                )
                            )
                        }
                    ) {
                        AddressSection(
                            address = state.address,
                            onStreetChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateAddressStreet(it))
                            },
                            onCityChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateAddressCity(it))
                            },
                            onStateChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateAddressState(it))
                            },
                            onPostalCodeChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateAddressPostalCode(it))
                            },
                            onCountryChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateAddressCountry(it))
                            },
                            onAddressTypeChange = { type ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateAddressType(type))
                            }
                        )
                    }
                }

                // Notes Section
                item {
                    ExpandableSection(
                        title = "Notes",
                        icon = Icons.Default.Notes,
                        isExpanded = state.expandedSections.contains(ContactSection.NOTES),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.NOTES,
                                    expanded
                                )
                            )
                        }
                    ) {
                        NotesSection(
                            notes = state.notes,
                            onNotesChange = {
                                viewModel.sendEvent(AddEditContactEvent.UpdateNotes(it))
                            }
                        )
                    }
                }

                // Events Section
                item {
                    ExpandableSection(
                        title = "Events",
                        icon = Icons.Default.Event,
                        isExpanded = state.expandedSections.contains(ContactSection.EVENTS),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.EVENTS,
                                    expanded
                                )
                            )
                        }
                    ) {
                        EventsSection(
                            events = state.events,
                            onAddEvent = { viewModel.sendEvent(AddEditContactEvent.AddEvent) },
                            onRemoveEvent = { id ->
                                viewModel.sendEvent(AddEditContactEvent.RemoveEvent(id))
                            },
                            onEventTypeChange = { id, type ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateEventType(id, type))
                            },
                            onEventDateClick = { id ->
                                viewModel.showDatePicker(id)
                            },
                            onEventLabelChange = { id, label ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateEventLabel(id, label))
                            }
                        )
                    }
                }

                // Websites Section
                item {
                    ExpandableSection(
                        title = "Websites",
                        icon = Icons.Default.Language,
                        isExpanded = state.expandedSections.contains(ContactSection.WEBSITES),
                        onToggle = { expanded ->
                            viewModel.sendEvent(
                                AddEditContactEvent.ToggleSection(
                                    ContactSection.WEBSITES,
                                    expanded
                                )
                            )
                        }
                    ) {
                        WebsitesSection(
                            websites = state.websites,
                            onAddWebsite = { viewModel.sendEvent(AddEditContactEvent.AddWebsite) },
                            onRemoveWebsite = { index ->
                                viewModel.sendEvent(AddEditContactEvent.RemoveWebsite(index))
                            },
                            onWebsiteChange = { index, url ->
                                viewModel.sendEvent(AddEditContactEvent.UpdateWebsite(index, url))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BasicInfoSection(
    displayName: String,
    displayNameError: String?,
    photoUri: Uri?,
    isStarred: Boolean,
    onDisplayNameChange: (String) -> Unit,
    onPhotoClick: () -> Unit,
    onStarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPhotoClick)
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Contact Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Star icon
                IconButton(
                    onClick = onStarClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name input
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Full Name *") },
                isError = displayNameError != null,
                supportingText = { displayNameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun PhoneNumbersSection(
    phoneNumbers: List<EditablePhoneNumber>,
    onAddPhone: () -> Unit,
    onRemovePhone: (String) -> Unit,
    onPhoneNumberChange: (String, String) -> Unit,
    onPhoneTypeChange: (String, PhoneNumberType) -> Unit,
    onPhoneLabelChange: (String, String) -> Unit,
    onSetPrimary: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        phoneNumbers.forEach { phone ->
            PhoneNumberItem(
                phone = phone,
                onRemove = { onRemovePhone(phone.id) },
                onNumberChange = { onPhoneNumberChange(phone.id, it) },
                onTypeChange = { onPhoneTypeChange(phone.id, it) },
                onLabelChange = { onPhoneLabelChange(phone.id, it) },
                onSetPrimary = { onSetPrimary(phone.id) }
            )
        }

        Button(
            onClick = onAddPhone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Phone Number")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberItem(
    phone: EditablePhoneNumber,
    onRemove: () -> Unit,
    onNumberChange: (String) -> Unit,
    onTypeChange: (PhoneNumberType) -> Unit,
    onLabelChange: (String) -> Unit,
    onSetPrimary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone type dropdown
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {}
                ) {
                    Text(
                        text = when (phone.type) {
                            PhoneNumberType.HOME -> "Home"
                            PhoneNumberType.WORK -> "Work"
                            PhoneNumberType.MOBILE -> "Mobile"
                            PhoneNumberType.OTHER -> "Other"
                            PhoneNumberType.CUSTOM -> phone.label ?: "Custom"
                        },
                        modifier = Modifier
                            .clickable { }
                            .padding(8.dp)
                    )
                }

                Row {
                    if (!phone.isPrimary) {
                        TextButton(onClick = onSetPrimary) {
                            Text("Set Primary", fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = "Primary",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            OutlinedTextField(
                value = phone.number,
                onValueChange = onNumberChange,
                label = { Text("Phone Number") },
                isError = phone.error != null,
                supportingText = { phone.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        when (phone.type) {
                            PhoneNumberType.HOME -> Icons.Default.Home
                            PhoneNumberType.WORK -> Icons.Default.Work
                            PhoneNumberType.MOBILE -> Icons.Default.Phone
                            else -> Icons.Default.Phone
                        },
                        contentDescription = null
                    )
                }
            )

            if (phone.type == PhoneNumberType.CUSTOM) {
                OutlinedTextField(
                    value = phone.label ?: "",
                    onValueChange = onLabelChange,
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun EmailsSection(
    emails: List<EditableEmail>,
    onAddEmail: () -> Unit,
    onRemoveEmail: (String) -> Unit,
    onEmailChange: (String, String) -> Unit,
    onEmailTypeChange: (String, EmailType) -> Unit,
    onEmailLabelChange: (String, String) -> Unit,
    onSetPrimary: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        emails.forEach { email ->
            EmailItem(
                email = email,
                onRemove = { onRemoveEmail(email.id) },
                onEmailChange = { onEmailChange(email.id, it) },
                onTypeChange = { onEmailTypeChange(email.id, it) },
                onLabelChange = { onEmailLabelChange(email.id, it) },
                onSetPrimary = { onSetPrimary(email.id) }
            )
        }

        Button(
            onClick = onAddEmail,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Email Address")
        }
    }
}

@Composable
fun EmailItem(
    email: EditableEmail,
    onRemove: () -> Unit,
    onEmailChange: (String) -> Unit,
    onTypeChange: (EmailType) -> Unit,
    onLabelChange: (String) -> Unit,
    onSetPrimary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (email.type) {
                        EmailType.HOME -> "Home"
                        EmailType.WORK -> "Work"
                        EmailType.OTHER -> "Other"
                        EmailType.CUSTOM -> email.label ?: "Custom"
                    },
                    modifier = Modifier.padding(8.dp)
                )

                Row {
                    if (!email.isPrimary) {
                        TextButton(onClick = onSetPrimary) {
                            Text("Set Primary", fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = "Primary",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            OutlinedTextField(
                value = email.email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                isError = email.error != null,
                supportingText = { email.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                }
            )

            if (email.type == EmailType.CUSTOM) {
                OutlinedTextField(
                    value = email.label ?: "",
                    onValueChange = onLabelChange,
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun OrganizationSection(
    organization: String,
    jobTitle: String,
    department: String,
    onOrganizationChange: (String) -> Unit,
    onJobTitleChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = organization,
            onValueChange = onOrganizationChange,
            label = { Text("Company") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Business, contentDescription = null)
            }
        )

        OutlinedTextField(
            value = jobTitle,
            onValueChange = onJobTitleChange,
            label = { Text("Job Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = department,
            onValueChange = onDepartmentChange,
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun AddressSection(
    address: EditableAddress,
    onStreetChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onAddressTypeChange: (AddressType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Address type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = address.type == AddressType.HOME,
                onClick = { onAddressTypeChange(AddressType.HOME) },
                label = { Text("Home") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = address.type == AddressType.WORK,
                onClick = { onAddressTypeChange(AddressType.WORK) },
                label = { Text("Work") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = address.type == AddressType.OTHER,
                onClick = { onAddressTypeChange(AddressType.OTHER) },
                label = { Text("Other") }
            )
        }

        OutlinedTextField(
            value = address.street,
            onValueChange = onStreetChange,
            label = { Text("Street Address") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address.city,
            onValueChange = onCityChange,
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = address.state,
                onValueChange = onStateChange,
                label = { Text("State") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = address.postalCode,
                onValueChange = onPostalCodeChange,
                label = { Text("Postal Code") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = address.country,
            onValueChange = onCountryChange,
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notes") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        maxLines = 10
    )
}

@Composable
fun EventsSection(
    events: List<EditableEvent>,
    onAddEvent: () -> Unit,
    onRemoveEvent: (String) -> Unit,
    onEventTypeChange: (String, EventType) -> Unit,
    onEventDateClick: (String) -> Unit,
    onEventLabelChange: (String, String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        events.forEach { event ->
            EventItem(
                event = event,
                onRemove = { onRemoveEvent(event.id) },
                onTypeChange = { onEventTypeChange(event.id, it) },
                onDateClick = { onEventDateClick(event.id) },
                onLabelChange = { onEventLabelChange(event.id, it) }
            )
        }

        Button(
            onClick = onAddEvent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Event")
        }
    }
}

@Composable
fun EventItem(
    event: EditableEvent,
    onRemove: () -> Unit,
    onTypeChange: (EventType) -> Unit,
    onDateClick: () -> Unit,
    onLabelChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Event type selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = event.type == EventType.BIRTHDAY,
                        onClick = { onTypeChange(EventType.BIRTHDAY) },
                        label = { Text("Birthday") }
                    )
                    FilterChip(
                        selected = event.type == EventType.ANNIVERSARY,
                        onClick = { onTypeChange(EventType.ANNIVERSARY) },
                        label = { Text("Anniversary") }
                    )
                    FilterChip(
                        selected = event.type == EventType.OTHER,
                        onClick = { onTypeChange(EventType.OTHER) },
                        label = { Text("Other") }
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }

            // Date picker button
            OutlinedButton(
                onClick = onDateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    event.date?.let { formatDate(it) } ?: "Select Date"
                )
            }

            if (event.type == EventType.OTHER) {
                OutlinedTextField(
                    value = event.label ?: "",
                    onValueChange = onLabelChange,
                    label = { Text("Event Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun WebsitesSection(
    websites: List<String>,
    onAddWebsite: () -> Unit,
    onRemoveWebsite: (Int) -> Unit,
    onWebsiteChange: (Int, String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        websites.forEachIndexed { index, url ->
            OutlinedTextField(
                value = url,
                onValueChange = { onWebsiteChange(index, it) },
                label = { Text("Website URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onRemoveWebsite(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null)
                }
            )
        }

        Button(
            onClick = onAddWebsite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Website")
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!isExpanded) },
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null)
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            if (isExpanded) {
                Divider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            DatePicker(state = datePickerState)
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(Date(timestamp))
}