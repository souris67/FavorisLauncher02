package com.example.favorislauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherScreen() }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember { context.getSharedPreferences("launcher_prefs", MODE_PRIVATE) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var showAll by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                if (packageName == context.packageName) null
                else AppEntry(info.loadLabel(pm).toString(), packageName, info.loadIcon(pm))
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    val visible = apps.filter { it.label.contains(search, ignoreCase = true) }
    val shown = if (showAll) visible else visible.filter { favorites.contains(it.packageName) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Mes applications", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text(if (showAll) "Choisissez vos favoris" else "Accès rapide à vos favoris", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (dark) "Sombre" else "Clair", fontSize = 12.sp)
                        Switch(checked = dark, onCheckedChange = {
                            dark = it; prefs.edit().putBoolean("dark", it).apply()
                        })
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("Rechercher une application") })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showAll, onClick = { showAll = false }, label = { Text("Favoris") })
                    FilterChip(selected = showAll, onClick = { showAll = true }, label = { Text("Toutes les apps") })
                }
                Spacer(Modifier.height(8.dp))
                if (!showAll && shown.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun favori pour l'instant. Ouvrez « Toutes les apps » pour en ajouter.")
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 92.dp), contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            AppTile(app = app, favorite = favorites.contains(app.packageName),
                                onClick = {
                                    if (showAll) {
                                        val next = favorites.toMutableSet()
                                        if (next.contains(app.packageName)) next.remove(app.packageName) else next.add(app.packageName)
                                        favorites = next
                                        prefs.edit().putStringSet("favorites", next.toSet()).apply()
                                    } else {
                                        val launch = pm.getLaunchIntentForPackage(app.packageName)
                                        if (launch != null) context.startActivity(launch)
                                    }
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppTile(app: AppEntry, favorite: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(12.dp).fillMaxWidth()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.graphics.painter.DrawablePainter(app.icon),
            contentDescription = app.label,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(app.label, maxLines = 2, fontSize = 12.sp, lineHeight = 14.sp)
        if (favorite) Text("★", color = Color(0xFFFFB300), fontSize = 15.sp)
        else Text("Ajouter ★", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
    }
}
