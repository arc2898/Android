package com.arc.personalcallerid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF18252B)
private val Gold = Color(0xFFD7A84A)
private val Paper = Color(0xFFF7F4ED)
private val Muted = Color(0xFF6C777B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("personal_caller_id", MODE_PRIVATE)
        setContent {
            var number by remember { mutableStateOf("") }
            var name by remember { mutableStateOf("") }
            var tab by remember { mutableStateOf("Search") }
            var saved by remember {
                mutableStateOf(
                    runCatching {
                        val arr = JSONArray(prefs.getString("saved", "[]"))
                        (0 until arr.length()).map { i ->
                            val obj = arr.getJSONObject(i)
                            SavedContact(obj.optString("name"), obj.optString("number"))
                        }
                    }.getOrDefault(emptyList())
                )
            }
            fun persist(items: List<SavedContact>) {
                val arr = JSONArray()
                items.forEach { arr.put(JSONObject().put("name", it.name).put("number", it.number)) }
                prefs.edit().putString("saved", arr.toString()).apply()
            }
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Ink,
                    secondary = Gold,
                    background = Paper,
                    surface = Color.White,
                    onPrimary = Color.White,
                    onBackground = Ink,
                    onSurface = Ink
                )
            ) {
                Scaffold(
                    containerColor = Paper,
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            listOf("Search", "Saved").forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    icon = {
                                        Icon(
                                            if (item == "Search") Icons.Default.Call else Icons.Default.Person,
                                            contentDescription = item
                                        )
                                    },
                                    label = { Text(item) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.size(48.dp).background(Ink, RoundedCornerShape(15.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Call, null, tint = Gold, modifier = Modifier.size(25.dp))
                            }
                            Column {
                                Text("Personal Caller ID", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("Simple. Private. Yours.", fontSize = 13.sp, color = Muted)
                            }
                        }
                        if (tab == "Search") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("NUMBER LOOKUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Gold)
                                Text("Who are you calling?", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("Enter a number to open your phone app. This starter version does not claim to identify unknown callers without a trusted lookup provider.", color = Muted, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = number,
                                    onValueChange = { number = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Phone number") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Button(
                                    onClick = { if (number.isNotBlank()) runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number.trim())}"))) } },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                                ) {
                                    Icon(Icons.Default.Call, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open dialer")
                                }
                                HorizontalDivider(color = Color(0xFFE6E0D4))
                                Text("SAVE A NUMBER", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Gold)
                                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Name or label") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                                OutlinedTextField(value = number, onValueChange = { number = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Phone number to save") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        val n = number.trim()
                                        if (n.isNotEmpty()) {
                                            saved = (saved.filterNot { it.number == n } + SavedContact(name.trim().ifBlank { n }, n))
                                            persist(saved)
                                            name = ""
                                            tab = "Saved"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ) { Text("Save locally") }
                            }
                        } else {
                            Text("YOUR SAVED NUMBERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Gold)
                            if (saved.isEmpty()) {
                                Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Person, null, tint = Gold, modifier = Modifier.size(32.dp))
                                        Text("No saved numbers yet", fontWeight = FontWeight.SemiBold)
                                        Text("Save a number from Search. It stays on this device.", color = Muted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(saved, key = { it.number }) { contact ->
                                        Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(42.dp).background(Paper, CircleShape), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Person, null, tint = Ink)
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(contact.name, fontWeight = FontWeight.SemiBold)
                                                    Text(contact.number, color = Muted, fontSize = 13.sp)
                                                }
                                                IconButton(onClick = { runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(contact.number)}"))) } }) {
                                                    Icon(Icons.Default.Call, "Call", tint = Ink)
                                                }
                                                IconButton(onClick = { saved = saved.filterNot { it.number == contact.number }; persist(saved) }) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = Muted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Caller names can be inaccurate or spoofed. Verify important calls independently.", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private data class SavedContact(val name: String, val number: String)
