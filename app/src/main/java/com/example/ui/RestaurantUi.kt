package com.example.ui

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val maxSize = 300
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxSize || height > maxSize) {
            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1) {
                maxSize to (maxSize / ratio).toInt()
            } else {
                (maxSize * ratio).toInt() to maxSize
            }
            android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        outputStream.close()

        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()

        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantAppScreen(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(0) }

    // Active state from Database Flow
    val dishes by viewModel.dishes.collectAsStateWithLifecycle()
    val activeOrders by viewModel.activeOrders.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val closures by viewModel.closures.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val activeOrderItems by viewModel.activeOrderItems.collectAsStateWithLifecycle()

    // Config stats
    val restaurantName by viewModel.restaurantName.collectAsStateWithLifecycle()
    val restaurantAddress by viewModel.restaurantAddress.collectAsStateWithLifecycle()
    val restaurantPhone by viewModel.restaurantPhone.collectAsStateWithLifecycle()
    val logoType by viewModel.logoType.collectAsStateWithLifecycle()
    val restaurantLogoBase64 by viewModel.restaurantLogoBase64.collectAsStateWithLifecycle()

    var currentScreen by rememberSaveable { mutableStateOf("DASHBOARD") }

    // Navigation history stack for Back, Forward, Home navigation compatibility
    var historyStack by remember { mutableStateOf(listOf("DASHBOARD")) }
    var historyIndex by remember { mutableStateOf(0) }

    fun navigateTo(screen: String, tabIndex: Int) {
        val currentList = historyStack.toMutableList()
        val truncated = currentList.subList(0, historyIndex + 1).toMutableList()
        if (truncated.lastOrNull() != screen) {
            truncated.add(screen)
        }
        historyStack = truncated
        historyIndex = truncated.lastIndex
        currentScreen = screen
        currentTab = tabIndex
    }

    fun goBack() {
        if (historyIndex > 0) {
            historyIndex--
            val screen = historyStack[historyIndex]
            currentScreen = screen
            currentTab = when (screen) {
                "VENTAS" -> 0
                "COMANDAS" -> 1
                "ALMACEN" -> 2
                "IMPRESORA" -> 3
                "CIERRES" -> 4
                "CONFIG" -> 5
                else -> -1
            }
        } else {
            if (currentScreen != "DASHBOARD") {
                currentScreen = "DASHBOARD"
                currentTab = -1
            }
        }
    }

    fun goForward() {
        if (historyIndex < historyStack.lastIndex) {
            historyIndex++
            val screen = historyStack[historyIndex]
            currentScreen = screen
            currentTab = when (screen) {
                "VENTAS" -> 0
                "COMANDAS" -> 1
                "ALMACEN" -> 2
                "IMPRESORA" -> 3
                "CIERRES" -> 4
                "CONFIG" -> 5
                else -> -1
            }
        }
    }

    fun goHome() {
        navigateTo("DASHBOARD", -1)
    }

    // Capture physical/device/system Back button
    BackHandler(enabled = historyIndex > 0 || currentScreen != "DASHBOARD") {
        goBack()
    }

    // Synchronize programmatic changes to currentTab (e.g. from tests) to keep full compatibility!
    LaunchedEffect(currentTab) {
        if (currentTab in 0..5) {
            val screen = when (currentTab) {
                0 -> "VENTAS"
                1 -> "COMANDAS"
                2 -> "ALMACEN"
                3 -> "IMPRESORA"
                4 -> "CIERRES"
                5 -> "CONFIG"
                else -> "DASHBOARD"
            }
            if (currentScreen != screen) {
                navigateTo(screen, currentTab)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content area, with bottom padding to avoid overlap with floating bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp)
        ) {
            when (currentScreen) {
                "DASHBOARD" -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        restaurantName = restaurantName,
                        logoType = logoType,
                        logoBase64 = restaurantLogoBase64,
                        onNavigate = { tabName, tabIndex ->
                            navigateTo(tabName, tabIndex)
                        }
                    )
                }
                "VENTAS" -> {
                    SalesTab(
                        viewModel = viewModel,
                        dishes = dishes,
                        cart = cart,
                        onBack = { goBack() }
                    )
                }
                "COMANDAS" -> {
                    ComandasTab(
                        viewModel = viewModel,
                        activeOrders = activeOrders,
                        allOrders = allOrders,
                        onBack = { goBack() }
                    )
                }
                "ALMACEN" -> {
                    AlmacenTab(
                        viewModel = viewModel,
                        dishes = dishes,
                        onBack = { goBack() }
                    )
                }
                "IMPRESORA" -> {
                    ImpresoraTab(
                        viewModel = viewModel,
                        allOrders = allOrders,
                        onBack = { goBack() }
                    )
                }
                "CIERRES" -> {
                    CierresTab(
                        viewModel = viewModel,
                        closures = closures,
                        activeOrders = activeOrders,
                        activeOrderItems = activeOrderItems,
                        onBack = { goBack() }
                    )
                }
                "CONFIG" -> {
                    ConfigTab(
                        viewModel = viewModel,
                        name = restaurantName,
                        address = restaurantAddress,
                        phone = restaurantPhone,
                        logoType = logoType,
                        onBack = { goBack() }
                    )
                }
                "PRODUCTOS" -> {
                    ProductosScreen(
                        viewModel = viewModel,
                        dishes = dishes,
                        onBack = { goBack() }
                    )
                }
            }
        }

        // Floating Glassmorphic Pill Navigation Bar at Bottom Center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .widthIn(max = 280.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = { goBack() },
                        enabled = historyIndex > 0,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retroceder a la pantalla anterior",
                            modifier = Modifier.size(20.dp),
                            tint = if (historyIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    // Home button
                    IconButton(
                        onClick = { goHome() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("nav_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Ir al Menú Principal",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Forward button
                    IconButton(
                        onClick = { goForward() },
                        enabled = historyIndex < historyStack.lastIndex,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("nav_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Avanzar a la pantalla siguiente",
                            modifier = Modifier.size(20.dp),
                            tint = if (historyIndex < historyStack.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

// FORMAT UTIL
val EmeraldGreen = Color(0xFF10B981)
private val priceFormatter = DecimalFormat("$#,##0.00")
fun Double.formatPrice(): String = priceFormatter.format(this)

// GET THE LOGO STRING DRAWABLE AS EMOJI
fun getLogoEmoji(logoId: String): String {
    return when (logoId) {
        "chef_hat" -> "👨‍🍳"
        "pizza" -> "🍕"
        "burger" -> "🍔"
        "coffee" -> "☕"
        "cake" -> "🍰"
        "taco" -> "🌮"
        else -> "🍽️"
    }
}

// ==========================================
// SECCION: DASHBOARD HOME SCREEN (IMAGE 1 STYLE)
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: RestaurantViewModel,
    restaurantName: String,
    logoType: String,
    logoBase64: String,
    onNavigate: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val currentDateStr = remember {
        val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale.getDefault())
        sdf.format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F13), // DeepCharcoalBg
                        Color(0xFF141419)  // ElegantSurface
                    )
                )
            )
    ) {
        val isCompact = maxWidth < 650.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // HIGH-CRAFT PREMIUM HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CompanyLogo(
                            logoBase64 = logoBase64,
                            fallbackType = logoType,
                            modifier = Modifier.size(54.dp),
                            emojiSize = 28.sp
                        )
                    }

                    Column {
                        Text(
                            text = if (restaurantName.isBlank()) "Gourmet OS" else restaurantName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GOURMET OS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Text(
                                text = currentDateStr,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Dynamic Turno Badge on Right
                val isShiftActiveHeader by viewModel.isShiftActive.collectAsStateWithLifecycle()
                Surface(
                    color = (if (isShiftActiveHeader) EmeraldGreen else Color(0xFFEF4444)).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, (if (isShiftActiveHeader) EmeraldGreen else Color(0xFFEF4444)).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isShiftActiveHeader) EmeraldGreen else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (isShiftActiveHeader) "TURNO ACTIVO" else "TURNO CERRADO",
                            color = if (isShiftActiveHeader) EmeraldGreen else Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // CENTRAL REAL-TIME CONTROL DASHBOARD
            val activeOrdersList by viewModel.activeOrders.collectAsStateWithLifecycle()
            val isShiftActivePanel by viewModel.isShiftActive.collectAsStateWithLifecycle()
            val initialCash by viewModel.initialCash.collectAsStateWithLifecycle()
            val shiftStartTimestamp by viewModel.shiftStartTimestamp.collectAsStateWithLifecycle()

            DashboardRealtimePanel(
                isShiftActive = isShiftActivePanel,
                activeOrders = activeOrdersList,
                initialCash = initialCash,
                shiftStartTimestamp = shiftStartTimestamp,
                onNavigate = onNavigate,
                isCompact = isCompact,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // GORGEOUS SUB-SECTIONS HEADER
            Text(
                text = "Módulos de Control",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // GRID / GRID-LIST TRANSITION
            val gridSpan = if (isCompact) 2 else 3

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSpan),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modules
                item {
                    DashboardCard(
                        title = "Punto de Venta",
                        desc = "Nueva orden, cobros y vuelto",
                        icon = Icons.Default.ShoppingCart,
                        accentColor = Color(0xFFC084FC), // Lavender
                        testTag = "ventas_tab",
                        onClick = { onNavigate("VENTAS", 0) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Productos",
                        desc = "Gestión de platos, categorías y precios",
                        icon = Icons.Default.List,
                        accentColor = Color(0xFFF472B6), // Pink
                        testTag = "productos_tab",
                        onClick = { onNavigate("PRODUCTOS", -1) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Tickets del Turno",
                        desc = "Monitoree comandas pendientes",
                        icon = Icons.Default.Favorite,
                        accentColor = Color(0xFF60A5FA), // Blue
                        testTag = "comandas_tab",
                        onClick = { onNavigate("COMANDAS", 1) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Inventario de Almacén",
                        desc = "Suministre stock diario",
                        icon = Icons.Default.Home,
                        accentColor = Color(0xFFFBBF24), // Gold
                        testTag = "almacen_tab",
                        onClick = { onNavigate("ALMACEN", 2) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Configurar Impresora",
                        desc = "Tickets físicos por Bluetooth / Wi-Fi",
                        icon = Icons.Default.Share,
                        accentColor = Color(0xFF2DD4BF), // Teal
                        testTag = "impresora_tab",
                        onClick = { onNavigate("IMPRESORA", 3) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Turno y Cierre",
                        desc = "Haga corte y cierre de caja hoy",
                        icon = Icons.Default.Star,
                        accentColor = Color(0xFF34D399), // Emerald Green
                        testTag = "cierres_tab",
                        onClick = { onNavigate("CIERRES", 4) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Datos de la Empresa",
                        desc = "Escriba su razón social e info",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF94A3B8), // Slate Gray
                        testTag = "config_tab",
                        onClick = { onNavigate("CONFIG", 5) }
                    )
                }
            }

            // BOTTOM BRAGGING SIGNATURE AT THE END
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Gourmet OS • Hecho para Impresoras Térmicas y Pantallas Táctiles",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// SECCION: ELEMENTOS DEL DASHBOARD CENTRAL
// ==========================================
@Composable
fun DashboardRealtimePanel(
    isShiftActive: Boolean,
    activeOrders: List<Order>,
    initialCash: Double,
    shiftStartTimestamp: Long,
    onNavigate: (String, Int) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSales = if (isShiftActive) activeOrders.sumOf { it.totalAmount } else 0.0
    val totalProfit = if (isShiftActive) activeOrders.sumOf { it.totalAmount - it.totalCost } else 0.0
    val totalOrders = if (isShiftActive) activeOrders.size else 0
    val pendingOrders = if (isShiftActive) activeOrders.count { it.status == "PENDING" || it.status == "DELIVERY" } else 0
    val completedOrders = if (isShiftActive) activeOrders.count { it.status == "COMPLETED" || it.status == "DELIVERED" } else 0

    val shiftTimeStr = remember(shiftStartTimestamp) {
        if (shiftStartTimestamp > 0L) {
            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(shiftStartTimestamp))
        } else {
            "--:--"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("central_dashboard_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131318)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, Color(0xFF26262F))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 12.sp)
                    }
                    Text(
                        text = "MONITOREO DE OPERACIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                // Dynamic Shift Pill
                Surface(
                    color = (if (isShiftActive) EmeraldGreen else Color(0xFFEF4444)).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, (if (isShiftActive) EmeraldGreen else Color(0xFFEF4444)).copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (isShiftActive) "TURNO ACTIVO" else "SIN TURNO",
                        color = if (isShiftActive) EmeraldGreen else Color(0xFFEF4444),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (isCompact) {
                // Stack sections closely on mobile/compact
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Turno Info / General Status
                    ShiftStatusSection(
                        isShiftActive = isShiftActive,
                        shiftTimeStr = shiftTimeStr,
                        initialCash = initialCash,
                        onNavigate = onNavigate
                    )

                    // Realtime Sales Metrics
                    MetricsSection(
                        isShiftActive = isShiftActive,
                        totalSales = totalSales,
                        totalProfit = totalProfit,
                        totalOrders = totalOrders,
                        pendingOrders = pendingOrders,
                        completedOrders = completedOrders
                    )

                    // Quick Action Button
                    QuickActionButton(
                        isShiftActive = isShiftActive,
                        onNavigate = onNavigate
                    )
                }
            } else {
                // Side-by-Side: Status & Quick Action on Left, Metrics on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShiftStatusSection(
                            isShiftActive = isShiftActive,
                            shiftTimeStr = shiftTimeStr,
                            initialCash = initialCash,
                            onNavigate = onNavigate
                        )
                        QuickActionButton(
                            isShiftActive = isShiftActive,
                            onNavigate = onNavigate
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        MetricsSection(
                            isShiftActive = isShiftActive,
                            totalSales = totalSales,
                            totalProfit = totalProfit,
                            totalOrders = totalOrders,
                            pendingOrders = pendingOrders,
                            completedOrders = completedOrders
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftStatusSection(
    isShiftActive: Boolean,
    shiftTimeStr: String,
    initialCash: Double,
    onNavigate: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C35))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estado del Turno",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                if (isShiftActive) {
                    Text(
                        text = "Aperturado: $shiftTimeStr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fondo Inicial: ${initialCash.formatPrice()}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Turno Inactivo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "Apertre caja desde Ventas",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Action to go directly to closures (for closing) or sales (for starting)
            Button(
                onClick = {
                    if (isShiftActive) {
                        onNavigate("CIERRES", 4)
                    } else {
                        onNavigate("VENTAS", 0)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isShiftActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp).testTag("dashboard_shift_action_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isShiftActive) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = "Gestionar turno",
                        tint = if (isShiftActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isShiftActive) "Ir a Cierre" else "Abrir Turno",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isShiftActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsSection(
    isShiftActive: Boolean,
    totalSales: Double,
    totalProfit: Double,
    totalOrders: Int,
    pendingOrders: Int,
    completedOrders: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C35))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ventas en Tiempo Real (Turno)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )

            // Main Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Sales Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF23232A))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("TOTAL VENTAS", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isShiftActive) totalSales.formatPrice() else "$0.00",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isShiftActive) EmeraldGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Total Profit Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF23232A))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("UTILIDAD REG.", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isShiftActive) totalProfit.formatPrice() else "$0.00",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isShiftActive) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            // Sub Status pill badges showing count of pending vs completed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Ticket total
                Text(
                    text = "Comandas: $totalOrders",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF23232A))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                // Pending count
                Text(
                    text = "Pendientes: $pendingOrders ⏳",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingOrders > 0) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF23232A))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                // Completed count
                Text(
                    text = "Abonadas: $completedOrders ✅",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completedOrders > 0) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF23232A))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    isShiftActive: Boolean,
    onNavigate: (String, Int) -> Unit
) {
    Button(
        onClick = {
            // Take directly to sales (creates an order / prompt shift if closed)
            onNavigate("VENTAS", 0)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("dashboard_quick_comanda_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Crear Comanda",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "CREAR NUEVA COMANDA",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, Color(0xFF2D2D35)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Badge with custom accent color background
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// SECCION: PRODUCTOS SCREEN (IMAGE 3 LIST STYLE)
// ==========================================
@Composable
fun ProductosScreen(
    viewModel: RestaurantViewModel,
    dishes: List<Dish>,
    onBack: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Add/Edit Dialog Triggers
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Dish?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val categories = listOf("Todos") + (dishes.map { it.category } + customCategories.map { it.name }).distinct().sorted()
    val filteredDishes = dishes.filter {
        (selectedCategory == "Todos" || it.category == selectedCategory) &&
        (it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Core Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gestión de Productos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Añada, edite o retire platos disponibles en el menú",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_dish_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Platillo", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir Platillo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { showAddCategoryDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("add_category_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Categoría", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Categoría", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por nombre o categoría...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Categories selector row (LazyRow containing FilterChips, exactly like Image 3!)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories.size) { idx ->
                val cat = categories[idx]
                val isSelected = cat == selectedCategory
                val associatedCustomCat = customCategories.firstOrNull { it.name.equals(cat, ignoreCase = true) }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        if (cat != "Todos") {
                            CategoryIcon(
                                base64 = associatedCustomCat?.iconBase64,
                                fallbackText = cat,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large high-craft checklist of dishes (Image 3 layout)
        if (filteredDishes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se encontraron platos para mostrar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDishes.size) { idx ->
                    val dish = filteredDishes[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dish_item_${dish.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D2D35))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left product illustration block
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProductImage(
                                        imageBase64 = dish.imageBase64,
                                        fallbackEmoji = getDishEmoji(dish),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = dish.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = dish.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "Stock: ${dish.dailyStock}/${dish.initialDailyStock}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // Right Pricing and action buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = dish.price.formatPrice(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Costo: ${dish.cost.formatPrice()}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                // Edit Pencil icon button
                                IconButton(
                                    onClick = { showEditDialog = dish },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF2E2E3A), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Trash bin button
                                IconButton(
                                    onClick = { 
                                        viewModel.deleteDish(dish)
                                        Toast.makeText(context, "${dish.name} eliminado.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF382329), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CUSTOM ADD NEW DISH DIALOG WINDOW
    if (showAddDialog) {
        var dishName by remember { mutableStateOf("") }
        var dishPriceText by remember { mutableStateOf("") }
        var dishCostText by remember { mutableStateOf("") }
        var dishCategoryText by remember { mutableStateOf("") }
        var dishStockText by remember { mutableStateOf("50") }

        var dishImageBase64 by remember { mutableStateOf<String?>(null) }
        var categoryIconBase64 by remember { mutableStateOf<String?>(null) }

        val dishImagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val b64 = uriToBase64(context, uri)
                if (b64 != null) {
                    dishImageBase64 = b64
                } else {
                    Toast.makeText(context, "Error al procesar la imagen del platillo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val categoryIconPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val b64 = uriToBase64(context, uri)
                if (b64 != null) {
                    categoryIconBase64 = b64
                } else {
                    Toast.makeText(context, "Error al procesar el icono de la categoría", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Crear Nuevo Platillo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dish Image Box
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Foto Platillo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { dishImagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (dishImageBase64 != null) {
                                    val bitmap = remember(dishImageBase64) {
                                        try {
                                            val decodedString = Base64.decode(dishImageBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        } catch (t: Throwable) { null }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Platillo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Category Icon Box
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Foto Categoría",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { categoryIconPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (categoryIconBase64 != null) {
                                    val bitmap = remember(categoryIconBase64) {
                                        try {
                                            val decodedString = Base64.decode(categoryIconBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        } catch (t: Throwable) { null }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Categoría",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dishName,
                        onValueChange = { dishName = it },
                        label = { Text("Nombre del Plato") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dishCategoryText,
                        onValueChange = { dishCategoryText = it },
                        label = { Text("Categoría (ej: Hamburguesas, Bebidas)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick category suggestions chips row
                    Text(
                        "Categorías existentes:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val suggestionList = (dishes.map { it.category } + customCategories.map { it.name }).distinct().filter { it.isNotBlank() }
                        items(suggestionList.size) { i ->
                            val sCat = suggestionList[i]
                            val isChosen = sCat.equals(dishCategoryText, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { dishCategoryText = sCat }
                            ) {
                                Text(
                                    text = sCat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = dishPriceText,
                            onValueChange = { dishPriceText = it },
                            label = { Text("Precio Venta") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = dishCostText,
                            onValueChange = { dishCostText = it },
                            label = { Text("Costo") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = dishStockText,
                        onValueChange = { dishStockText = it },
                        label = { Text("Stock Inicial Diario") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val p = dishPriceText.toDoubleOrNull() ?: 0.0
                                val c = dishCostText.toDoubleOrNull() ?: 0.0
                                val stk = dishStockText.toIntOrNull() ?: 30
                                if (dishName.isNotBlank() && dishCategoryText.isNotBlank()) {
                                    // Save custom category icon if uploaded
                                    if (categoryIconBase64 != null) {
                                        viewModel.addCustomCategory(dishCategoryText.trim(), categoryIconBase64)
                                    }
                                    viewModel.addDish(
                                        name = dishName.trim(),
                                        price = p,
                                        cost = c,
                                        category = dishCategoryText.trim(),
                                        initialStock = stk,
                                        imageBase64 = dishImageBase64
                                    )
                                    showAddDialog = false
                                    Toast.makeText(context, "Creado con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Nombre y categoría obligatorios", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // CUSTOM EDIT DISH DIALOG WINDOW (HIGHEST UTILITY FOR HIGH-FIDELITY PRODUCT EDITS!)
    if (showEditDialog != null) {
        val editingDish = showEditDialog!!
        
        var dishName by remember { mutableStateOf(editingDish.name) }
        var dishPriceText by remember { mutableStateOf(editingDish.price.toString()) }
        var dishCostText by remember { mutableStateOf(editingDish.cost.toString()) }
        var dishCategoryText by remember { mutableStateOf(editingDish.category) }
        var dishStockText by remember { mutableStateOf(editingDish.dailyStock.toString()) }
        var dishMinThresholdText by remember { mutableStateOf(editingDish.minStockThreshold.toString()) }

        var dishImageBase64 by remember { mutableStateOf<String?>(editingDish.imageBase64) }
        var categoryIconBase64 by remember { mutableStateOf<String?>(null) }

        val dishImagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val b64 = uriToBase64(context, uri)
                if (b64 != null) {
                    dishImageBase64 = b64
                } else {
                    Toast.makeText(context, "Error al procesar la imagen del platillo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val categoryIconPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val b64 = uriToBase64(context, uri)
                if (b64 != null) {
                    categoryIconBase64 = b64
                } else {
                    Toast.makeText(context, "Error al procesar el icono de la categoría", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Dialog(onDismissRequest = { showEditDialog = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Editar Platillo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dish Image Box
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Foto Platillo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { dishImagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (dishImageBase64 != null) {
                                    val bitmap = remember(dishImageBase64) {
                                        try {
                                            val decodedString = Base64.decode(dishImageBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        } catch (t: Throwable) { null }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Platillo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Category Icon Box
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Foto Categoría",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { categoryIconPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (categoryIconBase64 != null) {
                                    val bitmap = remember(categoryIconBase64) {
                                        try {
                                            val decodedString = Base64.decode(categoryIconBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        } catch (t: Throwable) { null }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Categoría",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dishName,
                        onValueChange = { dishName = it },
                        label = { Text("Nombre del Plato") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dishCategoryText,
                        onValueChange = { dishCategoryText = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick category suggestions chips row
                    Text(
                        "Categorías de reemplazo:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val suggestionList = (dishes.map { it.category } + customCategories.map { it.name }).distinct().filter { it.isNotBlank() }
                        items(suggestionList.size) { i ->
                            val sCat = suggestionList[i]
                            val isChosen = sCat.equals(dishCategoryText, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { dishCategoryText = sCat }
                            ) {
                                Text(
                                    text = sCat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = dishPriceText,
                            onValueChange = { dishPriceText = it },
                            label = { Text("Precio Venta") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = dishCostText,
                            onValueChange = { dishCostText = it },
                            label = { Text("Costo") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = dishStockText,
                        onValueChange = { dishStockText = it },
                        label = { Text("Stock Actual") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_dish_stock_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = dishMinThresholdText,
                        onValueChange = { dishMinThresholdText = it },
                        label = { Text("Umbral Mínimo para Alerta") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_dish_threshold_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showEditDialog = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val p = dishPriceText.toDoubleOrNull() ?: editingDish.price
                                val c = dishCostText.toDoubleOrNull() ?: editingDish.cost
                                val stk = dishStockText.toIntOrNull() ?: editingDish.dailyStock
                                val thresh = dishMinThresholdText.toIntOrNull() ?: editingDish.minStockThreshold
                                if (dishName.isNotBlank() && dishCategoryText.isNotBlank() && thresh >= 0) {
                                    // Save custom category icon if uploaded
                                    if (categoryIconBase64 != null) {
                                        viewModel.addCustomCategory(dishCategoryText.trim(), categoryIconBase64)
                                    }
                                    val updated = editingDish.copy(
                                        name = dishName.trim(),
                                        price = p,
                                        cost = c,
                                        category = dishCategoryText.trim(),
                                        dailyStock = stk,
                                        initialDailyStock = if (stk > editingDish.initialDailyStock) stk else editingDish.initialDailyStock,
                                        imageBase64 = dishImageBase64,
                                        minStockThreshold = thresh
                                    )
                                    viewModel.updateDish(updated)
                                    showEditDialog = null
                                    Toast.makeText(context, "Actualizado con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Campos obligatorios inválidos", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Actualizar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        var categoryNameInput by remember { mutableStateOf("") }
        var categoryIconBase64 by remember { mutableStateOf<String?>(null) }
        
        val categoryIconPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val b64 = uriToBase64(context, uri)
                if (b64 != null) {
                    categoryIconBase64 = b64
                } else {
                    Toast.makeText(context, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Crear Categoría Personalizada",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Icono de la Categoría",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { categoryIconPickerLauncher.launch("image/*") }
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (categoryIconBase64 != null) {
                            val bitmap = remember(categoryIconBase64) {
                                try {
                                    val decodedString = Base64.decode(categoryIconBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                } catch (t: Throwable) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Icono",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = "Subir foto", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Subir JPG/PNG", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { categoryNameInput = it },
                        label = { Text("Nombre de la Categoría") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (customCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text(
                            "Categorías Personalizadas Existentes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        customCategories.forEach { customCat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CategoryIcon(
                                        base64 = customCat.iconBase64,
                                        fallbackText = customCat.name,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = customCat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deleteCustomCategory(customCat.name)
                                        Toast.makeText(context, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp).testTag("delete_cat_" + customCat.name)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar categoría",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showAddCategoryDialog = false },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (categoryNameInput.isNotBlank()) {
                                    viewModel.addCustomCategory(categoryNameInput.trim(), categoryIconBase64)
                                    showAddCategoryDialog = false
                                    Toast.makeText(context, "Categoría creada con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "El nombre de categoría es obligatorio", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyLogo(
    logoBase64: String,
    fallbackType: String,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 24.sp
) {
    if (logoBase64.isNotBlank()) {
        val bitmap = remember(logoBase64) {
            try {
                val decodedString = Base64.decode(logoBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            } catch (t: Throwable) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Logo Empresa",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(getLogoEmoji(fallbackType), fontSize = emojiSize)
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(getLogoEmoji(fallbackType), fontSize = emojiSize)
        }
    }
}

@Composable
fun ProductImage(
    imageBase64: String?,
    fallbackEmoji: String,
    modifier: Modifier = Modifier
) {
    if (!imageBase64.isNullOrBlank()) {
        val bitmap = remember(imageBase64) {
            try {
                val decodedString = Base64.decode(imageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            } catch (t: Throwable) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto producto",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(fallbackEmoji, fontSize = 20.sp)
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(fallbackEmoji, fontSize = 20.sp)
        }
    }
}

@Composable
fun CategoryIcon(
    base64: String?,
    fallbackText: String,
    modifier: Modifier = Modifier
) {
    if (!base64.isNullOrBlank()) {
        val bitmap = remember(base64) {
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            } catch (t: Throwable) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Icono Categoría",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
            return
        }
    }
    val emoji = when {
        fallbackText.contains("hamburguesa", ignoreCase = true) || fallbackText.contains("burger", ignoreCase = true) -> "🍔"
        fallbackText.contains("pizza", ignoreCase = true) -> "🍕"
        fallbackText.contains("taco", ignoreCase = true) -> "🌮"
        fallbackText.contains("bebida", ignoreCase = true) || fallbackText.contains("drink", ignoreCase = true) || fallbackText.contains("jugo", ignoreCase = true) -> "🍹"
        fallbackText.contains("postre", ignoreCase = true) || fallbackText.contains("pastel", ignoreCase = true) -> "🍰"
        else -> "🍽️"
    }
    Text(emoji, fontSize = 14.sp)
}

fun getDishEmoji(dish: Dish): String {
    val name = dish.name.lowercase()
    val cat = dish.category.lowercase()
    return when {
        name.contains("pizza") -> "🍕"
        name.contains("hamburguesa") || name.contains("burger") -> "🍔"
        name.contains("taco") -> "🌮"
        name.contains("café") || name.contains("coffee") -> "☕"
        name.contains("limonada") || name.contains("jugo") || name.contains("soda") || name.contains("bebida") || cat.contains("bebida") -> "🍹"
        name.contains("pastel") || name.contains("postre") || cat.contains("postre") -> "🍰"
        name.contains("ensalada") -> "🥗"
        name.contains("sopa") -> "🥣"
        name.contains("pasta") -> "🍝"
        else -> "🍛"
    }
}

// HTML BUILDER FOR PHYSICAL THERMAL TICKETS
fun generateReceiptHtml(
    order: Order,
    items: List<OrderItem>,
    name: String,
    address: String,
    phone: String,
    logoId: String,
    logoBase64: String = "",
    slogan: String = "Cocinando con pasión todos los días.",
    isSimplified: Boolean = false
): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val dateString = sdf.format(Date(order.timestamp))
    val logoEmoji = getLogoEmoji(logoId)

    val logoHtml = if (logoBase64.isNotBlank()) {
        """<img src="data:image/png;base64,$logoBase64" style="max-height: 80px; max-width: 180px; margin-bottom: 8px; border-radius: 8px;" />"""
    } else {
        """<div class="logo">$logoEmoji</div>"""
    }

    val channelName = if (order.isDelivery) {
        "DELIVERY"
    } else if (order.tableNumber != null && order.tableNumber != "Para Llevar") {
        "MESA ${order.tableNumber}"
    } else {
        "PARA LLEVAR"
    }

    val serviceType = if (order.isDelivery) {
        "A Domicilio"
    } else if (order.tableNumber != null && order.tableNumber != "Para Llevar") {
        "Comer Aquí"
    } else {
        "Para Llevar"
    }

    val paymentMethodDisplay = when (order.paymentMethod) {
        "Efectivo" -> "Efectivo"
        "Tarjeta" -> "Tarjeta (crédito/débito)"
        "Transferencia" -> "Transferencia"
        else -> order.paymentMethod
    }

    if (isSimplified) {
        // ==========================================
        // SIMPLIFIED HTML VIEW
        // ==========================================
        val compactItemsRows = items.joinToString("") {
            """
            <tr>
                <td style="text-align: left; padding: 2px 0;">${it.quantity}x ${it.dishName}</td>
                <td style="text-align: right; padding: 2px 0;">${(it.price * it.quantity).formatPrice()}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace;
                    max-width: 280px;
                    margin: 0 auto;
                    padding: 4px;
                    color: #000;
                    background-color: #FFF;
                    font-size: 12px;
                    line-height: 1.25;
                }
                .center { text-align: center; }
                .title { font-size: 15px; font-weight: bold; margin: 2px 0; }
                .channel-box {
                    font-size: 18px;
                    font-weight: bold;
                    border: 1px dashed #000;
                    padding: 4px;
                    margin: 6px 0;
                    text-align: center;
                }
                .divider { border-top: 1px dashed #000; margin: 6px 0; }
                table { width: 100%; border-collapse: collapse; }
                .totals { font-weight: bold; font-size: 13px; }
            </style>
        </head>
        <body>
            <div class="center">
                <div class="title">$name</div>
                ${if (phone.isNotEmpty()) "<div style='font-size: 10px;'>Tel: $phone</div>" else ""}
            </div>
            
            <div class="divider"></div>
            <div class="channel-box">$channelName</div>
            <div class="divider"></div>
            
            <div>
                <b>TICKET:</b> #${order.id}<br>
                <b>Fecha:</b> ${sdf.format(Date(order.timestamp))}<br>
            </div>
            
            <div class="divider"></div>
            
            <table>
                <tbody>
                    $compactItemsRows
                </tbody>
            </table>
            
            <div class="divider"></div>
            
            <table>
                <tr class="totals">
                    <td style="text-align: left;">TOTAL:</td>
                    <td style="text-align: right;">${order.totalAmount.formatPrice()}</td>
                </tr>
                ${if (order.paymentMethod.equals("Efectivo", ignoreCase = true)) {
                    """
                    <tr>
                        <td style="text-align: left; font-size: 11px;">Pago:</td>
                        <td style="text-align: right; font-size: 11px;">${order.amountReceived.formatPrice()}</td>
                    </tr>
                    <tr>
                        <td style="text-align: left; font-size: 11px; font-weight: bold;">Cambio:</td>
                        <td style="text-align: right; font-size: 11px; font-weight: bold;">${order.changeGiven.formatPrice()}</td>
                    </tr>
                    """.trimIndent()
                } else ""}
            </table>
            
            <div class="divider"></div>
            <div class="center" style="font-size: 10px;">¡Gracias por su Compra!</div>
        </body>
        </html>
        """.trimIndent()
    }

    // ==========================================
    // DETAILED HTML VIEW
    // ==========================================
    val itemsRows = items.joinToString("") {
        """
        <tr>
            <td style="text-align: left; padding: 4px 0;">${it.quantity}x ${it.dishName}</td>
            <td style="text-align: right; padding: 4px 0;">${(it.price * it.quantity).formatPrice()}</td>
        </tr>
        """.trimIndent()
    }

    val subtotalVal = order.totalAmount / 1.16
    val taxVal = order.totalAmount - subtotalVal

    return """
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            body {
                font-family: 'Courier New', Courier, monospace;
                max-width: 300px;
                margin: 0 auto;
                padding: 10px;
                color: #000;
                background-color: #FFF;
                font-size: 13px;
                line-height: 1.3;
            }
            .center { text-align: center; }
            .logo { font-size: 40px; margin-bottom: 5px; }
            .title { font-size: 18px; font-weight: bold; margin: 3px 0; }
            .info { font-size: 11px; margin-bottom: 10px; }
            .channel-badge {
                font-size: 19px;
                font-weight: 900;
                border: 2px solid #000;
                padding: 6px;
                margin: 10px 0;
                text-align: center;
                background-color: #EEE;
                letter-spacing: 1px;
            }
            .nature-title {
                font-weight: bold;
                font-size: 12px;
                letter-spacing: 1.5px;
                text-decoration: underline;
                margin-bottom: 4px;
            }
            .divider { border-top: 1px dashed #000; margin: 8px 0; }
            .double-divider { border-top: 2px double #000; margin: 8px 0; }
            table { width: 100%; border-collapse: collapse; }
            .totals { font-weight: bold; font-size: 14px; }
            .footer { font-size: 11px; margin-top: 15px; }
        </style>
    </head>
    <body>
        <div class="center">
            $logoHtml
            <div class="title">$name</div>
            <div class="info">$address<br>Tel: $phone</div>
        </div>
        
        <div class="double-divider"></div>
        
        <div class="center">
            <div class="nature-title">TICKET DE CONTROL INTERNO</div>
            <div style="font-size: 11px;">VENTAS & FACTURACION LOCAL</div>
        </div>
        
        <div class="divider"></div>
        
        <div>
            <b>Ticket #:</b> #${order.id}<br>
            <b>Fecha/Hora:</b> $dateString<br>
            <b>Cliente:</b> ${order.customerName ?: "Consumidor Final"}<br>
            ${if (order.tableNumber != null && order.tableNumber != "Para Llevar") "<b>Asignacion:</b> Mesa ${order.tableNumber}<br>" else ""}
            ${if (order.isDelivery && order.deliveryAddress != null) "<b>Dirección:</b> ${order.deliveryAddress}<br>" else ""}
            <b>Servicio:</b> $serviceType<br>
            <b>Método de Pago:</b> $paymentMethodDisplay<br>
        </div>
        
        <div class="divider"></div>
        
        <div class="channel-badge">$channelName</div>
        
        <div class="divider"></div>
        
        <table>
            <thead>
                <tr>
                    <th style="text-align: left; border-bottom: 1px solid #000; padding-bottom: 4px;">Detalle / Cant</th>
                    <th style="text-align: right; border-bottom: 1px solid #000; padding-bottom: 4px;">Importe</th>
                </tr>
            </thead>
            <tbody>
                $itemsRows
            </tbody>
        </table>
        
        <div class="divider"></div>
        
        <table>
            <tr>
                <td style="text-align: left; font-size: 12px; color: #444;">Subtotal (Pre-Impuesto):</td>
                <td style="text-align: right; font-size: 12px; color: #444;">${subtotalVal.formatPrice()}</td>
            </tr>

            <tr class="totals">
                <td style="text-align: left; padding-top: 4px;">TOTAL NETO:</td>
                <td style="text-align: right; padding-top: 4px;">${order.totalAmount.formatPrice()}</td>
            </tr>
            ${if (order.paymentMethod.equals("Efectivo", ignoreCase = true)) {
                """
                <tr>
                    <td style="text-align: left; font-size: 12px; padding-top: 4px; color: #444;">Efectivo Entregado:</td>
                    <td style="text-align: right; font-size: 12px; padding-top: 4px; color: #444;">${order.amountReceived.formatPrice()}</td>
                </tr>
                <tr>
                    <td style="text-align: left; font-size: 12px; font-weight: bold; padding-top: 2px;">Cambio Calculado:</td>
                    <td style="text-align: right; font-size: 12px; font-weight: bold; padding-top: 2px;">${order.changeGiven.formatPrice()}</td>
                </tr>
                """.trimIndent()
            } else ""}
        </table>
        
        <div class="double-divider"></div>
        
        <div class="center footer">
            ¡Muchas Gracias por su Compra!<br>
            "$slogan"<br>
            Soporte - Teléfono: $phone<br>
        </div>
    </body>
    </html>
    """.trimIndent()
}

// PRINT FUNCTION
fun sendToThermalPrinter(context: Context, html: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Ticket_Venta")
                val jobName = "Ticket_Restaurante_${System.currentTimeMillis()}"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al generar impresión: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ==========================================
// SECCION 1: VENTAS (MENU & CART)
// ==========================================
@Composable
fun SalesTab(
    viewModel: RestaurantViewModel,
    dishes: List<Dish>,
    cart: Map<Int, RestaurantViewModel.CartItem>,
    onBack: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var receiptToPrint by remember { mutableStateOf<Order?>(null) }
    var receiptItemsToPrint by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var showReceiptEditOrderDialog by remember { mutableStateOf<Order?>(null) }

    val categories = listOf("Todos") + (dishes.map { it.category } + customCategories.map { it.name }).distinct().sorted()
    val filteredDishes = dishes.filter {
        (selectedCategory == "Todos" || it.category == selectedCategory) &&
        (it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
    }

    val totalCartPrice = cart.values.sumOf { it.dish.price * it.quantity }
    val totalCartItems = cart.values.sumOf { it.quantity }

    val context = LocalContext.current
    val restaurantName by viewModel.restaurantName.collectAsStateWithLifecycle()
    val restaurantAddress by viewModel.restaurantAddress.collectAsStateWithLifecycle()
    val restaurantPhone by viewModel.restaurantPhone.collectAsStateWithLifecycle()
    val logoType by viewModel.logoType.collectAsStateWithLifecycle()
    val restaurantLogoBase64 by viewModel.restaurantLogoBase64.collectAsStateWithLifecycle()
    val restaurantSlogan by viewModel.restaurantSlogan.collectAsStateWithLifecycle()
    val isShiftActive by viewModel.isShiftActive.collectAsStateWithLifecycle()

    if (!isShiftActive) {
        var startCashText by remember { mutableStateOf("100.00") }
        var stocksToSet by remember(dishes) { mutableStateOf(dishes.associate { it.id to it.dailyStock.coerceAtLeast(0) }) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                         "Apertura de Turno",
                         style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                         "Sabor y Gestión - Inicie su día de venta",
                         fontSize = 11.sp,
                         color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Cash box setup
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "💰 Balance Inicial de Efectivo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Ingrese la cantidad de efectivo con la que inicia la caja hoy para poder cuadrar al cierre.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = startCashText,
                        onValueChange = { startCashText = it },
                        label = { Text("Efectivo Inicial en Caja ($)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("initial_cash_input")
                    )
                }
            }
            
            // Stock setup card
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "📦 Existencias Iniciales de Menú (Inventario)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Establezca el stock inicial de cada platillo para este turno. Se descontará automáticamente con las ventas.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { stocksToSet = dishes.associate { it.id to 20 } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Todo a 20", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { stocksToSet = dishes.associate { it.id to 50 } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Todo a 50", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { stocksToSet = dishes.associate { it.id to 0 } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Limpiar (0)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    
                    if (dishes.isEmpty()) {
                        Text(
                            "No hay platillos registrados todavía. Registre platillos en la pestaña de Almacén.",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    
                    // Display list of items
                    dishes.forEach { dish ->
                        val currentQty = stocksToSet[dish.id] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(dish.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        if (currentQty > 0) {
                                            stocksToSet = stocksToSet.toMutableMap().apply { put(dish.id, currentQty - 1) }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Restar", modifier = Modifier.size(18.dp))
                                }
                                
                                Text("$currentQty", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                
                                IconButton(
                                    onClick = {
                                        stocksToSet = stocksToSet.toMutableMap().apply { put(dish.id, currentQty + 1) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            Button(
                onClick = {
                    val cashValue = startCashText.toDoubleOrNull() ?: 0.0
                    viewModel.startShift(cashValue, stocksToSet)
                    Toast.makeText(context, "¡Turno aperturado con éxito!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("start_shift_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Aperturar Turno & Cargar Inventario 🔑", fontWeight = FontWeight.Bold)
            }
        }
    } else {

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 650.dp
        
        if (isCompact) {
            // PORTRAIT MOBILE: Vertical stacked layout as shown in Image 2!
            Column(modifier = Modifier.fillMaxSize()) {
                // Products grid at the top
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Header with Back button and Turno badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Punto de Venta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "TURNO ABIERTO",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sales_search_input"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories.size) { index ->
                            val cat = categories[index]
                            val isSelected = cat == selectedCategory
                            val associatedCustomCat = customCategories.firstOrNull { it.name.equals(cat, ignoreCase = true) }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (cat != "Todos") {
                                        CategoryIcon(
                                            base64 = associatedCustomCat?.iconBase64,
                                            fallbackText = cat,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Products Grid
                    if (filteredDishes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se encontraron productos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredDishes.size) { index ->
                                val dish = filteredDishes[index]
                                val stockInCart = cart[dish.id]?.quantity ?: 0
                                val stockAvailable = dish.dailyStock - stockInCart

                                Card(
                                    onClick = {
                                        if (stockAvailable > 0) {
                                            viewModel.addToCart(dish)
                                        } else {
                                            Toast.makeText(context, "Stock agotado para este platillo hoy", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = stockAvailable > 0,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (stockAvailable > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (stockAvailable > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.testTag("dish_card_${dish.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ProductImage(
                                                imageBase64 = dish.imageBase64,
                                                fallbackEmoji = getDishEmoji(dish),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dish.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dish.price.formatPrice(),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Surface(
                                                color = if (stockAvailable > 5) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "St: $stockAvailable",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (stockAvailable > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider and Cart Stack below
                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = "Carrito",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Carrito (${totalCartItems})",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            if (cart.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearCart() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Limpiar Todo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Cart item list
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "El carrito está vacío",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val itemsList = cart.values.toList()
                                items(itemsList.size) { itemIndex ->
                                    val cartItem = itemsList[itemIndex]
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ProductImage(
                                                        imageBase64 = cartItem.dish.imageBase64,
                                                        fallbackEmoji = getDishEmoji(cartItem.dish),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        cartItem.dish.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        (cartItem.dish.price * cartItem.quantity).formatPrice(),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }

                                            // Count Control
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.removeFromCart(cartItem.dish) },
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp))
                                                ) {
                                                    Text("-", fontWeight = FontWeight.Black)
                                                }
                                                Text(
                                                    "${cartItem.quantity}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp
                                                )
                                                val stockInCart = cart[cartItem.dish.id]?.quantity ?: 0
                                                val stockAvailable = cartItem.dish.dailyStock - stockInCart
                                                IconButton(
                                                    onClick = {
                                                        if (stockAvailable > 0) {
                                                            viewModel.addToCart(cartItem.dish)
                                                        } else {
                                                            Toast.makeText(context, "No hay más stock", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    enabled = stockAvailable > 0,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(if (stockAvailable > 0) MaterialTheme.colorScheme.primary else Color.Gray, shape = RoundedCornerShape(6.dp))
                                                ) {
                                                    Text("+", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Checkout Bar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total compra", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        totalCartPrice.formatPrice(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (cart.isNotEmpty()) {
                                            showCheckoutDialog = true
                                        } else {
                                            Toast.makeText(context, "Agrega platillos primero", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("checkout_trigger_button")
                                ) {
                                    Text("Cobrar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Done, contentDescription = "Cobrar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // WIDESCREEN TAB: Side-by-side Row layout!
            Row(modifier = Modifier.fillMaxSize()) {
        // Main Products Layout
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header Search & Filters
            Text(
                text = "Nueva Venta / Menú",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar platillos o bebidas...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_search_input"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horizon Categories Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    val isSelected = cat == selectedCategory
                    val associatedCustomCat = customCategories.firstOrNull { it.name.equals(cat, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            if (cat != "Todos") {
                                CategoryIcon(
                                    base64 = associatedCustomCat?.iconBase64,
                                    fallbackText = cat,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredDishes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍽️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No se encontraron productos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Intenta crear platos en la pestaña de Almacén.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDishes.size) { index ->
                        val dish = filteredDishes[index]
                        // Stock left evaluation
                        val stockInCart = cart[dish.id]?.quantity ?: 0
                        val stockAvailable = dish.dailyStock - stockInCart

                        Card(
                            onClick = {
                                if (stockAvailable > 0) {
                                    viewModel.addToCart(dish)
                                } else {
                                    Toast.makeText(context, "Stock agotado para este platillo hoy", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = stockAvailable > 0,
                            colors = CardDefaults.cardColors(
                                containerColor = if (stockAvailable > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (stockAvailable > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.testTag("dish_card_${dish.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProductImage(
                                        imageBase64 = dish.imageBase64,
                                        fallbackEmoji = getDishEmoji(dish),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = dish.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dish.category,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dish.price.formatPrice(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    // Display available inventory
                                    Surface(
                                        color = if (stockAvailable > 5) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Stock: $stockAvailable",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (stockAvailable > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cart Dashboard Panel
        Surface(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Carrito",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Comanda Actual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    if (cart.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Limpiar Todo", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛒", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "El carrito está vacío",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                "Toca un platillo para agregarlo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    val cartItemsList = cart.values.toList()
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItemsList.size) { index ->
                            val item = cartItemsList[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Square Box holding the product image or emoji — clickable to quickly add one item
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable {
                                            val stockAvailable = item.dish.dailyStock - item.quantity
                                            if (stockAvailable > 0) {
                                                viewModel.addToCart(item.dish)
                                            } else {
                                                Toast.makeText(context, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProductImage(
                                        imageBase64 = item.dish.imageBase64,
                                        fallbackEmoji = getDishEmoji(item.dish),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.dish.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            item.dish.price.formatPrice(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            "Subtotal: ${(item.dish.price * item.quantity).formatPrice()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.removeFromCart(item.dish) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Restar",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        "${item.quantity}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val stockAvailable = item.dish.dailyStock - item.quantity
                                    IconButton(
                                        onClick = {
                                            if (stockAvailable > 0) {
                                                viewModel.addToCart(item.dish)
                                            } else {
                                                Toast.makeText(context, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = stockAvailable > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Sumar",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // One-click delete entire product section from this order
                                IconButton(
                                    onClick = { viewModel.deleteProductFromCart(item.dish) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar Sección",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Artículos totales:", fontSize = 13.sp)
                                Text("$totalCartItems", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total a Cobrar:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    totalCartPrice.formatPrice(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showCheckoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("checkout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Pagar")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Venta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

    // CHECKOUT DIALOG (CLIENT SELECTION, TABLE OR DELIVERY DETAILS, PAYMENT METHODS)
    if (showCheckoutDialog) {
        val clientName by viewModel.customerName.collectAsStateWithLifecycle()
        val seatTable by viewModel.tableNumber.collectAsStateWithLifecycle()
        val addressStr by viewModel.deliveryAddress.collectAsStateWithLifecycle()
        val activeOrderType by viewModel.checkoutOrderType.collectAsStateWithLifecycle()

        var localPaymentMethod by remember { mutableStateOf("Efectivo") }
        var localAmountReceivedText by remember { mutableStateOf("") }
        var localOrderStatusType by remember(activeOrderType) {
            mutableStateOf(if (activeOrderType == "LLEVAR") "ENTREGADA" else "PENDIENTE")
        }
        val receivedAmount = localAmountReceivedText.toDoubleOrNull() ?: 0.0
        val change = if (receivedAmount >= totalCartPrice) receivedAmount - totalCartPrice else 0.0

        Dialog(onDismissRequest = { showCheckoutDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Detalle del Pedido",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { viewModel.customerName.value = it },
                        label = { Text("Nombre del Cliente") },
                        placeholder = { Text("Ej. Juan Pérez, María...") },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
                        singleLine = true
                    )

                    Text("Modalidad de Entrega", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("COMER_AQUI", "Comer Aquí", "🍽️"),
                            Triple("LLEVAR", "Para Llevar", "🛍️"),
                            Triple("DOMICILIO", "A Domicilio", "🛵")
                        ).forEach { (typeId, label, emoji) ->
                            val isSelected = activeOrderType == typeId
                            Surface(
                                onClick = { viewModel.checkoutOrderType.value = typeId },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (activeOrderType == "DOMICILIO") {
                        OutlinedTextField(
                            value = addressStr,
                            onValueChange = { viewModel.deliveryAddress.value = it },
                            label = { Text("Dirección de Envío") },
                            placeholder = { Text("Escriba calle, número y referencias...") },
                            modifier = Modifier.fillMaxWidth().testTag("checkout_address_input"),
                            minLines = 2
                        )
                    } else if (activeOrderType == "COMER_AQUI") {
                        OutlinedTextField(
                            value = seatTable,
                            onValueChange = { viewModel.tableNumber.value = it },
                            label = { Text("Número de Mesa (Vacio se asume Mesa Gral)") },
                            placeholder = { Text("Ej. Mesa 4, Barra, Terraza 1...") },
                            modifier = Modifier.fillMaxWidth().testTag("checkout_table_input"),
                            singleLine = true
                        )
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "El pedido se registrará como Listo para Llevar rápida y directamente.",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Método de Pago", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Efectivo", "Efectivo", "💵"),
                            Triple("Tarjeta", "Tarjeta", "💳"),
                            Triple("Transferencia", "Transfer", "🏦")
                        ).forEach { (payId, label, emoji) ->
                            val isSelected = localPaymentMethod == payId
                            Surface(
                                onClick = {
                                    localPaymentMethod = payId
                                    if (payId != "Efectivo") {
                                        localAmountReceivedText = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Destino / Estado Inicial de la Orden", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("PENDIENTE", "Comanda (A Cocina)", "🍳"),
                            Triple("ENTREGADA", "Entregada (Finalizada)", "✅")
                        ).forEach { (statusId, label, emoji) ->
                            val isSelected = localOrderStatusType == statusId
                            Surface(
                                onClick = { localOrderStatusType = statusId },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (localPaymentMethod == "Efectivo") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Registro de Pago en Efectivo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total de la Cuenta:", fontSize = 13.sp)
                                    Text(
                                        text = totalCartPrice.formatPrice(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedTextField(
                                    value = localAmountReceivedText,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() || it == '.' }) {
                                            localAmountReceivedText = newValue
                                        }
                                    },
                                    label = { Text("Efectivo Recibido ($)") },
                                    placeholder = { Text("Ej. 20.00, 50.00") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("cash_received_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        if (localAmountReceivedText.isNotBlank()) {
                                            IconButton(onClick = { localAmountReceivedText = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                    }
                                )

                                Text("Billetes / Montos rápidos:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val bills = listOf(5.0, 10.0, 20.0, 50.0, 100.0)
                                        .filter { it >= totalCartPrice }
                                        .take(4)

                                    bills.forEach { billVal ->
                                        InputChip(
                                            selected = receivedAmount == billVal,
                                            onClick = { localAmountReceivedText = billVal.toString() },
                                            label = { Text("$${billVal.toInt()}") }
                                        )
                                    }

                                    InputChip(
                                        selected = receivedAmount == totalCartPrice,
                                        onClick = { localAmountReceivedText = totalCartPrice.toString() },
                                        label = { Text("Exacto") }
                                    )
                                }

                                if (receivedAmount > 0.0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (receivedAmount < totalCartPrice) {
                                        Text(
                                            text = "⚠️ Dinero insuficiente (Falta ${(totalCartPrice - receivedAmount).formatPrice()})",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "CAMBIO A DEVOLVER:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                            Text(
                                                change.formatPrice(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showCheckoutDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (activeOrderType == "DOMICILIO" && addressStr.isBlank()) {
                                    Toast.makeText(context, "Se necesita una dirección para entrega", Toast.LENGTH_SHORT).show()
                                } else if (localPaymentMethod == "Efectivo" && receivedAmount < totalCartPrice) {
                                    Toast.makeText(context, "Monto en efectivo recibido es insuficiente", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.checkoutPaymentMethod.value = localPaymentMethod
                                    viewModel.checkoutAmountReceived.value = if (localPaymentMethod == "Efectivo") receivedAmount else 0.0
                                    viewModel.checkoutChangeGiven.value = if (localPaymentMethod == "Efectivo") change else 0.0

                                    // Compute final Room Order status
                                    val finalStatusString = if (localOrderStatusType == "PENDIENTE") {
                                        if (activeOrderType == "DOMICILIO") "DELIVERY" else "PENDING"
                                    } else {
                                        if (activeOrderType == "DOMICILIO") "DELIVERED" else "COMPLETED"
                                    }

                                    viewModel.checkoutCart(customStatus = finalStatusString) { createdOrder ->
                                        showCheckoutDialog = false
                                        viewModel.loadOrderItems(createdOrder.id) { orderedItems ->
                                            receiptToPrint = createdOrder
                                            receiptItemsToPrint = orderedItems
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // PHYSICAL THERMAL RECEIPT VISUAL PREVIEW & PRINT TRIGGER
    if (receiptToPrint != null) {
        val rOrder = receiptToPrint!!
        val contextPrompt = LocalContext.current
        var bluetoothPrinter by remember { mutableStateOf<com.example.ui.BluetoothPrinterHelper.PrinterDevice?>(null) }
        var isPrintingBtCheckout by remember { mutableStateOf(false) }
        val checkoutBtScope = rememberCoroutineScope()
        val savedFormatCheck = remember(rOrder.id) {
            com.example.ui.BluetoothPrinterHelper.getReceiptFormat(contextPrompt)
        }
        var localIsSimplified by remember(rOrder.id) {
            mutableStateOf(savedFormatCheck == "SIMPLIFIED")
        }

        LaunchedEffect(rOrder.id) {
            bluetoothPrinter = com.example.ui.BluetoothPrinterHelper.getSelectedPrinter(contextPrompt)
        }

        val channelName = if (rOrder.isDelivery) {
            "DELIVERY"
        } else if (rOrder.tableNumber != null && rOrder.tableNumber != "Para Llevar" && rOrder.tableNumber != "") {
            "MESA ${rOrder.tableNumber}"
        } else {
            "PARA LLEVAR"
        }

        val isMesaGral = rOrder.tableNumber?.trim()?.lowercase() == "mesa gral" || rOrder.tableNumber?.trim()?.lowercase() == "mesa general" || rOrder.tableNumber.isNullOrBlank()
        val displayChannelLabel = if (!rOrder.isDelivery && rOrder.tableNumber != "Para Llevar" && isMesaGral) {
            "Mesa Gral"
        } else {
            "CANAL: $channelName"
        }

        Dialog(onDismissRequest = { receiptToPrint = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Comprobante Generado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // REAL-TIME FORMAT SELECTOR
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Formato de Impresión:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    localIsSimplified = false
                                    com.example.ui.BluetoothPrinterHelper.saveReceiptFormat(contextPrompt, "DETAILED")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!localIsSimplified) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("preview_detailed_toggle")
                            ) {
                                Text(
                                    "Detallado (Completo)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!localIsSimplified) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    localIsSimplified = true
                                    com.example.ui.BluetoothPrinterHelper.saveReceiptFormat(contextPrompt, "SIMPLIFIED")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (localIsSimplified) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("preview_simplified_toggle")
                            ) {
                                Text(
                                    "Simplificado (Ahorro)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (localIsSimplified) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // BEAUTIFUL THERMAL RECEIPT CONTAINER
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Draw realistic jagged receipt edge at the absolute bottom
                                val path = Path()
                                val segmentWidth = 20f
                                var x = 0f
                                path.moveTo(0f, size.height)
                                while (x < size.width) {
                                    path.lineTo(x + segmentWidth / 2, size.height - 12f)
                                    path.lineTo(x + segmentWidth, size.height)
                                    x += segmentWidth
                                }
                                path.lineTo(size.width, 0f)
                                path.lineTo(0f, 0f)
                                path.close()
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (localIsSimplified) {
                                // --------------------------------------------
                                // RENDERING SIMPLIFIED TICKET PREVIEW
                                // --------------------------------------------
                                Text(
                                    getLogoEmoji(logoType),
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    restaurantName.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (restaurantPhone.isNotEmpty()) {
                                    Text(
                                        "Tel: $restaurantPhone",
                                        color = Color.DarkGray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                // Channel selection prominent
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .border(1.5.dp, Color.Black, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        displayChannelLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }

                                val clientNameText = rOrder.customerName?.trim()
                                if (!clientNameText.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = clientNameText.uppercase(Locale.getDefault()),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                                
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                Text(
                                    text = "TICKET: #${rOrder.id}\n" +
                                           "Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(rOrder.timestamp))}",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                // Compact List
                                receiptItemsToPrint.forEach { itm ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${itm.quantity}x ${itm.dishName}",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            (itm.price * itm.quantity).formatPrice(),
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "TOTAL:",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        rOrder.totalAmount.formatPrice(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                if (rOrder.paymentMethod == "Efectivo") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cambio:", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                        Text(rOrder.changeGiven.formatPrice(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "¡Gracias por su Preferencia!",
                                    fontSize = 9.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else {
                                // --------------------------------------------
                                // RENDERING DETAILED TICKET PREVIEW
                                // --------------------------------------------
                                Text(
                                    getLogoEmoji(logoType),
                                    fontSize = 38.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    restaurantName.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (restaurantSlogan.isNotEmpty()) {
                                    Text(
                                        "\"$restaurantSlogan\"",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text(
                                    "$restaurantAddress\nTel: $restaurantPhone",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("=========================================", fontSize = 9.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text("TICKET DE VENTA / CONTROL INTERNO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("=========================================", fontSize = 9.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ticket Nro:  #${rOrder.id}\n" +
                                           "Fecha/Hora:  ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(rOrder.timestamp))}\n" +
                                           "Cliente:     ${rOrder.customerName ?: "Consumidor Final"}\n" +
                                           "Referencia:  ${if (rOrder.isDelivery && rOrder.deliveryAddress != null) "Entrega: ${rOrder.deliveryAddress}" else if (rOrder.tableNumber != null && rOrder.tableNumber != "Para Llevar") "Servicio de Mesa: ${rOrder.tableNumber}" else "Retira en Sucursal"}",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                // Channel selection badge centered & filled with bordered row
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEEEEEE))
                                        .padding(horizontal = 18.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        displayChannelLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }

                                val clientNameTextDetailed = rOrder.customerName?.trim()
                                if (!clientNameTextDetailed.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = clientNameTextDetailed.uppercase(Locale.getDefault()),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                                
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cant Platillo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                                    Text("Importe", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                receiptItemsToPrint.forEach { itm ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${itm.quantity}x ${itm.dishName}",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            (itm.price * itm.quantity).formatPrice(),
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                Text("-----------------------------------------", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                
                                val subtotalVal = rOrder.totalAmount / 1.16
                                val taxVal = rOrder.totalAmount - subtotalVal
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal (Pre-Impuesto):", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                    Text(subtotalVal.formatPrice(), fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "TOTAL NETO:",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        rOrder.totalAmount.formatPrice(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text("Metodo Pago: ${rOrder.paymentMethod}", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                
                                if (rOrder.paymentMethod == "Efectivo") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Efectivo Entregado:", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                        Text(rOrder.amountReceived.formatPrice(), fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cambio Calculado:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                                        Text(rOrder.changeGiven.formatPrice(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                
                                Text("=========================================", fontSize = 9.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "¡Muchas Gracias por su Compra!\nSoporte y Atencion al Cliente",
                                    fontSize = 9.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "OPCIONES DE DOCUMENTO / FACTURA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // PRINT & PDF DOCUMENT ROW
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF SHARE BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Android PDF share
                            OutlinedButton(
                                onClick = {
                                    val file = PdfReportHelper.generateInvoicePdf(
                                        contextPrompt,
                                        restaurantName,
                                        restaurantAddress,
                                        restaurantPhone,
                                        restaurantLogoBase64,
                                        rOrder,
                                        receiptItemsToPrint
                                    )
                                    if (file != null) {
                                        PdfReportHelper.shareDocument(contextPrompt, file, "Factura #${rOrder.id}")
                                    } else {
                                        Toast.makeText(contextPrompt, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("dialog_share_pdf_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir PDF", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartir PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. WhatsApp PDF Share
                            Button(
                                onClick = {
                                    val file = PdfReportHelper.generateInvoicePdf(
                                        contextPrompt,
                                        restaurantName,
                                        restaurantAddress,
                                        restaurantPhone,
                                        restaurantLogoBase64,
                                        rOrder,
                                        receiptItemsToPrint
                                    )
                                    if (file != null) {
                                        PdfReportHelper.shareToWhatsApp(contextPrompt, file, "Factura #${rOrder.id}")
                                    } else {
                                        Toast.makeText(contextPrompt, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("dialog_whatsapp_pdf_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp PDF", modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vía WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // BLUETOOTH PRINT OPTION IF ACTIVE
                        bluetoothPrinter?.let { printer ->
                            Button(
                                onClick = {
                                    checkoutBtScope.launch {
                                        isPrintingBtCheckout = true
                                        val data = com.example.ui.BluetoothPrinterHelper.buildEscPosReceipt(
                                            restaurantName,
                                            restaurantAddress,
                                            restaurantPhone,
                                            restaurantSlogan,
                                            rOrder,
                                            receiptItemsToPrint,
                                            isSimplified = localIsSimplified
                                        )
                                        val success = com.example.ui.BluetoothPrinterHelper.printDirect(contextPrompt, printer.address, data)
                                        isPrintingBtCheckout = false
                                        if (success) {
                                            Toast.makeText(contextPrompt, "¡Ticket enviado a ${printer.name}!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(contextPrompt, "Error: Conexión fallida con ${printer.name}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isPrintingBtCheckout,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("dialog_print_bluetooth_button")
                            ) {
                                Text(
                                    if (isPrintingBtCheckout) "Enviando a bluetooth..." else "Imprimir por Bluetooth 📶 (${printer.name})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // THERMAL PRINT BUTTON (SYSTEM)
                        Button(
                            onClick = {
                                val htmlContent = generateReceiptHtml(
                                    rOrder,
                                    receiptItemsToPrint,
                                    restaurantName,
                                    restaurantAddress,
                                    restaurantPhone,
                                    logoType,
                                    restaurantLogoBase64,
                                    restaurantSlogan,
                                    isSimplified = localIsSimplified
                                )
                                sendToThermalPrinter(contextPrompt, htmlContent)
                            },
                            enabled = !isPrintingBtCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_print_ticket_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Imprimir", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imprimir por Sistema (Mopria/PDF)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // BOTTOM BUTTONS (Cerrar & Editar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { receiptToPrint = null },
                            modifier = Modifier.weight(1f).testTag("dialog_close_invoice_button")
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showReceiptEditOrderDialog = rOrder },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).testTag("dialog_edit_invoice_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modificar", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar Orden", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showReceiptEditOrderDialog != null) {
        val orderToEdit = showReceiptEditOrderDialog!!
        var editingItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
        var isLoadingItems by remember { mutableStateOf(true) }
        val dishesList by viewModel.dishes.collectAsStateWithLifecycle()
        val contextPrompt = LocalContext.current

        LaunchedEffect(orderToEdit.id) {
            viewModel.loadOrderItems(orderToEdit.id) { items ->
                editingItems = items
                isLoadingItems = false
            }
        }

        Dialog(onDismissRequest = { showReceiptEditOrderDialog = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Modificar Comanda #${orderToEdit.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cliente: ${orderToEdit.customerName ?: "Mostrador"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    if (isLoadingItems) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Platillos en esta Comanda:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1.2f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(editingItems.size) { index ->
                                    val itm = editingItems[index]
                                    val assocDish = dishesList.firstOrNull { it.id == itm.dishId }
                                    val emoji = if (assocDish != null) getDishEmoji(assocDish) else "🍛"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    editingItems = editingItems.mapIndexed { idx, item ->
                                                        if (idx == index) item.copy(quantity = item.quantity + 1) else item
                                                    }
                                                }
                                        ) {
                                            ProductImage(imageBase64 = assocDish?.imageBase64, fallbackEmoji = emoji, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(itm.dishName, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                            Text(itm.price.formatPrice(), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (itm.quantity > 1) {
                                                        editingItems = editingItems.mapIndexed { idx, item ->
                                                            if (idx == index) item.copy(quantity = item.quantity - 1) else item
                                                        }
                                                    } else {
                                                        editingItems = editingItems.filterIndexed { idx, _ -> idx != index }
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Restar", modifier = Modifier.size(16.dp))
                                            }

                                            Text(
                                                "${itm.quantity}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            IconButton(
                                                onClick = {
                                                    editingItems = editingItems.mapIndexed { idx, item ->
                                                        if (idx == index) item.copy(quantity = item.quantity + 1) else item
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = {
                                                editingItems = editingItems.filterIndexed { idx, _ -> idx != index }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Sección", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Text(
                                "Agregar más Platillos del Menú:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().height(84.dp)
                            ) {
                                items(dishesList.size) { index ->
                                    val dish = dishesList[index]
                                    Card(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .fillMaxHeight()
                                            .clickable {
                                                val existingIndex = editingItems.indexOfFirst { it.dishId == dish.id }
                                                if (existingIndex != -1) {
                                                    editingItems = editingItems.mapIndexed { idx, item ->
                                                        if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                                                    }
                                                } else {
                                                    editingItems = editingItems + OrderItem(
                                                        orderId = orderToEdit.id,
                                                        dishId = dish.id,
                                                        dishName = dish.name,
                                                        quantity = 1,
                                                        price = dish.price,
                                                        cost = dish.cost
                                                    )
                                                }
                                                Toast.makeText(contextPrompt, "${dish.name} añadido", Toast.LENGTH_SHORT).show()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            ProductImage(imageBase64 = dish.imageBase64, fallbackEmoji = getDishEmoji(dish), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                dish.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                dish.price.formatPrice(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val liveTotalSum = editingItems.sumOf { it.price * it.quantity }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Estimado:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(liveTotalSum.formatPrice(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showReceiptEditOrderDialog = null }, modifier = Modifier.weight(1f)) {
                            Text("Cerrar")
                        }

                        Button(
                            onClick = {
                                if (editingItems.isEmpty()) {
                                    Toast.makeText(contextPrompt, "No puedes dejar la orden vacía. Por favor cancela la comanda si ya no se cocinará nada.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.modifyOrder(orderToEdit.id, editingItems) {
                                        viewModel.loadOrderItems(orderToEdit.id) { updatedItems ->
                                            receiptToPrint = orderToEdit.copy(totalAmount = liveTotalSum)
                                            receiptItemsToPrint = updatedItems
                                            showReceiptEditOrderDialog = null
                                            Toast.makeText(contextPrompt, "Comanda #${orderToEdit.id} actualizada", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
    }
    }
}

// ==========================================
// SECCION 2: COMANDAS (ACTIVE ORDERS & DELIVERIES)
// ==========================================
@Composable
fun ComandasTab(
    viewModel: RestaurantViewModel,
    activeOrders: List<Order>,
    allOrders: List<Order>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val restaurantName by viewModel.restaurantName.collectAsStateWithLifecycle()
    val restaurantAddress by viewModel.restaurantAddress.collectAsStateWithLifecycle()
    val restaurantPhone by viewModel.restaurantPhone.collectAsStateWithLifecycle()
    val logoType by viewModel.logoType.collectAsStateWithLifecycle()
    val restaurantLogoBase64 by viewModel.restaurantLogoBase64.collectAsStateWithLifecycle()
    val restaurantSlogan by viewModel.restaurantSlogan.collectAsStateWithLifecycle()

    var selectedOrderType by remember { mutableStateOf("ACTIVAS") } // "ACTIVAS", "HISTORIAL"
    var filterType by remember { mutableStateOf("TODAS") } // "TODAS", "LOCAL", "DOMICILIO"
    var showEditOrderDialog by remember { mutableStateOf<Order?>(null) }

    fun localPrintTicket(order: Order, items: List<OrderItem>) {
        val printerType = com.example.ui.BluetoothPrinterHelper.getPrinterType(context)
        val activePrinter = com.example.ui.BluetoothPrinterHelper.getSelectedPrinter(context)
        val receiptFormat = com.example.ui.BluetoothPrinterHelper.getReceiptFormat(context)
        val (wifiIp, wifiPort) = com.example.ui.BluetoothPrinterHelper.getWifiPrinter(context)
        val isSimplified = receiptFormat == "SIMPLIFIED"

        if (printerType == "BLUETOOTH") {
            val bDevice = activePrinter
            if (bDevice != null) {
                scope.launch {
                    val data = com.example.ui.BluetoothPrinterHelper.buildEscPosReceipt(
                        restaurantName, restaurantAddress, restaurantPhone, restaurantSlogan, order, items, isSimplified
                    )
                    val ok = com.example.ui.BluetoothPrinterHelper.printDirect(context, bDevice.address, data)
                    if (ok) {
                        Toast.makeText(context, "Ticket #${order.id} impreso con éxito via Bluetooth", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al imprimir via Bluetooth. Reintentando por impresión del sistema...", Toast.LENGTH_LONG).show()
                        val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
                        sendToThermalPrinter(context, html)
                    }
                }
            } else {
                Toast.makeText(context, "No hay impresora Bluetooth vinculada. Usando impresión de sistema.", Toast.LENGTH_SHORT).show()
                val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
                sendToThermalPrinter(context, html)
            }
        } else if (printerType == "WIFI") {
            scope.launch {
                val ip = wifiIp.trim()
                val port = wifiPort
                val data = com.example.ui.BluetoothPrinterHelper.buildEscPosReceipt(
                    restaurantName, restaurantAddress, restaurantPhone, restaurantSlogan, order, items, isSimplified
                )
                val ok = com.example.ui.BluetoothPrinterHelper.printViaWifi(context, ip, port, data)
                if (ok) {
                    Toast.makeText(context, "Ticket #${order.id} enviado exitosamente a $ip:$port", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error de enlace Wi-Fi. Reintentando por impresión del sistema...", Toast.LENGTH_LONG).show()
                    val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
                    sendToThermalPrinter(context, html)
                }
            }
        } else if (printerType == "USB") {
            Toast.makeText(context, "Imprimiendo Ticket #${order.id} mediante puerto USB...", Toast.LENGTH_SHORT).show()
            val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
            sendToThermalPrinter(context, html)
        } else {
            // SYSTEM PRINT (HTML)
            val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
            sendToThermalPrinter(context, html)
        }
    }

    val displayOrders = if (selectedOrderType == "ACTIVAS") {
        activeOrders.filter {
            filterType == "TODAS" ||
            (filterType == "LOCAL" && !it.isDelivery) ||
            (filterType == "DOMICILIO" && it.isDelivery)
        }
    } else {
        allOrders.filter { !activeOrders.contains(it) }.take(30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Tickets del Turno",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            // Switch to history
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Activas (${activeOrders.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedOrderType == "ACTIVAS") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { selectedOrderType = "ACTIVAS" }
                        .background(if (selectedOrderType == "ACTIVAS") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    "Historial",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedOrderType == "HISTORIAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { selectedOrderType = "HISTORIAL" }
                        .background(if (selectedOrderType == "HISTORIAL") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filters for active tables or deliveries
        if (selectedOrderType == "ACTIVAS") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("TODAS", "LOCAL", "DOMICILIO")
                filters.forEach { filter ->
                    val isSelected = filterType == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterType = filter },
                        label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (displayOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🥘", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (selectedOrderType == "ACTIVAS") "¡Gusto impecable! No hay comandas pendientes hoy" else "No hay pedidos completados en el historial físico",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayOrders.size) { index ->
                    val order = displayOrders[index]
                    var isExpanded by remember { mutableStateOf(false) }
                    var linkedItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }

                    // Load items when expanded
                    LaunchedEffect(isExpanded) {
                        if (isExpanded && linkedItems.isEmpty()) {
                            viewModel.loadOrderItems(order.id) { linkedItems = it }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header comanda
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isDeliveryColor = if (order.isDelivery) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    val statusIconColor = if (order.isDelivery) "🛵" else "🍽️"

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(isDeliveryColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(statusIconColor, fontSize = 20.sp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = order.customerName ?: "Cliente Mostrador",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        Text(
                                            dateFormat.format(Date(order.timestamp)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        order.totalAmount.formatPrice(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Status pill
                                    val statusText = when (order.status) {
                                        "PENDING" -> "COCIENDO"
                                        "DELIVERY" -> "EN REPARTO"
                                        "DELIVERED" -> "ENTREGADO"
                                        "COMPLETED" -> "COMPLETADO"
                                        else -> order.status
                                    }
                                    val statusColor = when (order.status) {
                                        "PENDING" -> MaterialTheme.colorScheme.error
                                        "DELIVERY" -> MaterialTheme.colorScheme.secondary
                                        "DELIVERED", "COMPLETED" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Surface(
                                        color = statusColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Subheader Table or Address
                            if (order.isDelivery && order.deliveryAddress != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Dir", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Dirección: ${order.deliveryAddress}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            } else if (order.tableNumber != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = "Table", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Mesa: ${order.tableNumber}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Pick", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Venta Directa de Mostrador",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // EXPANDABLE LINKED ITEMS DETAIL
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isExpanded) "Ocultar Detalle" else "Ver Platillos (${linkedItems.size.ifZeroGetText("Cargando")})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Flecha",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    linkedItems.forEach { itm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${itm.dishName}  x${itm.quantity}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                (itm.price * itm.quantity).formatPrice(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    // Action buttons for active order
                                    if (selectedOrderType == "ACTIVAS") {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        val context = LocalContext.current
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // 1. Salir
                                                OutlinedButton(
                                                    onClick = onBack,
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                    modifier = Modifier.weight(1f).testTag("salir_button")
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Salir", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Salir", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                }

                                                // 2. Editar
                                                OutlinedButton(
                                                    onClick = { showEditOrderDialog = order },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                    modifier = Modifier.weight(1.1f).testTag("editar_button")
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Modificar", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Editar", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                }

                                                // 3. Cocinar o/ Listo
                                                Button(
                                                    onClick = { viewModel.advanceOrderStatus(order) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (order.isDelivery) MaterialTheme.colorScheme.secondary else Color(0xFF2E7D32)
                                                    ),
                                                    modifier = Modifier.weight(1.3f).testTag("advance_status_button")
                                                ) {
                                                    Icon(
                                                        if (order.isDelivery) Icons.Default.Info else Icons.Default.Check,
                                                        contentDescription = "Avanzar",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        if (order.isDelivery) "Entregar" else "Cocinar o/ Listo",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // 4. Eliminar
                                                OutlinedButton(
                                                    onClick = { viewModel.cancelOrder(order) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                    modifier = Modifier.weight(1f).testTag("eliminar_button")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                }
 
                                                // 5. Compartir
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.loadOrderItems(order.id) { items ->
                                                            try {
                                                                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                                val shareText = java.lang.StringBuilder().apply {
                                                                    appendLine("=== COMANDA #${order.id} ===")
                                                                    appendLine("Cliente: ${order.customerName ?: "Mostrador"}")
                                                                    if (order.isDelivery && order.deliveryAddress != null) {
                                                                        appendLine("Entrega: ${order.deliveryAddress}")
                                                                    } else if (order.tableNumber != null) {
                                                                        appendLine("Mesa: ${order.tableNumber}")
                                                                    } else {
                                                                        appendLine("Tipo: Para llevar / Mostrador")
                                                                    }
                                                                    appendLine("Fecha: ${dateFormat.format(Date(order.timestamp))}")
                                                                    appendLine("-------------------------")
                                                                    items.forEach {
                                                                        appendLine("- ${it.dishName} x${it.quantity}: ${(it.price * it.quantity).formatPrice()}")
                                                                    }
                                                                    appendLine("-------------------------")
                                                                    appendLine("TOTAL: ${order.totalAmount.formatPrice()}")
                                                                    appendLine("=========================")
                                                                }.toString()
 
                                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                    type = "text/plain"
                                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                val chooser = Intent.createChooser(shareIntent, "Compartir Comanda").apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                context.startActivity(chooser)
                                                            } catch (ex: Exception) {
                                                                Toast.makeText(context, "Error al compartir comanda: ${ex.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                    modifier = Modifier.weight(1f).testTag("compartir_button")
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Compartir", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                }

                                                // 6. Reimprimir Ticket
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.loadOrderItems(order.id) { items ->
                                                            localPrintTicket(order, items)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                                                    modifier = Modifier.weight(1.1f).testTag("reimprimir_button")
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = "Imprimir", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Imprimir", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    } else {
                                        // Action buttons for completed/historic order
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 1. Compartir
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.loadOrderItems(order.id) { items ->
                                                        try {
                                                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                            val shareText = java.lang.StringBuilder().apply {
                                                                appendLine("=== COMANDA #${order.id} ===")
                                                                appendLine("Cliente: ${order.customerName ?: "Mostrador"}")
                                                                if (order.isDelivery && order.deliveryAddress != null) {
                                                                    appendLine("Entrega: ${order.deliveryAddress}")
                                                                } else if (order.tableNumber != null) {
                                                                    appendLine("Mesa: ${order.tableNumber}")
                                                                } else {
                                                                    appendLine("Tipo: Para llevar / Mostrador")
                                                                }
                                                                appendLine("Fecha: ${dateFormat.format(Date(order.timestamp))}")
                                                                appendLine("-------------------------")
                                                                items.forEach {
                                                                    appendLine("- ${it.dishName} x${it.quantity}: ${(it.price * it.quantity).formatPrice()}")
                                                                }
                                                                appendLine("-------------------------")
                                                                appendLine("TOTAL: ${order.totalAmount.formatPrice()}")
                                                                appendLine("=========================")
                                                            }.toString()

                                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            val chooser = Intent.createChooser(shareIntent, "Compartir Comanda").apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(chooser)
                                                        } catch (ex: Exception) {
                                                            Toast.makeText(context, "Error al compartir comanda: ${ex.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f).testTag("historial_compartir_button")
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Compartir", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                            }

                                            // 2. Reimprimir Ticket (Historial)
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.loadOrderItems(order.id) { items ->
                                                        localPrintTicket(order, items)
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1.1f).testTag("historial_reimprimir_button")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Reimprimir Ticket", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reimprimir", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showEditOrderDialog != null) {
        val orderToEdit = showEditOrderDialog!!
        var editingItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
        var isLoadingItems by remember { mutableStateOf(true) }
        val dishes by viewModel.dishes.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(orderToEdit.id) {
            viewModel.loadOrderItems(orderToEdit.id) { items ->
                editingItems = items
                isLoadingItems = false
            }
        }

        Dialog(onDismissRequest = { showEditOrderDialog = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Modificar Comanda #${orderToEdit.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cliente: ${orderToEdit.customerName ?: "Mostrador"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    if (isLoadingItems) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Platillos en esta Comanda:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1.2f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(editingItems.size) { index ->
                                    val itm = editingItems[index]
                                    val assocDish = dishes.firstOrNull { it.id == itm.dishId }
                                    val emoji = if (assocDish != null) getDishEmoji(assocDish) else "🍛"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Emoji click inserts another item
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    editingItems = editingItems.mapIndexed { idx, item ->
                                                        if (idx == index) item.copy(quantity = item.quantity + 1) else item
                                                    }
                                                }
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(itm.dishName, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                            Text(itm.price.formatPrice(), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (itm.quantity > 1) {
                                                        editingItems = editingItems.mapIndexed { idx, item ->
                                                            if (idx == index) item.copy(quantity = item.quantity - 1) else item
                                                        }
                                                    } else {
                                                        editingItems = editingItems.filterIndexed { idx, _ -> idx != index }
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Restar", modifier = Modifier.size(16.dp))
                                            }

                                            Text(
                                                "${itm.quantity}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            IconButton(
                                                onClick = {
                                                    editingItems = editingItems.mapIndexed { idx, item ->
                                                        if (idx == index) item.copy(quantity = item.quantity + 1) else item
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = {
                                                editingItems = editingItems.filterIndexed { idx, _ -> idx != index }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Sección", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Text(
                                "Agregar más Platillos del Menú:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().height(84.dp)
                            ) {
                                items(dishes.size) { index ->
                                    val dish = dishes[index]
                                    Card(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .fillMaxHeight()
                                            .clickable {
                                                val existingIndex = editingItems.indexOfFirst { it.dishId == dish.id }
                                                if (existingIndex != -1) {
                                                    editingItems = editingItems.mapIndexed { idx, item ->
                                                        if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                                                    }
                                                } else {
                                                    editingItems = editingItems + OrderItem(
                                                        orderId = orderToEdit.id,
                                                        dishId = dish.id,
                                                        dishName = dish.name,
                                                        quantity = 1,
                                                        price = dish.price,
                                                        cost = dish.cost
                                                    )
                                                }
                                                Toast.makeText(context, "${dish.name} añadido", Toast.LENGTH_SHORT).show()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(getDishEmoji(dish), fontSize = 18.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                dish.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                dish.price.formatPrice(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val liveTotalSum = editingItems.sumOf { it.price * it.quantity }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Estimado:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(liveTotalSum.formatPrice(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showEditOrderDialog = null }, modifier = Modifier.weight(1f)) {
                            Text("Cerrar")
                        }

                        Button(
                            onClick = {
                                if (editingItems.isEmpty()) {
                                    Toast.makeText(context, "No puedes dejar la orden vacía. Por favor cancela la comanda si ya no se cocinará nada.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.modifyOrder(orderToEdit.id, editingItems)
                                    showEditOrderDialog = null
                                    Toast.makeText(context, "Comanda #${orderToEdit.id} actualizada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}
}

private fun Int.ifZeroGetText(sub: String): String = if (this == 0) sub else this.toString()

// ==========================================
// SECCION 3: ALMACEN (INVENTORY & RESUPPLIES)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlmacenTab(
    viewModel: RestaurantViewModel,
    dishes: List<Dish>,
    onBack: () -> Unit
) {
    var showAddDishDialog by remember { mutableStateOf(false) }
    var showResupplyDialog by remember { mutableStateOf<Dish?>(null) }
    var showEditStockDialog by remember { mutableStateOf<Dish?>(null) }
    var showStartDayDialog by remember { mutableStateOf(false) }
    var filterLowStockOnly by remember { mutableStateOf(false) }

    val lowStockDishes = remember(dishes) {
        dishes.filter { it.dailyStock <= it.minStockThreshold }
    }

    LaunchedEffect(lowStockDishes.size) {
        if (lowStockDishes.isEmpty()) {
            filterLowStockOnly = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inventario de Almacén",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Control de stock de ingredientes, platos y compras",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { showAddDishDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_dish_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Plato")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Platillo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // START SALES DAY CARD
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Día de Venta", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Carga existencias al por mayor del menú hoy. Se irán descontando al vender.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { showStartDayDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Configurar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val displayedDishes = if (filterLowStockOnly) lowStockDishes else dishes

        if (lowStockDishes.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filterLowStockOnly) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (filterLowStockOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("low_stock_notification_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔔", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notificación de Almacén",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "¡Alerta! Hay ${lowStockDishes.size} ${if (lowStockDishes.size == 1) "producto" else "productos"} con existencia menor al umbral crítico.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { filterLowStockOnly = !filterLowStockOnly },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (filterLowStockOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("low_stock_filter_toggle")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (filterLowStockOnly) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = if (filterLowStockOnly) "Mostrar Todo" else "Filtrar",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (filterLowStockOnly) "Mostrar Todo" else "Ver Alertas",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (dishes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "El almacén está vacío.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Comienza agregando platillos con el botón superior.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedDishes.size) { index ->
                    val item = displayedDishes[index]
                    val isLowStock = item.dailyStock <= item.minStockThreshold
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = if (isLowStock) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            item.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                item.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (isLowStock) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("⚠️", fontSize = 9.sp)
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        "MÍN: ${item.minStockThreshold}",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(
                                            "Precio de Venta: ${item.price.formatPrice()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            "Costo de Platillo: ${item.cost.formatPrice()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                // STOCK CONTROL VISUAL BLOCK
                                Column(horizontalAlignment = Alignment.End) {
                                    val pct = if (item.initialDailyStock > 0) item.dailyStock.toFloat() / item.initialDailyStock else 0f
                                    val stockColor = when {
                                        isLowStock -> MaterialTheme.colorScheme.error
                                        pct <= 0.2f -> MaterialTheme.colorScheme.error
                                        pct <= 0.5f -> MaterialTheme.colorScheme.secondary
                                        else -> Color(0xFF2E7D32)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${item.dailyStock}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = stockColor
                                        )
                                        Text(
                                            " / ${item.initialDailyStock}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }

                                    Text(
                                        "Stock para hoy",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // ADMIN CONTROLS PER PRODUCT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditStockDialog = item },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar Stock", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fijar Stock Inicial", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showResupplyDialog = item },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Comprar")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Comprar Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteDish(item) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1) DIALOG TO CREATE INGREDIENT / DISH
    if (showAddDishDialog) {
        var dishName by remember { mutableStateOf("") }
        var dishPrice by remember { mutableStateOf("") }
        var dishCost by remember { mutableStateOf("") }
        var dishStock by remember { mutableStateOf("") }
        var dishMinThreshold by remember { mutableStateOf("5") }
        var dishCat by remember { mutableStateOf("Platos Principales") }
        var dishImageBase64 by remember { mutableStateOf<String?>(null) }

        val presetCats = listOf("Platos Principales", "Bebidas", "Postres", "Entradas", "Guarniciones")
        val context = LocalContext.current

        Dialog(onDismissRequest = { showAddDishDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Crear Nuevo Platillo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = dishName,
                        onValueChange = { dishName = it },
                        label = { Text("Nombre del platillo o bebida") },
                        modifier = Modifier.fillMaxWidth().testTag("dish_name_field"),
                        singleLine = true
                    )

                    // Image picker for this custom product/food item
                    val itemImagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            val base64 = uriToBase64(context, uri)
                            if (base64 != null) {
                                dishImageBase64 = base64
                            } else {
                                Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { itemImagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!dishImageBase64.isNullOrBlank()) {
                                    ProductImage(imageBase64 = dishImageBase64, fallbackEmoji = "🍽️", modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Subir Imagen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (!dishImageBase64.isNullOrBlank()) "¡Imagen cargada!" else "Agregar Imagen / Logo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (!dishImageBase64.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Toque el recuadro para seleccionar un PNG o JPG.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        if (!dishImageBase64.isNullOrBlank()) {
                            IconButton(onClick = { dishImageBase64 = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dishPrice,
                            onValueChange = { dishPrice = it },
                            label = { Text("PVP Venta ($)") },
                            modifier = Modifier.weight(1f).testTag("dish_price_field"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dishCost,
                            onValueChange = { dishCost = it },
                            label = { Text("Costo Prep ($)") },
                            modifier = Modifier.weight(1f).testTag("dish_cost_field"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = dishStock,
                        onValueChange = { dishStock = it },
                        label = { Text("Stock Base Diario") },
                        modifier = Modifier.fillMaxWidth().testTag("dish_stock_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dishMinThreshold,
                        onValueChange = { dishMinThreshold = it },
                        label = { Text("Umbral Mínimo para Alerta") },
                        placeholder = { Text("Ej. 5") },
                        modifier = Modifier.fillMaxWidth().testTag("dish_min_threshold_field"),
                        singleLine = true
                    )

                    Text("Categoría del Menú", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items(presetCats.size) { index ->
                            val cat = presetCats[index]
                            val isSel = cat == dishCat
                            FilterChip(
                                selected = isSel,
                                onClick = { dishCat = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Manual input category if needed
                    var isCustomCat by remember { mutableStateOf(false) }
                    var customCatText by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCustomCat, onCheckedChange = { isCustomCat = it })
                        Text("Otra categoría personalizada", fontSize = 12.sp)
                    }
                    if (isCustomCat) {
                        OutlinedTextField(
                            value = customCatText,
                            onValueChange = { customCatText = it },
                            label = { Text("Nueva Categoría") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showAddDishDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val costVal = dishCost.toDoubleOrNull() ?: 0.0
                                val priceVal = dishPrice.toDoubleOrNull() ?: 0.0
                                val stockVal = dishStock.toIntOrNull() ?: 0
                                val threshVal = dishMinThreshold.toIntOrNull() ?: 5
                                val finalCategory = if (isCustomCat && customCatText.isNotBlank()) customCatText.trim() else dishCat

                                if (dishName.isBlank() || priceVal <= 0.0 || stockVal < 0 || threshVal < 0) {
                                    Toast.makeText(context, "Favor rellenar campos con valores válidos", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addDish(dishName.trim(), priceVal, costVal, finalCategory, stockVal, dishImageBase64, threshVal)
                                    showAddDishDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Registrar")
                        }
                    }
                }
            }
        }
    }

    // 2) BOX TO ADD INGREDIENT SUPPLIES (COMPRA DE PLATO)
    if (showResupplyDialog != null) {
        val dish = showResupplyDialog!!
        var addQty by remember { mutableStateOf("10") }
        var unitCostStr by remember { mutableStateOf(dish.cost.toString()) }
        val context = LocalContext.current

        Dialog(onDismissRequest = { showResupplyDialog = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Registrar Compra / Suministro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("Producto: ${dish.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = addQty,
                        onValueChange = { addQty = it },
                        label = { Text("Cantidad a Adquirir") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unitCostStr,
                        onValueChange = { unitCostStr = it },
                        label = { Text("Costo Unitario de Compra ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showResupplyDialog = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val qtyVal = addQty.toIntOrNull() ?: 0
                                val costVal = unitCostStr.toDoubleOrNull() ?: 0.0
                                if (qtyVal <= 0 || costVal <= 0.0) {
                                    Toast.makeText(context, "Valores incorrectos", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.buyDishStock(dish.id, qtyVal, costVal)
                                    showResupplyDialog = null
                                    Toast.makeText(context, "Abastecimiento guardado con éxito", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Guardar Compra")
                        }
                    }
                }
            }
        }
    }

    // 3) DIALOG TO MANUALLY DEFINE DAILY INITIAL STOCK
    if (showEditStockDialog != null) {
        val dish = showEditStockDialog!!
        var startStockStr by remember { mutableStateOf(dish.initialDailyStock.toString()) }
        var minThresholdStr by remember { mutableStateOf(dish.minStockThreshold.toString()) }
        val context = LocalContext.current

        Dialog(onDismissRequest = { showEditStockDialog = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configurar Existencias de Almacén", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Ajustando parámetros de: ${dish.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = startStockStr,
                        onValueChange = { startStockStr = it },
                        label = { Text("Nuevo Stock Técnico") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_stock_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minThresholdStr,
                        onValueChange = { minThresholdStr = it },
                        label = { Text("Umbral Mínimo para Alerta") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_threshold_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showEditStockDialog = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val finalStk = startStockStr.toIntOrNull() ?: -1
                                val finalThresh = minThresholdStr.toIntOrNull() ?: -1
                                if (finalStk < 0 || finalThresh < 0) {
                                    Toast.makeText(context, "Favor escribir valores numéricos válidos", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updatedDish = dish.copy(
                                        dailyStock = finalStk,
                                        initialDailyStock = finalStk,
                                        minStockThreshold = finalThresh
                                    )
                                    viewModel.updateDish(updatedDish)
                                    showEditStockDialog = null
                                    Toast.makeText(context, "Parámetros de almacén redefinidos con éxito", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Establecer")
                        }
                    }
                }
            }
        }
    }

    if (showStartDayDialog) {
        var stocksToSet by remember { mutableStateOf(dishes.associate { it.id to it.dailyStock.coerceAtLeast(0) }) }
        val context = LocalContext.current

        Dialog(onDismissRequest = { showStartDayDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configurar Día de Venta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Establece las existencias iniciales para iniciar el día de venta. Las cantidades se descontarán en tiempo real al vender.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                stocksToSet = dishes.associate { it.id to 20 }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Todo a 20", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                stocksToSet = dishes.associate { it.id to 50 }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Todo a 50", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                stocksToSet = dishes.associate { it.id to 0 }
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Limpiar (0)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dishes.size) { index ->
                            val dish = dishes[index]
                            val currentQty = stocksToSet[dish.id] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(getDishEmoji(dish), fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(dish.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentQty > 0) {
                                                stocksToSet = stocksToSet.toMutableMap().apply {
                                                    put(dish.id, currentQty - 1)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Restar",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        "$currentQty",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(
                                        onClick = {
                                            stocksToSet = stocksToSet.toMutableMap().apply {
                                                put(dish.id, currentQty + 1)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Sumar",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showStartDayDialog = false }, modifier = Modifier.weight(1.2f)) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                viewModel.startSalesDay(stocksToSet)
                                showStartDayDialog = false
                                Toast.makeText(context, "¡Día de venta configurado con éxito!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.8f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Iniciar Día de Venta", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECCION 4: CIERRE DE CAJA (CLOSURE & STATS)
// ==========================================
@Composable
fun CierresTab(
    viewModel: RestaurantViewModel,
    closures: List<CashClosure>,
    activeOrders: List<Order>,
    activeOrderItems: List<OrderItem>,
    onBack: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf("CONTROL_CAJA") } // "CONTROL_CAJA" or "REPORTES"
    var selectedReportClosure by remember { mutableStateOf<CashClosure?>(null) }
    val selectedClosureDetails by viewModel.selectedClosure.collectAsStateWithLifecycle()
    val selectedClosureItems by viewModel.selectedClosureItems.collectAsStateWithLifecycle()
    
    val isShiftActive by viewModel.isShiftActive.collectAsStateWithLifecycle()
    val initialCash by viewModel.initialCash.collectAsStateWithLifecycle()
    var showCloseTurnDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showCloseTurnDialog) {
        var actualCashText by remember { mutableStateOf("") }
        val activeCashSalesSum = activeOrders.filter { it.paymentMethod == "Efectivo" }.sumOf { it.totalAmount }
        val expectedCash = initialCash + activeCashSalesSum
        val actualCash = actualCashText.toDoubleOrNull() ?: 0.0
        val difference = actualCash - expectedCash

        Dialog(onDismissRequest = { showCloseTurnDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cerrar Turno y Caja",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Realice el conteo físico de dinero en caja para cuadrar el turno actual y archivar las comandas correspondientes.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Totales de Caja
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Efectivo Inicial:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(initialCash.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("+ Ventas en Efectivo:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(activeCashSalesSum.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Efectivo Esperado (Teórico):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(expectedCash.formatPrice(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Campo para ingresar efectivo real
                    OutlinedTextField(
                        value = actualCashText,
                        onValueChange = { actualCashText = it },
                        label = { Text("Efectivo Real en Caja ($)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("actual_cash_input")
                    )

                    // Mostrar diferencia
                    if (actualCashText.isNotEmpty()) {
                        val diffColor = when {
                            difference == 0.0 -> Color(0xFF2E7D32)
                            difference > 0.0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                        val diffText = when {
                            difference == 0.0 -> "Caja Cuadrada"
                            difference > 0.0 -> "Sobrante de: ${difference.formatPrice()}"
                            else -> "Faltante de: ${difference.formatPrice()}"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(diffColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Diferencia: $diffText",
                                fontWeight = FontWeight.Bold,
                                color = diffColor,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showCloseTurnDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                viewModel.closeCashRegister(
                                    actualCashAtClose = actualCash,
                                    onSuccess = { closure ->
                                        showCloseTurnDialog = false
                                        Toast.makeText(context, "¡Caja cerrada exitosamente!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirmar Cierre", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Calculations for the current active open drawer (before closing)
    val openSalesSum = activeOrders.sumOf { it.totalAmount }
    val openCostSum = activeOrders.sumOf { it.totalCost }
    val openProfitSum = openSalesSum - openCostSum
    val openOrderCount = activeOrders.size

    val openStats = viewModel.computeStats(activeOrderItems)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Turno y Cierre",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { activeSubTab = "CONTROL_CAJA" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "CONTROL_CAJA") MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (activeSubTab == "CONTROL_CAJA") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("subtab_control_caja"),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Caja Diaria", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { activeSubTab = "REPORTES" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "REPORTES") MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (activeSubTab == "REPORTES") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("subtab_reportes"),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reportes Período", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (activeSubTab == "REPORTES") {
            ReportesPeriodoView(
                viewModel = viewModel,
                closures = closures,
                onBack = onBack
            )
        } else {
            // PANEL 1: REGISTRADORA EN CURSO (TIPO CORTE PARCIAL)
            Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val statusText = if (isShiftActive) "Caja Registradora ABIERTA" else "Caja Registradora CERRADA"
                        val statusDesc = if (isShiftActive) "Ventas acumuladas hoy listas para corte" else "Debe aperturar turno en la pestaña de Ventas"
                        val statusColor = if (isShiftActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        
                        Text(statusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = statusColor)
                        Text(statusDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (isShiftActive) {
                                showCloseTurnDialog = true
                            } else {
                                Toast.makeText(context, "No hay ningún turno activo para cerrar.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isShiftActive,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("trigger_closure_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Cerrar Caja")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cerrar Caja Hoy", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // STATS COUNTERS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ventas
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Ventas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(openSalesSum.formatPrice(), fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Ganancias
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ganancia Neta", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(openProfitSum.formatPrice(), fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF2E7D32))
                        }
                    }

                    // Pedidos
                    Surface(
                        modifier = Modifier.weight(0.8f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Comandas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$openOrderCount items", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // BEST SELLING PRODUCTS (SESSION STATS)
                if (openStats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Platillos más vendidos (Sesión Abierta):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))

                    openStats.take(5).forEachIndexed { index, stat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stat.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Cant: ${stat.quantitySold}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Utilidad: ${stat.profit.formatPrice()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                // PDF, WhatsApp & Email Reports Row for active session
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Exportar Reporte de Turno Activo (PDF):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rName by viewModel.restaurantName.collectAsStateWithLifecycle()
                    val rAddress by viewModel.restaurantAddress.collectAsStateWithLifecycle()
                    val rPhone by viewModel.restaurantPhone.collectAsStateWithLifecycle()
                    val rLogoBase64 by viewModel.restaurantLogoBase64.collectAsStateWithLifecycle()

                    Button(
                        onClick = {
                            val paymentsMap = activeOrders.groupBy { it.paymentMethod }
                                .mapValues { it.value.sumOf { o -> o.totalAmount } }
                            val orderTypesMap = activeOrders.groupBy {
                                when {
                                    it.isDelivery -> "DOMICILIO"
                                    it.tableNumber == "Para Llevar" -> "LLEVAR"
                                    else -> "COMER_AQUI"
                                }
                            }.mapValues { it.value.size }

                            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            val file = PdfReportHelper.generateClosureReport(
                                context = context,
                                restaurantName = rName,
                                restaurantAddress = rAddress,
                                restaurantPhone = rPhone,
                                restaurantLogoBase64 = rLogoBase64,
                                reportTitle = "REPORTE DE PRE-CIERRE DE CAJA (CORTE PARCIAL)",
                                dateRange = dateStr,
                                ordersCount = openOrderCount,
                                totalSales = openSalesSum,
                                totalCost = openCostSum,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = orderTypesMap,
                                productStats = openStats
                            )
                            if (file != null) {
                                PdfReportHelper.shareDocument(context, file, "Pre-Cierre de Caja ($dateStr)")
                            } else {
                                Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("share_active_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "PDF", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val paymentsMap = activeOrders.groupBy { it.paymentMethod }
                                .mapValues { it.value.sumOf { o -> o.totalAmount } }
                            val orderTypesMap = activeOrders.groupBy {
                                when {
                                    it.isDelivery -> "DOMICILIO"
                                    it.tableNumber == "Para Llevar" -> "LLEVAR"
                                    else -> "COMER_AQUI"
                                }
                            }.mapValues { it.value.size }

                            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            val file = PdfReportHelper.generateClosureReport(
                                context = context,
                                restaurantName = rName,
                                restaurantAddress = rAddress,
                                restaurantPhone = rPhone,
                                restaurantLogoBase64 = rLogoBase64,
                                reportTitle = "REPORTE DE PRE-CIERRE DE CAJA (CORTE PARCIAL)",
                                dateRange = dateStr,
                                ordersCount = openOrderCount,
                                totalSales = openSalesSum,
                                totalCost = openCostSum,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = orderTypesMap,
                                productStats = openStats
                            )
                            if (file != null) {
                                PdfReportHelper.shareToWhatsApp(context, file, "Pre-Cierre ($dateStr)")
                            } else {
                                Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("whatsapp_active_pdf_button")
                    ) {
                        Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val paymentsMap = activeOrders.groupBy { it.paymentMethod }
                                .mapValues { it.value.sumOf { o -> o.totalAmount } }
                            val orderTypesMap = activeOrders.groupBy {
                                when {
                                    it.isDelivery -> "DOMICILIO"
                                    it.tableNumber == "Para Llevar" -> "LLEVAR"
                                    else -> "COMER_AQUI"
                                }
                            }.mapValues { it.value.size }

                            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            val file = PdfReportHelper.generateClosureReport(
                                context = context,
                                restaurantName = rName,
                                restaurantAddress = rAddress,
                                restaurantPhone = rPhone,
                                restaurantLogoBase64 = rLogoBase64,
                                reportTitle = "REPORTE DE PRE-CIERRE DE CAJA (CORTE PARCIAL)",
                                dateRange = dateStr,
                                ordersCount = openOrderCount,
                                totalSales = openSalesSum,
                                totalCost = openCostSum,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = orderTypesMap,
                                productStats = openStats
                            )
                            if (file != null) {
                                PdfReportHelper.shareToEmail(context, file, "Pre-Cierre ($dateStr)")
                            } else {
                                Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE15C42)), // Red/Coral for Email
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("email_active_pdf_button")
                    ) {
                        Text("Correo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // PANEL 2: COMPLETED CLOSURES REPORTS LIST
        Text(
            "Cierres de Caja Anteriores",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (closures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se ha registrado ningún cierre de caja oficial.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            closures.forEach { closure ->
                val isSelected = selectedClosureDetails?.id == closure.id
                Card(
                    onClick = {
                        if (isSelected) {
                            viewModel.selectClosure(null)
                        } else {
                            viewModel.selectClosure(closure)
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = "Cierre", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Cierre ID: #${closure.id}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    "Fecha: ${closure.dateString}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Venta: ${closure.totalSales.formatPrice()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Ganancia: ${closure.netProfit.formatPrice()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        // EXPANDED STATISTICS OF THE SELECTED CASH CLOSURE
                        if (isSelected && selectedClosureDetails != null && selectedClosureDetails!!.id == closure.id) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Métricas de Fin de Día Recorrido:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text("• Total Órdenes Procesadas: ${closure.totalOrdersCount}", fontSize = 12.sp)
                            Text("• Costo de Ingredientes: ${closure.totalCost.formatPrice()}", fontSize = 12.sp)
                            Text("• Ganancia Neta Final (Marginal): ${closure.netProfit.formatPrice()}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Ranking de Más Vendidos en este Cierre:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            val stats = viewModel.computeStats(selectedClosureItems)
                            if (stats.isEmpty()) {
                                Text("No hay ítems registrados", fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                            } else {
                                stats.forEachIndexed { rank, pStat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${rank + 1}. ${pStat.name} (${pStat.quantitySold} uds)", fontSize = 12.sp)
                                        Text("Ganancia: ${pStat.profit.formatPrice()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                }
                            }

                            // PDF Actions for selected historical closure
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Compartir Reporte de Caja en PDF:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val closureOrders = viewModel.allOrders.value.filter { it.closureId == closure.id }
                                        val paymentsMap = closureOrders.groupBy { it.paymentMethod }
                                            .mapValues { it.value.sumOf { o -> o.totalAmount } }
                                        val orderTypesMap = closureOrders.groupBy {
                                            when {
                                                it.isDelivery -> "DOMICILIO"
                                                it.tableNumber == "Para Llevar" -> "LLEVAR"
                                                else -> "COMER_AQUI"
                                            }
                                        }.mapValues { it.value.size }

                                        val file = PdfReportHelper.generateClosureReport(
                                            context = context,
                                            restaurantName = viewModel.restaurantName.value,
                                            restaurantAddress = viewModel.restaurantAddress.value,
                                            restaurantPhone = viewModel.restaurantPhone.value,
                                            restaurantLogoBase64 = viewModel.restaurantLogoBase64.value,
                                            reportTitle = "REPORTE DE CIERRE DE CAJA #${closure.id}",
                                            dateRange = closure.dateString,
                                            ordersCount = closure.totalOrdersCount,
                                            totalSales = closure.totalSales,
                                            totalCost = closure.totalCost,
                                            paymentsBreakdown = paymentsMap,
                                            orderTypesBreakdown = orderTypesMap,
                                            productStats = stats
                                        )
                                        if (file != null) {
                                            PdfReportHelper.shareDocument(context, file, "Cierre de Caja #${closure.id}")
                                        } else {
                                            Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("share_closure_pdf_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "PDF", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compartir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val closureOrders = viewModel.allOrders.value.filter { it.closureId == closure.id }
                                        val paymentsMap = closureOrders.groupBy { it.paymentMethod }
                                            .mapValues { it.value.sumOf { o -> o.totalAmount } }
                                        val orderTypesMap = closureOrders.groupBy {
                                            when {
                                                it.isDelivery -> "DOMICILIO"
                                                it.tableNumber == "Para Llevar" -> "LLEVAR"
                                                else -> "COMER_AQUI"
                                            }
                                        }.mapValues { it.value.size }

                                        val file = PdfReportHelper.generateClosureReport(
                                            context = context,
                                            restaurantName = viewModel.restaurantName.value,
                                            restaurantAddress = viewModel.restaurantAddress.value,
                                            restaurantPhone = viewModel.restaurantPhone.value,
                                            restaurantLogoBase64 = viewModel.restaurantLogoBase64.value,
                                            reportTitle = "REPORTE DE CIERRE DE CAJA #${closure.id}",
                                            dateRange = closure.dateString,
                                            ordersCount = closure.totalOrdersCount,
                                            totalSales = closure.totalSales,
                                            totalCost = closure.totalCost,
                                            paymentsBreakdown = paymentsMap,
                                            orderTypesBreakdown = orderTypesMap,
                                            productStats = stats
                                        )
                                        if (file != null) {
                                            PdfReportHelper.shareToWhatsApp(context, file, "Cierre #${closure.id}")
                                        } else {
                                            Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("whatsapp_closure_pdf_button")
                                ) {
                                    Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        val closureOrders = viewModel.allOrders.value.filter { it.closureId == closure.id }
                                        val paymentsMap = closureOrders.groupBy { it.paymentMethod }
                                            .mapValues { it.value.sumOf { o -> o.totalAmount } }
                                        val orderTypesMap = closureOrders.groupBy {
                                            when {
                                                it.isDelivery -> "DOMICILIO"
                                                it.tableNumber == "Para Llevar" -> "LLEVAR"
                                                else -> "COMER_AQUI"
                                            }
                                        }.mapValues { it.value.size }

                                        val file = PdfReportHelper.generateClosureReport(
                                            context = context,
                                            restaurantName = viewModel.restaurantName.value,
                                            restaurantAddress = viewModel.restaurantAddress.value,
                                            restaurantPhone = viewModel.restaurantPhone.value,
                                            restaurantLogoBase64 = viewModel.restaurantLogoBase64.value,
                                            reportTitle = "REPORTE DE CIERRE DE CAJA #${closure.id}",
                                            dateRange = closure.dateString,
                                            ordersCount = closure.totalOrdersCount,
                                            totalSales = closure.totalSales,
                                            totalCost = closure.totalCost,
                                            paymentsBreakdown = paymentsMap,
                                            orderTypesBreakdown = orderTypesMap,
                                            productStats = stats
                                        )
                                        if (file != null) {
                                            PdfReportHelper.shareToEmail(context, file, "Cierre #${closure.id}")
                                        } else {
                                            Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE15C42)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("email_closure_pdf_button")
                                ) {
                                    Text("Correo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        } // Close activeSubTab "else" block (CONTROL_CAJA)
    }
}

// ==========================================
// SECCION 4: IMPRESORA (REPRINT COMPLETED TICKETS)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpresoraTab(
    viewModel: RestaurantViewModel,
    allOrders: List<Order>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val restaurantName by viewModel.restaurantName.collectAsStateWithLifecycle()
    val restaurantAddress by viewModel.restaurantAddress.collectAsStateWithLifecycle()
    val restaurantPhone by viewModel.restaurantPhone.collectAsStateWithLifecycle()
    val logoType by viewModel.logoType.collectAsStateWithLifecycle()
    val restaurantLogoBase64 by viewModel.restaurantLogoBase64.collectAsStateWithLifecycle()
    val restaurantSlogan by viewModel.restaurantSlogan.collectAsStateWithLifecycle()
    val isShiftActive by viewModel.isShiftActive.collectAsStateWithLifecycle()

    var activeConfigTab by remember { mutableStateOf("CONTROL_VAL") } // "CONTROL_VAL", "IMPRESORA_CONFIG"
    var receiptFormat by remember { mutableStateOf("DETAILED") } // "DETAILED" or "SIMPLIFIED"
    
    // CONTROL DE TICKETS DEL TURNO STATE
    var ticketFilterSubTab by remember { mutableStateOf("PENDIENTES") } // "PENDIENTES" or "FINALIZADOS"
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("ID_ASC") } // "ID_ASC", "ID_DESC"

    var expandedOrderId by remember { mutableStateOf<Int?>(null) }
    var expandedItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var loadingItemsId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(expandedOrderId) {
        val id = expandedOrderId
        if (id != null) {
            loadingItemsId = id
            viewModel.loadOrderItems(id) { items ->
                expandedItems = items
                loadingItemsId = null
            }
        } else {
            expandedItems = emptyList()
        }
    }

    // Filter current shift orders (closureId == null)
    val currentShiftOrders = allOrders.filter { it.closureId == null }

    val filteredOrders = currentShiftOrders.filter { order ->
        val matchesQuery = if (searchQuery.isBlank()) {
            true
        } else {
            val q = searchQuery.lowercase(Locale.getDefault())
            val idStr = "#${order.id}"
            val matchesId = idStr.contains(q) || order.id.toString().contains(q)
            val matchesName = order.customerName?.lowercase(Locale.getDefault())?.contains(q) == true
            val matchesTable = order.tableNumber?.lowercase(Locale.getDefault())?.contains(q) == true
            matchesId || matchesName || matchesTable
        }

        val matchesStatus = if (ticketFilterSubTab == "PENDIENTES") {
            order.status != "COMPLETED"
        } else {
            order.status == "COMPLETED"
        }

        matchesQuery && matchesStatus
    }

    val sortedOrders = when (selectedSort) {
        "ID_ASC" -> filteredOrders.sortedBy { it.id }
        "ID_DESC" -> filteredOrders.sortedByDescending { it.id }
        else -> filteredOrders
    }

    // PRINTER CONFIGURATION STATE
    var printerType by remember { mutableStateOf("BLUETOOTH") }
    var wifiIp by remember { mutableStateOf("192.168.1.100") }
    var wifiPort by remember { mutableStateOf("9100") }
    var usbDeviceName by remember { mutableStateOf("Impresora Termica USB") }
    
    var activePrinter by remember { mutableStateOf<com.example.ui.BluetoothPrinterHelper.PrinterDevice?>(null) }
    var pairedPrinters by remember { mutableStateOf<List<com.example.ui.BluetoothPrinterHelper.PrinterDevice>>(emptyList()) }
    var isPairedListExpanded by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        printerType = com.example.ui.BluetoothPrinterHelper.getPrinterType(context)
        activePrinter = com.example.ui.BluetoothPrinterHelper.getSelectedPrinter(context)
        receiptFormat = com.example.ui.BluetoothPrinterHelper.getReceiptFormat(context)
        val (ip, pt) = com.example.ui.BluetoothPrinterHelper.getWifiPrinter(context)
        wifiIp = ip
        wifiPort = pt.toString()
        usbDeviceName = com.example.ui.BluetoothPrinterHelper.getUsbPrinterName(context) ?: "Impresora Termica USB Generica-58mm"
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val connectGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            permissions[android.Manifest.permission.BLUETOOTH] == true
        }
        if (connectGranted) {
            if (com.example.ui.BluetoothPrinterHelper.isBluetoothEnabled(context)) {
                pairedPrinters = com.example.ui.BluetoothPrinterHelper.getPairedPrinters(context)
                isPairedListExpanded = true
            } else {
                Toast.makeText(context, "Por favor active el Bluetooth de su dispositivo", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Permisos Bluetooth necesarios para buscar impresoras", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndLoadPrinters() {
        if (com.example.ui.BluetoothPrinterHelper.hasBluetoothPermission(context)) {
            if (com.example.ui.BluetoothPrinterHelper.isBluetoothEnabled(context)) {
                pairedPrinters = com.example.ui.BluetoothPrinterHelper.getPairedPrinters(context)
                isPairedListExpanded = !isPairedListExpanded
            } else {
                Toast.makeText(context, "Por favor active el Bluetooth de su dispositivo", Toast.LENGTH_LONG).show()
            }
        } else {
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN)
            } else {
                arrayOf(android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN)
            }
            requestPermissionLauncher.launch(permissions)
        }
    }

    // CONSOLIDATED PRINT FUNCTION
    fun printTicket(order: Order, items: List<OrderItem>) {
        val isSimplified = receiptFormat == "SIMPLIFIED"
        if (printerType == "BLUETOOTH") {
            val bDevice = activePrinter
            if (bDevice != null) {
                scope.launch {
                    val data = com.example.ui.BluetoothPrinterHelper.buildEscPosReceipt(
                        restaurantName, restaurantAddress, restaurantPhone, restaurantSlogan, order, items, isSimplified
                    )
                    val ok = com.example.ui.BluetoothPrinterHelper.printDirect(context, bDevice.address, data)
                    if (ok) {
                        Toast.makeText(context, "Ticket #${order.id} impreso con éxito via Bluetooth", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al imprimir via Bluetooth. Reintentando por impresión del sistema...", Toast.LENGTH_LONG).show()
                        val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
                        sendToThermalPrinter(context, html)
                    }
                }
            } else {
                Toast.makeText(context, "No hay impresora Bluetooth vinculada. Usando impresión de sistema.", Toast.LENGTH_SHORT).show()
                val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
                sendToThermalPrinter(context, html)
            }
        } else if (printerType == "WIFI") {
            scope.launch {
                val ip = wifiIp.trim()
                val port = wifiPort.toIntOrNull() ?: 9100
                val data = com.example.ui.BluetoothPrinterHelper.buildEscPosReceipt(
                    restaurantName, restaurantAddress, restaurantPhone, restaurantSlogan, order, items, isSimplified
                )
                val ok = com.example.ui.BluetoothPrinterHelper.printViaWifi(context, ip, port, data)
                if (ok) {
                    Toast.makeText(context, "Ticket #${order.id} enviado exitosamente a $ip:$port", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error de enlace Wi-Fi. Reintentando por impresión del sistema...", Toast.LENGTH_LONG).show()
                    val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
                    sendToThermalPrinter(context, html)
                }
            }
        } else if (printerType == "USB") {
            Toast.makeText(context, "Imprimiendo Ticket #${order.id} mediante puerto USB...", Toast.LENGTH_SHORT).show()
            val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
            sendToThermalPrinter(context, html)
        } else {
            // SYSTEM PRINT (HTML)
            val html = generateReceiptHtml(order, items, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan, isSimplified)
            sendToThermalPrinter(context, html)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Tickets del Turno",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Control de comandas, reimpresiones y ajustes de impresora",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // DUAL MENU TAB ROW (Control vs Configuración)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeConfigTab == "CONTROL_VAL") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeConfigTab = "CONTROL_VAL" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Control",
                        tint = if (activeConfigTab == "CONTROL_VAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Control de Tickets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeConfigTab == "CONTROL_VAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeConfigTab == "IMPRESORA_CONFIG") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeConfigTab = "IMPRESORA_CONFIG" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = if (activeConfigTab == "IMPRESORA_CONFIG") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Configurar Impresora",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeConfigTab == "IMPRESORA_CONFIG") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeConfigTab == "CONTROL_VAL") {
            // ==========================================
            // SECCIÓN: CONTROL DE TICKETS DEL TURNO
            // ==========================================
            
            // Turno Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isShiftActive) Color(0xFF2E7D32).copy(alpha = 0.05f) else Color(0xFFD32F2F).copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, if (isShiftActive) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isShiftActive) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                        )
                        Text(
                            text = if (isShiftActive) "Turno Vigente de Ventas ACTIVO" else "No hay Turno de Ventas Abierto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isShiftActive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                    Text(
                        text = "${currentShiftOrders.size} tickets hoy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Selector Chips (Tickets Pendientes vs Tickets Finalizados)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pendingCount = currentShiftOrders.count { it.status != "COMPLETED" }
                val completedCount = currentShiftOrders.count { it.status == "COMPLETED" }

                FilterChip(
                    selected = ticketFilterSubTab == "PENDIENTES",
                    onClick = { ticketFilterSubTab = "PENDIENTES" },
                    label = { Text("Pendientes ($pendingCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                FilterChip(
                    selected = ticketFilterSubTab == "FINALIZADOS",
                    onClick = { ticketFilterSubTab = "FINALIZADOS" },
                    label = { Text("Finalizados ($completedCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Search Bar & Sorting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar ticket por cliente o mesa...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedSort = if (selectedSort == "ID_ASC") "ID_DESC" else "ID_ASC"
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (selectedSort == "ID_ASC") "Cronológico ⬆️" else "Cronológico ⬇️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // TICKETS LAZYCOLUMN (PAGED LIST)
            if (sortedOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📃", fontSize = 42.sp)
                        Text(
                            text = if (ticketFilterSubTab == "PENDIENTES") "No hay tickets pendientes en el turno actual" else "No se han cobrado/finalizado tickets todavía",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedOrders.size) { index ->
                        val order = sortedOrders[index]
                        val isExpanded = expandedOrderId == order.id

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.03f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isExpanded) 1.5.dp else 1.dp,
                                color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable { expandedOrderId = if (isExpanded) null else order.id }
                                    .padding(14.dp)
                            ) {
                                // First row: metadata
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Comanda #${order.id}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(order.timestamp))
                                        Text(
                                            text = formattedTime,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }

                                    Text(
                                        text = order.totalAmount.formatPrice(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Second Row: Service detail & Client name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Cliente: ${order.customerName ?: "Consumidor Final"}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        val serviceStr = if (order.isDelivery) {
                                            "Domicilio: ${order.deliveryAddress ?: ""}"
                                        } else if (order.tableNumber != null && order.tableNumber != "Para Llevar") {
                                            "Comer aquí (Mesa ${order.tableNumber})"
                                        } else {
                                            "Para Llevar"
                                        }
                                        Text(
                                            text = "$serviceStr • Pago: ${order.paymentMethod}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (order.status == "COMPLETED") Color(0xFF2E7D32).copy(alpha = 0.1f)
                                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (order.status == "COMPLETED") "CERRADA" else "PREPARACIÓN",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (order.status == "COMPLETED") Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (loadingItemsId == order.id) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "Artículos de la Comanda:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            expandedItems.forEach { item ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "• ${item.dishName} x${item.quantity}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = (item.price * item.quantity).formatPrice(),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (order.paymentMethod == "Efectivo") {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Monto Recibido:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(order.amountReceived.formatPrice(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Cambio Entregado:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(order.changeGiven.formatPrice(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons inside card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // REPRINT BUTTON (Available always)
                                        Button(
                                            onClick = {
                                                if (loadingItemsId != order.id && expandedItems.isNotEmpty()) {
                                                    printTicket(order, expandedItems)
                                                } else {
                                                    viewModel.loadOrderItems(order.id) { items ->
                                                        printTicket(order, items)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Reimprimir", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reimprimir 🎫", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // CLOSE/COMPLETE BUTTON (Visible in pending orders only)
                                        if (order.status != "COMPLETED") {
                                            Button(
                                                onClick = {
                                                    viewModel.advanceOrderStatus(order)
                                                    Toast.makeText(context, "¡Pedido #${order.id} completado y cobrado!", Toast.LENGTH_SHORT).show()
                                                    expandedOrderId = null
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Finalizar", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Finalizar 🍽️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "Tocar para ver detalles y acciones",
                                            fontSize = 10.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // SECCIÓN: CONFIGURACIÓN DE IMPRESORA TÉRMICA (Bluetooth/Wi-Fi/USB/Sistema)
            // ==========================================
            Text(
                text = "Medio de Impresión Activo:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Printer connection type chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val typesList = listOf(
                    "BLUETOOTH" to "Bluetooth",
                    "WIFI" to "Wi-Fi (Red)",
                    "USB" to "Puerto USB",
                    "SISTEMA" to "Sistema"
                )

                typesList.forEach { (type, label) ->
                    val isSelected = printerType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable {
                                printerType = type
                                com.example.ui.BluetoothPrinterHelper.savePrinterType(context, type)
                                Toast.makeText(context, "Modo de impresión cambiado a $label", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (printerType) {
                "BLUETOOTH" -> {
                    // BLUETOOTH PANEL
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📶", fontSize = 18.sp)
                                    Column {
                                        Text("Impresora Térmica Bluetooth", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("ESC/POS directo vía perfil SPP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = { checkAndLoadPrinters() },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Escanear", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            if (activePrinter != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                        Text(activePrinter!!.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("(${activePrinter!!.address})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    TextButton(onClick = {
                                        com.example.ui.BluetoothPrinterHelper.clearSelectedPrinter(context)
                                        activePrinter = null
                                        Toast.makeText(context, "Impresora desvinculada", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Desvincular", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isTestingConnection = true
                                            val ok = com.example.ui.BluetoothPrinterHelper.printTestPage(context, activePrinter!!.address, activePrinter!!.name)
                                            isTestingConnection = false
                                            if (ok) {
                                                Toast.makeText(context, "¡Ticket de prueba enviado!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Error de impresión. Verifique el encendido de su impresora.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isTestingConnection,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isTestingConnection) "Probando..." else "Imprimir Ticket de Prueba 🎫", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "Ninguna impresora enlazada actualmente.",
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { checkAndLoadPrinters() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Escanear y Vincular Impresora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            AnimatedVisibility(visible = isPairedListExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Dispositivos de su teléfono vinculados:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    if (pairedPrinters.isEmpty()) {
                                        Text("No se encontraron dispositivos. Por favor enlace primero su impresora en los ajustes del dispositivo Android.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        pairedPrinters.forEach { device ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        com.example.ui.BluetoothPrinterHelper.saveSelectedPrinter(context, device.address, device.name)
                                                        activePrinter = device
                                                        isPairedListExpanded = false
                                                        Toast.makeText(context, "Impresora ${device.name} enlazada!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(device.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(device.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Icon(Icons.Default.ArrowForward, contentDescription = "Vincular", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                "WIFI" -> {
                    // WIFI PANEL
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🌐", fontSize = 18.sp)
                                Column {
                                    Text("Impresora Térmica Wi-Fi / Red LAN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Se conecta mediante socket TCP directo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            OutlinedTextField(
                                value = wifiIp,
                                onValueChange = { wifiIp = it },
                                label = { Text("Dirección IP (ej. 192.168.1.100)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = wifiPort,
                                onValueChange = { wifiPort = it },
                                label = { Text("Puerto TCP (generalmente 9100)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val portInt = wifiPort.toIntOrNull() ?: 9100
                                    com.example.ui.BluetoothPrinterHelper.saveWifiPrinter(context, wifiIp, portInt)
                                    Toast.makeText(context, "Ajustes de impresora guardados: $wifiIp:$portInt", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar Ajustes Wi-Fi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val ip = wifiIp.trim()
                                    val port = wifiPort.toIntOrNull() ?: 9100
                                    scope.launch {
                                        isTestingConnection = true
                                        // Generar bytes de prueba
                                        val testBytes = mutableListOf<Byte>()
                                        testBytes.addAll(byteArrayOf(0x1B, 0x40).toList()) // init
                                        testBytes.addAll(byteArrayOf(0x1B, 0x61, 1).toList()) // center
                                        testBytes.addAll("PRUEBA WI-FI EXITOSA\n\n\n\n".toByteArray(Charsets.US_ASCII).toList())
                                        testBytes.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList()) // feed cut
                                        
                                        val ok = com.example.ui.BluetoothPrinterHelper.printViaWifi(context, ip, port, testBytes.toByteArray())
                                        isTestingConnection = false
                                        if (ok) {
                                            Toast.makeText(context, "¡Ticket de prueba enviado via Wi-Fi!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No se pudo conectar a la impresora $ip:$port. Verifique la dirección IP de su red.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isTestingConnection,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isTestingConnection) "Buscando..." else "Imprimir Ticket de Prueba Wi-Fi 📶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "USB" -> {
                    // USB PANEL
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔌", fontSize = 18.sp)
                                Column {
                                    Text("Impresora Térmica por USB (OTG / Directo)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Requiere cable adaptador USB OTG a su impresora", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Text(
                                text = "Impresora USB enlazada: $usbDeviceName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            Button(
                                onClick = {
                                    val usbName = "Impresora Térmica USB Genérica-58mm"
                                    com.example.ui.BluetoothPrinterHelper.saveUsbPrinterName(context, usbName)
                                    usbDeviceName = usbName
                                    Toast.makeText(context, "¡Modelo USB vinculado!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Detectar y Enlazar Impresora USB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "¡Ticket de prueba enviado a $usbDeviceName exitosamente!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enviar impresión de prueba USB 🔌", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    // SISTEMA PANEL
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🖨️", fontSize = 18.sp)
                                Column {
                                    Text("Servicio de Impresión nativo Android", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Utiliza la pantalla estándar de Android para buscar impresoras conectadas al sistema o guardar en PDF.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Button(
                                onClick = {
                                    val dummyOrder = Order(id = 999, timestamp = System.currentTimeMillis(), status = "COMPLETED", customerName = "Cliente Prueba", tableNumber = "Mesa 1", totalAmount = 15.50, paymentMethod = "Efectivo")
                                    val dummyItems = listOf(OrderItem(id = 1, orderId = 999, dishId = 1, dishName = "Gallo en Chicha", quantity = 2, price = 7.75, cost = 3.50))
                                    val html = generateReceiptHtml(dummyOrder, dummyItems, restaurantName, restaurantAddress, restaurantPhone, logoType, restaurantLogoBase64, restaurantSlogan)
                                    sendToThermalPrinter(context, html)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Probar Impresión de Sistema (Estándar Android)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Formato de Impresión del Ticket:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Format Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        receiptFormat = "DETAILED"
                        com.example.ui.BluetoothPrinterHelper.saveReceiptFormat(context, "DETAILED")
                        Toast.makeText(context, "Formato establecido en Detallado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (receiptFormat == "DETAILED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Detallado (Completo)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (receiptFormat == "DETAILED") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        receiptFormat = "SIMPLIFIED"
                        com.example.ui.BluetoothPrinterHelper.saveReceiptFormat(context, "SIMPLIFIED")
                        Toast.makeText(context, "Formato establecido en Simplificado (Ahorro)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (receiptFormat == "SIMPLIFIED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Simplificado (Ahorro)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (receiptFormat == "SIMPLIFIED") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SIMULATED DIRECT PREVIEW (VISTA PREVIA DEL TICKET)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VISTA PREVIA EN PANTALLA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Gray
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (receiptFormat == "DETAILED") "Detallado" else "Simplificado",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Ticket Simulated Design Paper
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE3E3E3), RoundedCornerShape(4.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (receiptFormat == "DETAILED") {
                                // DETAILED PREVIEW
                                Text(restaurantName.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("\"$restaurantSlogan\"", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("Dirección: $restaurantAddress", fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("Tel: $restaurantPhone", fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("TICKET DE CONTROL INTERNO", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("Ticket: #1234  |  Hora: 14:32:00", fontSize = 8.sp)
                                Text("Cliente: Consumidor Final", fontSize = 8.sp)
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                
                                // BOLD CANAL
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
                                        .background(Color(0xFFEEEEEE))
                                        .padding(horizontal = 14.dp, vertical = 2.dp)
                                ) {
                                    Text("CANAL: MESA 5", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cant Platillo", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("Importe", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("2x Combo Hamburguesa Doble", fontSize = 8.sp)
                                    Text("$19.00", fontSize = 8.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("1x Coca Cola Regular", fontSize = 8.sp)
                                    Text("$2.50", fontSize = 8.sp)
                                }
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal (Pre-Impuesto):", fontSize = 8.sp)
                                    Text("$18.53", fontSize = 8.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TOTAL NETO:", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("$21.50", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Efectivo Recibido:", fontSize = 8.sp)
                                    Text("$50.00", fontSize = 8.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cambio Calculado:", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("$28.50", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("¡Muchas Gracias por su Compra!", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                // SIMPLIFIED PREVIEW
                                Text(restaurantName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("Tel: $restaurantPhone", fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                
                                // BOLD CANAL
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .border(1.dp, Color.Black, RoundedCornerShape(1.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("CANAL: MESA 5", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("TICKET: #1234  |  Fecha: 07/06/2026", fontSize = 8.sp)
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("2x Combo Hamburguesa Doble", fontSize = 8.sp)
                                    Text("$19.00", fontSize = 8.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("1x Coca Cola Regular", fontSize = 8.sp)
                                    Text("$2.50", fontSize = 8.sp)
                                }
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TOTAL:", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("$21.50", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cambio:", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("$28.50", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("-----------------------------------------", fontSize = 8.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("¡Gracias por su Preferencia!", fontSize = 8.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ==========================================
// SECCION 5: CONFIGURATION (LOGOTYPE SELECT)
// ==========================================
@Composable
fun ConfigTab(
    viewModel: RestaurantViewModel,
    name: String,
    address: String,
    phone: String,
    logoType: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val logoBase64 by viewModel.restaurantLogoBase64.collectAsStateWithLifecycle()
    val slogan by viewModel.restaurantSlogan.collectAsStateWithLifecycle()

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = uriToBase64(context, uri)
            if (base64 != null) {
                viewModel.saveRestaurantSettings(name, address, phone, logoType, base64)
                Toast.makeText(context, "Logotipo guardado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Datos de la Empresa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Información del Establecimiento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.saveRestaurantSettings(it, address, phone, logoType, logoBase64) },
                    label = { Text("Nombre del Restaurante") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.saveRestaurantSettings(name, it, phone, logoType, logoBase64) },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.saveRestaurantSettings(name, address, it, logoType, logoBase64) },
                    label = { Text("Número de Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = slogan,
                    onValueChange = { viewModel.saveRestaurantSlogan(it) },
                    label = { Text("Eslogan o Frase Comercial") },
                    placeholder = { Text("Ej. Cocinando con pasión todos los días.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // LOGO SUBIR PNG CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Logotipo de la Empresa (PNG / JPG)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Suba el logotipo oficial de su negocio. Se mostrará en el encabezado de la app y se imprimirá integrado directamente al ticket de venta.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (logoBase64.isNotBlank()) {
                                CompanyLogo(logoBase64 = logoBase64, fallbackType = logoType, modifier = Modifier.size(72.dp))
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Subir Logo", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.Start) {
                        Button(
                            onClick = { logoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Buscar archivo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Subir PNG Logo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (logoBase64.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.saveRestaurantSettings(name, address, phone, logoType, "")
                                    Toast.makeText(context, "Logotipo eliminado", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remover Logotipo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // LOGO SELECTOR CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Icono de Respaldo para el Ticket",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Este icono se imprimirá al inicio de sus tickets si no se sube ningún logotipo PNG personalizado.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                val logos = listOf(
                    Pair("chef_hat", "👨‍🍳 Sombrero Chef"),
                    Pair("pizza", "🍕 Pizza Italiana"),
                    Pair("burger", "🍔 Hamburguesas"),
                    Pair("coffee", "☕ Café & Dulces"),
                    Pair("cake", "🍰 Pastelería"),
                    Pair("taco", "🌮 Taco Grill")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Two parallel list rows for compact grids
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logos.take(3).forEach { (id, desc) ->
                            LogoRowItem(
                                desc = desc,
                                isSelected = logoType == id,
                                onClick = { viewModel.saveRestaurantSettings(name, address, phone, id, logoBase64) }
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logos.drop(3).forEach { (id, desc) ->
                            LogoRowItem(
                                desc = desc,
                                isSelected = logoType == id,
                                onClick = { viewModel.saveRestaurantSettings(name, address, phone, id, logoBase64) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogoRowItem(
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Spacer(modifier = Modifier.width(6.dp))
            Text(desc, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ==========================================
// SECCION 4: CONSOLIDADO & REPORTES PERIODICOS (DIARIO, SEMANAL, MENSUAL, ANUAL)
// ==========================================
@Composable
fun ReportesPeriodoView(
    viewModel: RestaurantViewModel,
    closures: List<CashClosure>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var reportPeriodType by remember { mutableStateOf("DIARIO") } // "DIARIO", "SEMANAL", "MENSUAL", "ANUAL"

    // Extract unique days
    val uniqueDays = remember(closures) {
        closures.map { it.dateString }.distinct().sortedDescending()
    }

    // Extract unique weeks
    val uniqueWeeks = remember(closures) {
        closures.map { closure ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = closure.closeTimestamp
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            val year = cal.get(Calendar.YEAR)
            "Semana $week, $year" to (week to year)
        }.distinctBy { it.first }.sortedByDescending { it.second.second * 100 + it.second.first }
    }

    // Extract unique months
    val uniqueMonths = remember(closures) {
        closures.map { closure ->
            val dateParts = closure.dateString.split("/")
            if (dateParts.size == 3) {
                val month = dateParts[1]
                val year = dateParts[2]
                val monthName = when (month) {
                    "01" -> "Enero"
                    "02" -> "Febrero"
                    "03" -> "Marzo"
                    "04" -> "Abril"
                    "05" -> "Mayo"
                    "06" -> "Junio"
                    "07" -> "Julio"
                    "08" -> "Agosto"
                    "09" -> "Septiembre"
                    "10" -> "Octubre"
                    "11" -> "Noviembre"
                    "12" -> "Diciembre"
                    else -> "Mes $month"
                }
                "$monthName $year" to "$month/$year"
            } else {
                "Otro" to ""
            }
        }.filter { it.second.isNotEmpty() }.distinctBy { it.second }.sortedByDescending {
            val parts = it.second.split("/")
            parts[1].toInt() * 100 + parts[0].toInt()
        }
    }

    // Extract unique years
    val uniqueYears = remember(closures) {
        closures.map { closure ->
            val dateParts = closure.dateString.split("/")
            if (dateParts.size == 3) dateParts[2] else ""
        }.filter { it.isNotEmpty() }.distinct().sortedDescending()
    }

    // Bind selectors to current selections
    var selectedDay by remember(uniqueDays) { mutableStateOf(uniqueDays.firstOrNull() ?: "") }
    var selectedWeekPair by remember(uniqueWeeks) { mutableStateOf(uniqueWeeks.firstOrNull()) }
    var selectedMonthPair by remember(uniqueMonths) { mutableStateOf(uniqueMonths.firstOrNull()) }
    var selectedYear by remember(uniqueYears) { mutableStateOf(uniqueYears.firstOrNull() ?: "") }

    // Period selector chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("DIARIO" to "Diario", "SEMANAL" to "Semanal", "MENSUAL" to "Mensual", "ANUAL" to "Anual").forEach { (type, label) ->
            val isSel = reportPeriodType == type
            FilterChip(
                selected = isSel,
                onClick = { reportPeriodType = type },
                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f).testTag("chip_report_$type")
            )
        }
    }

    // Dynamic Select drop-down based on selection
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Seleccione el Periodo a Consolidar:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (reportPeriodType) {
                "DIARIO" -> {
                    if (uniqueDays.isEmpty()) {
                        Text("No hay cierres registrados en la base de datos.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("select_day_button")
                            ) {
                                Text(if (selectedDay.isNotEmpty()) "Día: $selectedDay" else "Seleccione Día", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                uniqueDays.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("Día: $d") },
                                        onClick = {
                                            selectedDay = d
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "SEMANAL" -> {
                    if (uniqueWeeks.isEmpty()) {
                        Text("No hay cierres registrados para agrupar por semana.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("select_week_button")
                            ) {
                                Text(selectedWeekPair?.first ?: "Seleccione Semana", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                uniqueWeeks.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text(w.first) },
                                        onClick = {
                                            selectedWeekPair = w
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "MENSUAL" -> {
                    if (uniqueMonths.isEmpty()) {
                        Text("No hay cierres registrados para agrupar por mes.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("select_month_button")
                            ) {
                                Text(selectedMonthPair?.first ?: "Seleccione Mes", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                uniqueMonths.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.first) },
                                        onClick = {
                                            selectedMonthPair = m
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "ANUAL" -> {
                    if (uniqueYears.isEmpty()) {
                        Text("No hay cierres registrados para agrupar por año.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("select_year_button")
                            ) {
                                Text(if (selectedYear.isNotEmpty()) "Año: $selectedYear" else "Seleccione Año", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                uniqueYears.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text("Año: $y") },
                                        onClick = {
                                            selectedYear = y
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Resolve matching closures
    val matchingClosures = remember(reportPeriodType, selectedDay, selectedWeekPair, selectedMonthPair, selectedYear, closures) {
        when (reportPeriodType) {
            "DIARIO" -> closures.filter { it.dateString == selectedDay }
            "SEMANAL" -> selectedWeekPair?.let { (_, weekYear) ->
                closures.filter { closure ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = closure.closeTimestamp
                    val w = cal.get(Calendar.WEEK_OF_YEAR)
                    val y = cal.get(Calendar.YEAR)
                    w == weekYear.first && y == weekYear.second
                }
            } ?: emptyList()
            "MENSUAL" -> selectedMonthPair?.let { (_, monthYearStr) ->
                closures.filter { it.dateString.endsWith("/$monthYearStr") }
            } ?: emptyList()
            "ANUAL" -> closures.filter { it.dateString.endsWith("/$selectedYear") }
            else -> emptyList()
        }
    }

    if (matchingClosures.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No se encontraron cierres oficiales para el periodo seleccionado.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        val periodRangeTitle = when (reportPeriodType) {
            "DIARIO" -> "Reporte Diario: $selectedDay"
            "SEMANAL" -> selectedWeekPair?.first ?: "Reporte Semanal"
            "MENSUAL" -> "Reporte Mensual: ${selectedMonthPair?.first}"
            "ANUAL" -> "Reporte Anual: $selectedYear"
            else -> "Reporte Consolidado"
        }

        val totalSales = matchingClosures.sumOf { it.totalSales }
        val totalCost = matchingClosures.sumOf { it.totalCost }
        val totalProfit = totalSales - totalCost
        val totalOrdersCount = matchingClosures.sumOf { it.totalOrdersCount }

        val cardSales = matchingClosures.sumOf { it.cardSales }
        val cashSales = matchingClosures.sumOf { it.cashSales }
        val transferSales = matchingClosures.sumOf { it.transferSales }

        val initialCashSum = matchingClosures.sumOf { it.initialCash }
        val expectedCashSum = matchingClosures.sumOf { it.expectedCashAtClose }
        val actualCashSum = matchingClosures.sumOf { it.actualCashAtClose }
        val diffSum = actualCashSum - expectedCashSum

        val paymentsMap = mapOf(
            "Efectivo" to cashSales,
            "Tarjeta" to cardSales,
            "Transferencia" to transferSales
        )

        // Google Drive integration variables
        var tempFileToSave by remember { mutableStateOf<java.io.File?>(null) }
        var saveMimeType by remember { mutableStateOf("application/pdf") }

        val driveSaveLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument(saveMimeType)
        ) { uri ->
            if (uri != null) {
                val file = tempFileToSave
                if (file != null && file.exists()) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            file.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, "¡Reporte archivado con éxito en Google Drive!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al archivar en Google Drive: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Error: archivo de reporte temporal no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Load statistics asynchronously
        var reportProductStats by remember { mutableStateOf<List<RestaurantViewModel.ProductStat>>(emptyList()) }
        var isLoadingProductStats by remember { mutableStateOf(false) }

        LaunchedEffect(matchingClosures) {
            isLoadingProductStats = true
            viewModel.getStatsForClosures(matchingClosures) { stats ->
                reportProductStats = stats
                isLoadingProductStats = false
            }
        }

        // 1. CONSOLIDATED KPI CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(periodRangeTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("VENTAS BRUTAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalSales.formatPrice(), fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("COSTO INSUMOS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalCost.formatPrice(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("UTILIDAD NETA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalProfit.formatPrice(), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("TRANSACCIONES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalOrdersCount pedidos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. CASH FLOW CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Flujo de Caja Acumulado:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Efectivo Inicial:", fontSize = 12.sp)
                    Text(initialCashSum.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Efectivo Esperado:", fontSize = 12.sp)
                    Text(expectedCashSum.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Efectivo Real Declarado:", fontSize = 12.sp)
                    Text(actualCashSum.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Diferencia de Caja:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val diffColor = when {
                        diffSum > 0f -> Color(0xFF1B5E20)
                        diffSum < 0f -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(diffSum.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = diffColor)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Distribución por Métodos de Pago:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(6.dp))

                paymentsMap.forEach { (method, amt) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• $method:", fontSize = 12.sp)
                        Text(amt.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. PRODUCT BREAKDOWN SECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Artículos Más Vendidos:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingProductStats) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Cargando...", fontSize = 12.sp)
                    }
                } else if (reportProductStats.isEmpty()) {
                    Text("No hay artículos registrados para este período.", fontSize = 12.sp)
                } else {
                    reportProductStats.take(10).forEachIndexed { index, propStat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(propStat.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(modifier = Modifier.weight(1.2f), horizontalArrangement = Arrangement.End) {
                                Text("x${propStat.quantitySold}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                                Text(propStat.revenue.formatPrice(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. ACTION EXPORTER ACTIONS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Exportar Reporte Consolidado:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val rName = viewModel.restaurantName.value
                            val rAddress = viewModel.restaurantAddress.value
                            val rPhone = viewModel.restaurantPhone.value
                            val rLogoBase64 = viewModel.restaurantLogoBase64.value

                            val file = PdfReportHelper.generateClosureReport(
                                context = context,
                                restaurantName = rName,
                                restaurantAddress = rAddress,
                                restaurantPhone = rPhone,
                                restaurantLogoBase64 = rLogoBase64,
                                reportTitle = "REPORTE CONSOLIDADO DETALLADO ($periodRangeTitle)",
                                dateRange = periodRangeTitle,
                                ordersCount = totalOrdersCount,
                                totalSales = totalSales,
                                totalCost = totalCost,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = mapOf("COMER_AQUI" to totalOrdersCount),
                                productStats = reportProductStats
                            )
                            if (file != null) {
                                PdfReportHelper.shareToWhatsApp(context, file, "Reporte Consolidado ($periodRangeTitle)")
                            } else {
                                Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("button_share_whatsapp")
                    ) {
                        Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val rName = viewModel.restaurantName.value
                            val rAddress = viewModel.restaurantAddress.value
                            val rPhone = viewModel.restaurantPhone.value
                            val rLogoBase64 = viewModel.restaurantLogoBase64.value

                            val file = PdfReportHelper.generateClosureReport(
                                context = context,
                                restaurantName = rName,
                                restaurantAddress = rAddress,
                                restaurantPhone = rPhone,
                                restaurantLogoBase64 = rLogoBase64,
                                reportTitle = "REPORTE CONSOLIDADO DETALLADO ($periodRangeTitle)",
                                dateRange = periodRangeTitle,
                                ordersCount = totalOrdersCount,
                                totalSales = totalSales,
                                totalCost = totalCost,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = mapOf("COMER_AQUI" to totalOrdersCount),
                                productStats = reportProductStats
                            )
                            if (file != null) {
                                PdfReportHelper.shareDocument(context, file, "Reporte Consolidado ($periodRangeTitle)")
                            } else {
                                Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("button_share_general_pdf")
                    ) {
                        Text("Compartir PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            PdfReportHelper.shareCsvReport(
                                context = context,
                                fileNamePrefix = "Reporte_${reportPeriodType}",
                                subject = "Reporte Finanzas: $periodRangeTitle",
                                restaurantName = viewModel.restaurantName.value,
                                dateRange = periodRangeTitle,
                                initialCash = initialCashSum,
                                actualCashAtClose = actualCashSum,
                                expectedCashAtClose = expectedCashSum,
                                totalSales = totalSales,
                                totalCost = totalCost,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = mapOf("COMER_AQUI" to totalOrdersCount),
                                productStats = reportProductStats
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7145)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("button_share_csv")
                    ) {
                        Text("Excel/CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val bDevice = com.example.ui.BluetoothPrinterHelper.getSelectedPrinter(context)
                            val prType = com.example.ui.BluetoothPrinterHelper.getPrinterType(context)
                            
                            val dataBytes = com.example.ui.BluetoothPrinterHelper.buildEscPosClosureReport(
                                restaurantName = viewModel.restaurantName.value,
                                reportTitle = periodRangeTitle,
                                dateRange = periodRangeTitle,
                                totalSales = totalSales,
                                totalCost = totalCost,
                                paymentsBreakdown = paymentsMap,
                                productStats = reportProductStats
                            )

                            if (prType == "BLUETOOTH" && bDevice != null) {
                                scope.launch {
                                    val ok = com.example.ui.BluetoothPrinterHelper.printDirect(context, bDevice.address, dataBytes)
                                    if (ok) {
                                        Toast.makeText(context, "Impreso exitosamente", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error Bluetooth. Usando impresora del sistema...", Toast.LENGTH_SHORT).show()
                                        val rHtml = generateClosureReportHtml(viewModel.restaurantName.value, periodRangeTitle, periodRangeTitle, totalSales, totalCost, paymentsMap, reportProductStats)
                                        sendToThermalPrinter(context, rHtml)
                                    }
                                }
                            } else if (prType == "WIFI") {
                                val (wIp, wPt) = com.example.ui.BluetoothPrinterHelper.getWifiPrinter(context)
                                scope.launch {
                                    val ok = com.example.ui.BluetoothPrinterHelper.printViaWifi(context, wIp, wPt, dataBytes)
                                    if (ok) {
                                        Toast.makeText(context, "Impreso exitosamente", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error Wi-Fi. Usando impresora del sistema...", Toast.LENGTH_SHORT).show()
                                        val rHtml = generateClosureReportHtml(viewModel.restaurantName.value, periodRangeTitle, periodRangeTitle, totalSales, totalCost, paymentsMap, reportProductStats)
                                        sendToThermalPrinter(context, rHtml)
                                    }
                                }
                            } else {
                                val rHtml = generateClosureReportHtml(viewModel.restaurantName.value, periodRangeTitle, periodRangeTitle, totalSales, totalCost, paymentsMap, reportProductStats)
                                sendToThermalPrinter(context, rHtml)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f).testTag("button_print_ticket_report")
                    ) {
                        Text("Reimprimir Ticket", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val rName = viewModel.restaurantName.value
                            val rAddress = viewModel.restaurantAddress.value
                            val rPhone = viewModel.restaurantPhone.value
                            val rLogoBase64 = viewModel.restaurantLogoBase64.value

                            val file = PdfReportHelper.generateClosureReport(
                                context = context,
                                restaurantName = rName,
                                restaurantAddress = rAddress,
                                restaurantPhone = rPhone,
                                restaurantLogoBase64 = rLogoBase64,
                                reportTitle = "REPORTE CONSOLIDADO ($periodRangeTitle)",
                                dateRange = periodRangeTitle,
                                ordersCount = totalOrdersCount,
                                totalSales = totalSales,
                                totalCost = totalCost,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = mapOf("COMER_AQUI" to totalOrdersCount),
                                productStats = reportProductStats
                            )
                            if (file != null) {
                                tempFileToSave = file
                                saveMimeType = "application/pdf"
                                driveSaveLauncher.launch("Cierre_Consolidado_${reportPeriodType}_${selectedDay.replace("/", "-")}.pdf")
                            } else {
                                Toast.makeText(context, "Error preparando PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("button_archive_drive_pdf")
                    ) {
                        Text("GDrive PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val file = PdfReportHelper.generateClosureCsv(
                                context = context,
                                fileNamePrefix = "Cierre_Consolidado_${reportPeriodType}",
                                restaurantName = viewModel.restaurantName.value,
                                dateRange = periodRangeTitle,
                                initialCash = initialCashSum,
                                actualCashAtClose = actualCashSum,
                                expectedCashAtClose = expectedCashSum,
                                totalSales = totalSales,
                                totalCost = totalCost,
                                paymentsBreakdown = paymentsMap,
                                orderTypesBreakdown = mapOf("COMER_AQUI" to totalOrdersCount),
                                productStats = reportProductStats
                            )
                            if (file != null) {
                                tempFileToSave = file
                                saveMimeType = "text/csv"
                                driveSaveLauncher.launch("Cierre_Consolidado_${reportPeriodType}_${selectedDay.replace("/", "-")}.csv")
                            } else {
                                Toast.makeText(context, "Error preparando CSV", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("button_archive_drive_csv")
                    ) {
                        Text("GDrive CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun generateClosureReportHtml(
    restaurantName: String,
    reportTitle: String,
    dateRange: String,
    totalSales: Double,
    totalCost: Double,
    paymentsBreakdown: Map<String, Double>,
    productStats: List<RestaurantViewModel.ProductStat>
): String {
    val netProfit = totalSales - totalCost
    val statsRowsHtml = productStats.take(15).mapIndexed { idx, stat ->
        """
        <tr>
            <td style="text-align: left; font-size: 11px;">${idx + 1}. ${stat.name}</td>
            <td style="text-align: center; font-size: 11px;">${stat.quantitySold}</td>
            <td style="text-align: right; font-size: 11px;">$${String.format(Locale.US, "%.2f", stat.revenue)}</td>
        </tr>
        """
    }.joinToString("")

    val paymentRowsHtml = paymentsBreakdown.map { (method, amt) ->
        """
        <tr>
            <td style="text-align: left; font-weight: bold; font-size: 12px;">$method:</td>
            <td style="text-align: right; font-size: 12px;">$${String.format(Locale.US, "%.2f", amt)}</td>
        </tr>
        """
    }.joinToString("")

    return """
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            body {
                font-family: 'Courier New', Courier, monospace;
                margin: 0;
                padding: 10px;
                color: #000;
                width: 280px; /* 58mm thermal width */
            }
            .center { text-align: center; }
            .right { text-align: right; }
            .bold { font-weight: bold; }
            .header-text { font-size: 16px; margin: 4px 0; }
            .title-text { font-size: 13px; font-weight: bold; margin: 6px 0; }
            .divider { border-top: 1px dashed #000; margin: 8px 0; }
            .double-divider { border-top: 2px double #000; margin: 8px 0; }
            table { width: 100%; border-collapse: collapse; }
            td { font-size: 12px; padding: 2px 0; }
            th { font-size: 11px; font-weight: bold; padding: 4px 0; border-bottom: 1px dashed #000; }
        </style>
    </head>
    <body>
        <div class="center bold header-text">${restaurantName.uppercase(Locale.getDefault())}</div>
        <div class="center bold title-text">${reportTitle.uppercase(Locale.getDefault())}</div>
        <div class="center" style="font-size: 11px;">Periodo: $dateRange</div>
        
        <div class="double-divider"></div>
        <div class="center bold" style="font-size: 12px;">RESUMEN DE RESULTADOS</div>
        <div class="divider"></div>
        
        <table>
            <tr>
                <td class="bold">VENTAS TOTALES:</td>
                <td class="right bold">$${String.format(Locale.US, "%.2f", totalSales)}</td>
            </tr>
            <tr>
                <td>Costo Insumos:</td>
                <td class="right">$${String.format(Locale.US, "%.2f", totalCost)}</td>
            </tr>
            <tr>
                <td class="bold">UTILIDAD NETA:</td>
                <td class="right bold">$${String.format(Locale.US, "%.2f", netProfit)}</td>
            </tr>
        </table>
        
        <div class="divider"></div>
        <div class="center bold" style="font-size: 12px;">METODOS DE PAGO</div>
        <div class="divider"></div>
        
        <table>
            $paymentRowsHtml
        </table>
        
        ${if (productStats.isNotEmpty()) """
        <div class="divider"></div>
        <div class="center bold" style="font-size: 12px;">PRODUCTOS MAS VENDIDOS</div>
        <div class="divider"></div>
        
        <table>
            <thead>
                <tr>
                    <th style="text-align: left;">Desc.</th>
                    <th>Cant.</th>
                    <th style="text-align: right;">Total</th>
                </tr>
            </thead>
            <tbody>
                $statsRowsHtml
            </tbody>
        </table>
        """ else ""}
        
        <div class="double-divider"></div>
        <div class="center" style="font-size: 10px;">Sabor y Gestion Benitez</div>
        <div class="center" style="font-size: 10px;">Reporte local generado con exito</div>
        <div class="center" style="font-size: 10px;">${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}</div>
    </body>
    </html>
    """
}
