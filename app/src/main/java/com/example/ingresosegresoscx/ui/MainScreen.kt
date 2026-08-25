package com.example.ingresosegresoscx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ingresosegresoscx.models.Movimiento
import com.example.ingresosegresoscx.ui.theme.CoralExpense
import com.example.ingresosegresoscx.ui.theme.EmeraldIncome
import com.example.ingresosegresoscx.ui.theme.IndigoPrimary
import java.text.DecimalFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onOpenDialog: (String) -> Unit,
    onEditMovimiento: (Movimiento) -> Unit,
    onAddMovimiento: () -> Unit
) {
    val saldos by viewModel.saldos
    val movements by viewModel.movimientos
    val resumen by viewModel.resumenMes
    val selectedMonth by viewModel.mesSeleccionado
    val selectedYear by viewModel.anioSeleccionado
    val availableYears by viewModel.aniosDisponibles
    val availableMonths by viewModel.mesesDisponibles
    val searchQuery by viewModel.searchQuery

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }
    var imagenUriParaVer by remember { mutableStateOf<String?>(null) }

    val fmt = DecimalFormat("$#,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Buscar concepto...", color = Color.White.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.White,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    } else {
                        Text("Ingresos y Egresos")
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar búsqueda", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                        IconButton(onClick = { onOpenDialog("Grafica") }) {
                            Icon(Icons.Default.List, contentDescription = "Gráfica")
                        }
                        IconButton(onClick = { onOpenDialog("Menu") }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                    } else {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IndigoPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Ir al inicio")
                }

                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (movements.isNotEmpty()) {
                                listState.animateScrollToItem(movements.size - 1)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ir al final")
                }

                FloatingActionButton(
                    onClick = onAddMovimiento,
                    containerColor = IndigoPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!isSearchActive) {
                Dashboard(saldos, fmt)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (availableYears.isNotEmpty() || isSearchActive) {
                if (!isSearchActive) {
                    Filters(
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        availableMonths = availableMonths,
                        availableYears = availableYears,
                        onMonthSelected = { viewModel.setMes(it) },
                        onYearSelected = { viewModel.setAnio(it) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ResumenMesCard(resumen, fmt)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Text(
                    text = if (isSearchActive) "Resultados de búsqueda" else "Movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(movements) { movement ->
                        MovementItem(
                            movement = movement,
                            fmt = fmt,
                            onEdit = { onEditMovimiento(movement) },
                            onDelete = { viewModel.eliminarMovimiento(movement.id) },
                            onClickThumbnail = { imagenUriParaVer = it }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay datos registrados aún.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Text(
                            text = "Pulsa + para agregar tu primer movimiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    if (imagenUriParaVer != null) {
        PhotoViewerDialog(
            imagenUri = imagenUriParaVer!!,
            onDismiss = { imagenUriParaVer = null }
        )
    }
}

@Composable
fun Dashboard(saldos: Map<String, Double>, fmt: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IndigoPrimary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Balance Total",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = fmt.format(saldos["TOTAL"] ?: 0.0),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SaldoSmallItem("Efectivo", fmt.format(saldos["EFECTIVO"] ?: 0.0), Icons.Default.Payments)
                SaldoSmallItem("Banco", fmt.format(saldos["BANCO"] ?: 0.0), Icons.Default.AccountBalance)
            }
        }
    }
}

@Composable
fun SaldoSmallItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Filters(
    selectedMonth: Int,
    selectedYear: Int,
    availableMonths: List<Int>,
    availableYears: List<Int>,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = monthExpanded,
            onExpandedChange = { monthExpanded = !monthExpanded },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = if (selectedMonth in 1..12) months[selectedMonth - 1] else "Seleccionar",
                onValueChange = {},
                readOnly = true,
                label = { Text("Mes") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                modifier = Modifier.menuAnchor(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false }
            ) {
                availableMonths.forEach { monthIdx ->
                    DropdownMenuItem(
                        text = { Text(months[monthIdx - 1]) },
                        onClick = {
                            onMonthSelected(monthIdx)
                            monthExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = yearExpanded,
            onExpandedChange = { yearExpanded = !yearExpanded },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = selectedYear.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Año") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                modifier = Modifier.menuAnchor(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false }
            ) {
                availableYears.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearSelected(year)
                            yearExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ResumenMesCard(resumen: MainViewModel.ResumenMes, fmt: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Ingresos", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "+" + fmt.format(resumen.totalIngresos),
                    color = EmeraldIncome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Egresos", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "-" + fmt.format(resumen.totalEgresos),
                    color = CoralExpense,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Balance", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = fmt.format(resumen.balance),
                    color = if (resumen.balance < 0) CoralExpense else IndigoPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MovementItem(
    movement: Movimiento,
    fmt: DecimalFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClickThumbnail: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (movement.imagenUri != null) {
                    AsyncImage(
                        model = movement.imagenUri,
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClickThumbnail(movement.imagenUri!!) },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (movement.debe > 0) EmeraldIncome.copy(alpha = 0.2f) else CoralExpense.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (movement.debe > 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (movement.debe > 0) EmeraldIncome else CoralExpense
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = movement.concepto, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(text = "${movement.nombreCategoria} • ${movement.cuenta}", fontSize = 12.sp, color = Color.Gray)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val amount = if (movement.debe > 0) movement.debe else movement.haber
                    Text(
                        text = (if (movement.debe > 0) "+" else "-") + fmt.format(amount),
                        color = if (movement.debe > 0) EmeraldIncome else CoralExpense,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = movement.fecha, fontSize = 11.sp, color = Color.Gray)
                }
            }
            
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = CoralExpense)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}
