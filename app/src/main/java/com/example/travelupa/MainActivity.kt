package com.example.travelupa

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.travelupa.ui.RegisterScreen
import com.example.travelupa.ui.theme.TravelupaTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val currentUser : FirebaseUser? = FirebaseAuth.getInstance().currentUser

        setContent {
            TravelupaTheme {
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppNavigation(currentUser)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(currentUser: FirebaseUser?) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) {
            Log.w("Current User", currentUser.toString())
            Screen.RekomendasiTempat.route
        } else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.RekomendasiTempat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.RekomendasiTempat.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.RekomendasiTempat.route) {
            RekomendasiTempatScreen(
                onBackToLogin = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RekomendasiTempat.route) { inclusive = true}
                    }
                }
            )
        }
    }
}

@Composable
fun TambahTempatWisataDialog(
    firestore: FirebaseFirestore,
    context: Context,
    onDismiss: () -> Unit,
    onTambah: (String, String, String?) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var gambarUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val gambarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        gambarUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tempat Wisata Baru") },
        text = {
            Column {
                TextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Tempat") },
                    modifier = Modifier.fillMaxWidth (),
                    enabled = !isUploading
                )
                Spacer (modifier = Modifier.height (8.dp))
                TextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth (),
                    enabled = !isUploading
                )
                Spacer (modifier = Modifier.height (8.dp))
                gambarUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Gambar yang dipilih",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height (8.dp))
                Button (
                    onClick = { gambarLauncher.launch("image/*") },
                    modifier = Modifier . fillMaxWidth (),
                    enabled = !isUploading
                ) {
                    Text("Pilih Gambar")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && deskripsi.isNotBlank() && gambarUri != null) {
                        isUploading = true
                        val tempatWisata = TempatWisata(nama, deskripsi)
                        uploadImageToFirestore(
                            firestore,
                            context,
                            gambarUri!!,
                            tempatWisata,
                            onSuccess = { uploadedTempat ->
                                isUploading = false
                                onTambah(nama, deskripsi, uploadedTempat.gambarUriString)
                                onDismiss()
                            },
                            onFailure = { e ->
                                isUploading = false
                            }
                        )
                    }
                },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator (
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Tambah")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =
                        MaterialTheme.colors.surface
                ),
                enabled = !isUploading
            ) {
                Text("Batal")
            }
        }
    )
}

fun uploadImageToFirestore(
    firestore: FirebaseFirestore,
    context: Context,
    uri: Uri,
    tempatWisata: TempatWisata,
    onSuccess: (TempatWisata) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child(
        "tempat_wisata/${UUID.randomUUID()}.jpg"
    )

    imageRef.putFile(uri)
        .addOnSuccessListener {

            imageRef.downloadUrl
                .addOnSuccessListener { downloadUri ->

                    val data = tempatWisata.copy(
                        gambarUriString = downloadUri.toString()
                    )

                    firestore.collection("tempat_wisata")
                        .add(data)
                        .addOnSuccessListener {
                            onSuccess(data)
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { (mutableStateOf<String?>(null)) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { (Text("Email")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter email and password"
                    return@Button
                }

                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        val authResult = withContext(Dispatchers.IO) {
                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                .await()
                        }
                        isLoading = false
                        onLoginSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Login failed: ${e.localizedMessage}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text("Login")
            }
        }
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(
            onClick = { onRegisterClick() }
        ) {
            Text("Belum punya akun? Daftar")
        }
    }
}

@Composable
fun GreetingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Selamat Datang di Travelupa!",
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center
            )
            Spacer (modifier = Modifier.height(16.dp))
            Text(
                text = "Solusi buat kamu yang lupa kemana-mana",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .width(360.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Mulai")
        }
    }
}

data class TempatWisata(
    val nama: String = "",
    val deskripsi: String = "",
    val gambarUriString: String? = null,
    val gambarResId: Int? = null
)

@Composable
fun RekomendasiTempatScreen(
    onBackToLogin: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var daftarTempatWisata by remember { mutableStateOf(listOf(
        TempatWisata(
            "Tumpak Sewu",
            "Air terjun tercantik di Jawa Timur.",
            gambarResId =  R.drawable.tumsew
        ),
        TempatWisata(
            "Gunung Bromo",
            "Matahari terbitnya bagus banget.",
            gambarResId = R.drawable.bromo
        )
    )) }

    var showTambahDialog by remember { mutableStateOf(false) }

    Scaffold (
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTambahDialog = true },
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Tempat Wisata")
            }
        }
    ) { paddingValues ->
        Column (
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn {
                items(daftarTempatWisata) { tempat ->
                    TempatItemEditable(
                        tempat = tempat,
                        onDelete = {
                            daftarTempatWisata = daftarTempatWisata.filter { it != tempat }
                        }
                    )
                }
            }
        }

        if (showTambahDialog) {
            TambahTempatWisataDialog(
                firestore,
                context,
                onDismiss = {showTambahDialog = false},
                onTambah = { nama, deskripsi, gambar ->
                    val nuevoTempat = TempatWisata(nama, deskripsi, gambar)
                    daftarTempatWisata = daftarTempatWisata + nuevoTempat
                    showTambahDialog = false
                }
            )
        }
    }
}

@Composable
fun TempatItemEditable(
    tempat: TempatWisata,
    onDelete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colors.surface),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = tempat.gambarUriString?.let { uriString ->
                    rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(uriString))
                            .build()
                    )
                } ?: tempat.gambarResId?.let {
                    painterResource(id = it)
                } ?: painterResource(
                    id =
                        R.drawable.default_image
                ),
                contentDescription = tempat.nama,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        text = tempat.nama,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp, top = 12.dp)
                    )
                    Text(
                        text = tempat.deskripsi,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(250.dp, 0.dp)
                ) {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        firestore.collection("tempat_wisata").document(tempat.nama)
                            .delete()
                            .addOnSuccessListener {
                                onDelete()
                            }
                            .addOnFailureListener { e ->
                                Log.w("TempatItemEditable", "Error deleting document", e)
                            }
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun GambarPicker (
    gambarUri: Uri?,
    onPilihGambar: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        gambarUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter (
                    ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .build()
                ),
                contentDescription = "Gambar Tempat Wisata",
                modifier = Modifier
                    .size(200.dp)
                    .clickable { onPilihGambar() },
                contentScale = ContentScale.Crop
            )
        } ?: run {
            OutlinedButton(
                onClick = onPilihGambar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled. Add, contentDescription = "Pilih Gambar")
                Spacer (modifier = Modifier.width(8.dp))
                Text("Pilih Gambar")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    TravelupaTheme {
//        RekomendasiTempatScreen()
//    }
//}