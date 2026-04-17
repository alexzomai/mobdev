package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var hasPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) contacts = context.fetchAllContacts()
    }

    LaunchedEffect(Unit) {
        launcher.launch(android.Manifest.permission.READ_CONTACTS)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.contacts)) })
        }
    ) { paddingValues ->
        if (hasPermission) {
            LazyColumn(Modifier.padding(paddingValues)) {
                items(contacts) { contact ->
                    ContactItem(name = contact.name ?: "?", onClick = { selectedContact = contact })
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.no_permission))
                    Text(stringResource(R.string.allow_in_settings))
                }
            }
        }

        if (selectedContact != null) {
            AlertDialog(
                onDismissRequest = { selectedContact = null },
                confirmButton = { },
                title = { Text(selectedContact!!.name ?: "?") },
                text = {
                    Column {
                        Text(selectedContact!!.phoneNumber ?: stringResource(R.string.no_phone))
                        Text(selectedContact!!.email ?: stringResource(R.string.no_email))
                    }
                }
            )
        }
    }
}

@Composable
fun ContactItem(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(12.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Red, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.first().toString())
        }
        Text(name, modifier = Modifier.padding(start = 12.dp), fontSize = 18.sp)
    }
}