package com.example.ingresosegresoscx.ui

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ingresosegresoscx.database.DatabaseHelper
import com.example.ingresosegresoscx.models.Categoria
import com.example.ingresosegresoscx.models.Movimiento
import com.example.ingresosegresoscx.ui.theme.CoralExpense
import com.example.ingresosegresoscx.ui.theme.IndigoPrimary
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoFormDialog(
    movimiento: Movimiento?,
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var fecha by remember { mutableStateOf(movimiento?.fecha ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var concepto by remember { mutableStateOf(movimiento?.concepto ?: "") }
    var ingreso by remember { mutableStateOf(if ((movimiento?.debe ?: 0.0) > 0) movimiento?.debe.toString() else "") }
    var egreso by remember { mutableStateOf(if ((movimiento?.haber ?: 0.0) > 0) movimiento?.haber.toString() else "") }
    
    val cuentas = listOf("EFECTIVO", "BANCO")
    var selectedCuenta by remember { mutableStateOf(movimiento?.cuenta ?: "EFECTIVO") }
    var cuentaExpanded by remember { mutableStateOf(false) }
    
    val categorias = remember { dbHelper.obtenerCategorias() }
    var selectedCategoria by remember { mutableStateOf(categorias.find { it.id == movimiento?.categoriaId } ?: categorias.firstOrNull()) }
    var categoriaExpanded by remember { mutableStateOf(false) }

    var imagenUri by remember { mutableStateOf(movimiento?.imagenUri) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPhotoOptions = true
        } else {
            android.widget.Toast.makeText(context, "Permiso de cámara denegado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val savedUri = saveUriToInternalStorage(context, it)
            imagenUri = savedUri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            val savedUri = saveUriToInternalStorage(context, tempPhotoUri!!)
            imagenUri = savedUri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (concepto.isBlank()) return@Button
                val ing = ingreso.toDoubleOrNull() ?: 0.0
                val egr = egreso.toDoubleOrNull() ?: 0.0
                if (ing == 0.0 && egr == 0.0) return@Button
                
                val tipo = if (ing > 0) "INGRESO" else "EGRESO"
                if (movimiento != null) {
                    dbHelper.actualizarMovimiento(movimiento.id, fecha, concepto, ing, egr, selectedCuenta, selectedCategoria?.id ?: 1, tipo, imagenUri)
                } else {
                    dbHelper.insertarMovimiento(fecha, concepto, ing, egr, selectedCuenta, selectedCategoria?.id ?: 1, tipo, imagenUri)
                }
                onSave()
                onDismiss()
            }) {
                Text(if (movimiento == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text(if (movimiento == null) "Nuevo Movimiento" else "Editar Movimiento") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = fecha,
                    onValueChange = {},
                    label = { Text("Fecha") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            val parts = fecha.split("-")
                            val y = parts[0].toInt()
                            val m = parts[1].toInt() - 1
                            val d = parts[2].toInt()
                            DatePickerDialog(context, { _, year, month, day ->
                                fecha = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                            }, y, m, d).show()
                        }) {
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                )
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ingreso,
                        onValueChange = { ingreso = it },
                        label = { Text("Ingreso ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = egreso,
                        onValueChange = { egreso = it },
                        label = { Text("Egreso ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                ExposedDropdownMenuBox(
                    expanded = cuentaExpanded,
                    onExpandedChange = { cuentaExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCuenta,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuenta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cuentaExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = cuentaExpanded, onDismissRequest = { cuentaExpanded = false }) {
                        cuentas.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { selectedCuenta = it; cuentaExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = categoriaExpanded,
                    onExpandedChange = { categoriaExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategoria?.nombre ?: "Sin Categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = categoriaExpanded, onDismissRequest = { categoriaExpanded = false }) {
                        categorias.forEach {
                            DropdownMenuItem(text = { Text(it.nombre) }, onClick = { selectedCategoria = it; categoriaExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Evidencia / Foto:", style = MaterialTheme.typography.titleSmall)
                
                if (imagenUri != null) {
                    Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().height(150.dp)) {
                        AsyncImage(
                            model = imagenUri,
                            contentDescription = "Vista previa",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imagenUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar foto", tint = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { 
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar Foto")
                    }
                }
            }
        }
    )

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Seleccionar origen") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Cámara") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            val uri = createTempImageUri(context)
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Galería") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptions = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun GraficaGastosDialog(
    resumen: Map<String, Double>,
    titulo: String,
    onDismiss: () -> Unit
) {
    val total = resumen.values.sum()
    val colores = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5),
        Color(0xFF43A047), Color(0xFFFB8C00),
        Color(0xFF8E24AA), Color(0xFF00ACC1),
        Color(0xFF3949AB), Color(0xFFD81B60)
    )
    val fmt = DecimalFormat("$#,##0.00")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (total == 0.0) {
                    Text("No hay egresos en este periodo.", modifier = Modifier.padding(16.dp))
                } else {
                    Canvas(modifier = Modifier.size(200.dp).padding(16.dp)) {
                        var startAngle = 0f
                        resumen.values.forEachIndexed { index, value ->
                            val sweepAngle = (value / total).toFloat() * 360f
                            drawArc(
                                color = colores[index % colores.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                style = Fill
                            )
                            startAngle += sweepAngle
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    resumen.entries.forEachIndexed { index, entry ->
                        val porc = (entry.value / total) * 100
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(12.dp).background(colores[index % colores.size]))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${entry.key}: ${fmt.format(entry.value)} (%.1f%%)".format(porc),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TransferenciaDialog(
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var monto by remember { mutableStateOf("") }
    var concepto by remember { mutableStateOf("") }
    val cuentas = listOf("EFECTIVO", "BANCO")
    var origen by remember { mutableStateOf("EFECTIVO") }
    var destino by remember { mutableStateOf("BANCO") }
    var origenExpanded by remember { mutableStateOf(false) }
    var destinoExpanded by remember { mutableStateOf(false) }
    val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val m = monto.toDoubleOrNull() ?: 0.0
                if (m <= 0) return@Button
                if (origen == destino) return@Button
                
                dbHelper.registrarTransferencia(fecha, m, origen, destino, concepto)
                onSave()
                onDismiss()
            }) { Text("Transferir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Transferencia entre Cuentas") },
        text = {
            Column {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto (opcional)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = origen,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Origen") },
                            modifier = Modifier.clickable { origenExpanded = true }.fillMaxWidth()
                        )
                        DropdownMenu(expanded = origenExpanded, onDismissRequest = { origenExpanded = false }) {
                            cuentas.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { origen = it; origenExpanded = false }) }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = destino,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Destino") },
                            modifier = Modifier.clickable { destinoExpanded = true }.fillMaxWidth()
                        )
                        DropdownMenu(expanded = destinoExpanded, onDismissRequest = { destinoExpanded = false }) {
                            cuentas.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { destino = it; destinoExpanded = false }) }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CategoriasDialog(
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    var categorias by remember { mutableStateOf(dbHelper.obtenerCategorias()) }
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Categorías")
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        },
        text = {
            Column(modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                categorias.forEach { cat ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.nombre, modifier = Modifier.weight(1f))
                        if (cat.id != 1) {
                            IconButton(onClick = {
                                dbHelper.eliminarCategoria(cat.id)
                                categorias = dbHelper.obtenerCategorias()
                                onChanged()
                            }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        }
                    }
                }
            }
        }
    )

    if (showAddDialog) {
        var newCatName by remember { mutableStateOf("") }
        var selectedTipo by remember { mutableStateOf("EGRESO") }
        val tipos = listOf("INGRESO", "EGRESO", "AMBOS")
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        dbHelper.insertarCategoria(newCatName, selectedTipo)
                        categorias = dbHelper.obtenerCategorias()
                        showAddDialog = false
                        onChanged()
                    }
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } },
            title = { Text("Nueva Categoría") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nombre") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tipo:", style = MaterialTheme.typography.labelSmall)
                    Row {
                        tipos.forEach { tipo ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedTipo == tipo, onClick = { selectedTipo = tipo })
                                Text(tipo, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsBottomSheet(
    onDismiss: () -> Unit,
    onOptionClick: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Opciones y Configuración",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
            
            OptionItem(Icons.Default.SyncAlt, "Transferencia entre Cuentas", "Mover dinero entre Efectivo y Banco") {
                onOptionClick(0)
            }
            OptionItem(Icons.Default.Category, "Gestionar Categorías", "Añadir o eliminar categorías de gastos") {
                onOptionClick(1)
            }
            OptionItem(Icons.Default.AccountBalanceWallet, "Saldos Iniciales", "Ajustar el saldo de apertura de tus cuentas") {
                onOptionClick(2)
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            
            OptionItem(Icons.Default.TableChart, "Exportar a Excel (.xls)", "Generar reporte de movimientos") {
                onOptionClick(3)
            }
            OptionItem(Icons.Default.PictureAsPdf, "Exportar a PDF (.pdf)", "Generar PDF") {
                onOptionClick(6)
            }
            OptionItem(Icons.Default.Backup, "Exportar Respaldo Completo (ZIP)", "Guardar base de datos y fotos de tickets") {
                onOptionClick(4)
            }
            OptionItem(Icons.Default.FileUpload, "Importar Respaldo Completo (ZIP)", "Restaurar datos y fotos desde archivo") {
                onOptionClick(5)
            }
        }
    }
}

@Composable
fun OptionItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun PhotoViewerDialog(
    imagenUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imagenUri,
                contentDescription = "Full Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

// Helpers para fotos
fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
}

fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val dir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
        val outFile = File(dir, fileName)
        val outputStream = FileOutputStream(outFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        Uri.fromFile(outFile).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun SaldosInicialesDialog(
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var ef by remember { mutableStateOf(dbHelper.obtenerSaldoInicial("EFECTIVO").let { if (it > 0) it.toString() else "" }) }
    var ba by remember { mutableStateOf(dbHelper.obtenerSaldoInicial("BANCO").let { if (it > 0) it.toString() else "" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                dbHelper.guardarSaldoInicial("EFECTIVO", ef.toDoubleOrNull() ?: 0.0)
                dbHelper.guardarSaldoInicial("BANCO", ba.toDoubleOrNull() ?: 0.0)
                onSave()
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Saldos Iniciales") },
        text = {
            Column {
                OutlinedTextField(value = ef, onValueChange = { ef = it }, label = { Text("Saldo Efectivo") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = ba, onValueChange = { ba = it }, label = { Text("Saldo Banco") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.padding(top = 8.dp))
            }
        }
    )
}
