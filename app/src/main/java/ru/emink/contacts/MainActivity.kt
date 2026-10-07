package ru.emink.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContactsApp()
            }
        }
    }
}

@Composable
private fun ContactsApp() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = withContext(Dispatchers.IO) {
                context.fetchAllContacts()
            }
        }
    }

    Scaffold { innerPadding ->
        when {
            !hasPermission -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.permission_required))
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            }

            contacts == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            contacts.orEmpty().isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_contacts))
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.contacts_count,
                        contacts.orEmpty().size,
                        contacts.orEmpty().size,
                    ),
                    modifier = Modifier.padding(16.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(contacts.orEmpty()) { contact ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.fromParts("tel", contact.phoneNumber.orEmpty(), null),
                                        ),
                                    )
                                }
                                .padding(16.dp),
                        ) {
                            Text(contact.name.orEmpty())
                            Text(contact.phoneNumber.orEmpty())
                        }
                    }
                }
            }
        }
    }
}
