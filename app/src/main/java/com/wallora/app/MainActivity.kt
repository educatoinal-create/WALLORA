package com.wallora.app

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val dark = Color(0xFF090A15)
private val purple = Color(0xFFBD9BFF)
private val categories = listOf("AMOLED", "Nature", "Space", "Neon", "Abstract", "Minimal")
private data class Wallpaper(val id: Int, val category: String, val file: String) {
    val title: String get() = category + " " + (id % 4 + 1)
}
private val wallpapers = categories.flatMapIndexed { idx, category ->
    (1..4).map { no ->
        Wallpaper(idx * 4 + no - 1, category, category.lowercase() + "_" + no.toString().padStart(2, '0') + ".jpg")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 10, 21)
        window.navigationBarColor = android.graphics.Color.rgb(9, 10, 21)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = purple, background = dark, surface = Color(0xFF191A2B))) {
                Gallery()
            }
        }
    }
}

@Composable
private fun loadImage(context: Context, filename: String, sampleSize: Int): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(filename, sampleSize) {
        try {
            context.assets.open("wallpapers/" + filename).use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
}

private fun setWallpaper(context: Context, filename: String, flag: Int) {
    context.assets.open("wallpapers/" + filename).use { stream ->
        val image = requireNotNull(BitmapFactory.decodeStream(stream))
        try {
            WallpaperManager.getInstance(context).setBitmap(image, null, true, flag)
        } finally { image.recycle() }
    }
}

private fun saveWallpaper(context: Context, filename: String) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "WALLORA_" + filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WALLORA")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
    try {
        val output = requireNotNull(context.contentResolver.openOutputStream(uri))
        output.use { stream ->
            context.assets.open("wallpapers/" + filename).use { it.copyTo(stream) }
        }
        context.contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }, null, null)
    } catch (e: Exception) {
        context.contentResolver.delete(uri, null, null)
        throw e
    }
}

@Composable
private fun Gallery() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val prefs = remember { context.getSharedPreferences("wallora", Context.MODE_PRIVATE) }
    val favorites = remember {
        mutableStateListOf<Int>().apply {
            addAll((prefs.getStringSet("favorites", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() })
        }
    }
    fun toggleFavorite(id: Int) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("favorites", favorites.map { it.toString() }.toSet()).apply()
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Wallpaper?>(null) }
    val shown = wallpapers.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
            (!onlyFavorites || favorites.contains(it.id)) &&
            (search.isBlank() || it.title.contains(search, ignoreCase = true) || it.category.contains(search, ignoreCase = true))
    }

    Scaffold(containerColor = dark, snackbarHost = { SnackbarHost(snackbar) }) { scaffoldPadding ->
        if (selected != null) {
            val current = selected!!
            Column(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selected = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(current.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { toggleFavorite(current.id) }) {
                        Icon(
                            if (favorites.contains(current.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (favorites.contains(current.id)) purple else Color.White
                        )
                    }
                }
                val image = loadImage(context, current.file, 1)
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = current.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    )
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Wallpaper image unavailable")
                    }
                }
                Text("SET WALLPAPER", color = purple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Home" to WallpaperManager.FLAG_SYSTEM,
                        "Lock" to WallpaperManager.FLAG_LOCK,
                        "Both" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    ).forEach { (name, flag) ->
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { setWallpaper(context, current.file, flag) }
                                    }
                                    snackbar.showSnackbar(if (result.isSuccess) "Wallpaper applied to " + name else "Could not apply: " + (result.exceptionOrNull()?.message ?: "unknown error"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) { Text(name, fontSize = 12.sp) }
                    }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { saveWallpaper(context, current.file) }
                            }
                            snackbar.showSnackbar(if (result.isSuccess) "Saved to Pictures/WALLORA" else "Save failed: " + (result.exceptionOrNull()?.message ?: "unknown error"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Download to Gallery") }
                Spacer(Modifier.height(6.dp))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text("WALLORA", fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp, fontSize = 27.sp, color = purple)
                    Text("24 original wallpapers • Offline gallery", color = Color(0xFFB5B2C6), fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search wallpapers") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Text("Explore", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = { onlyFavorites = !onlyFavorites },
                        label = { Text("Favorites") },
                        leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.size + 1) { idx ->
                        val category = if (idx == 0) "All" else categories[idx - 1]
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No wallpapers found") }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(shown, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.64f)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(Color(0xFF222039))
                                    .clickable { selected = item }
                            ) {
                                val bitmap = loadImage(context, item.file, 4)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text(
                                    item.title,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                        .background(Color(0xB2000010))
                                        .fillMaxWidth().padding(10.dp),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { toggleFavorite(item.id) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        if (favorites.contains(item.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Toggle favorite",
                                        tint = if (favorites.contains(item.id)) purple else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
