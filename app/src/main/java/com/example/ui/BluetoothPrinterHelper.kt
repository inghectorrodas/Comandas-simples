package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.Order
import com.example.data.OrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object BluetoothPrinterHelper {
    private const val TAG = "BTPrinterHelper"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Key for storing the chosen printer MAC address in Shared Preferences
    private const val PREFS_NAME = "printer_prefs"
    private const val KEY_PRINTER_MAC = "selected_printer_mac"
    private const val KEY_PRINTER_NAME = "selected_printer_name"

    private const val KEY_PRINTER_TYPE = "printer_type" // "BLUETOOTH", "WIFI", "USB"
    private const val KEY_WIFI_IP = "printer_wifi_ip"
    private const val KEY_WIFI_PORT = "printer_wifi_port"
    private const val KEY_USB_NAME = "printer_usb_name"
    private const val KEY_RECEIPT_FORMAT = "receipt_format" // "SIMPLIFIED", "DETAILED"

    /**
     * Data class to represent a Bluetooth device for selection UI
     */
    data class PrinterDevice(val name: String, val address: String)

    /**
     * Checks if Bluetooth permissions are granted.
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Lists all paired Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<PrinterDevice> {
        val printerList = mutableListOf<PrinterDevice>()
        if (!hasBluetoothPermission(context)) {
            Log.w(TAG, "getPairedPrinters: Permiso Bluetooth no otorgado")
            return printerList
        }

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                pairedDevices?.forEach { device ->
                    val name = device.name ?: "Dispositivo Desconocido"
                    val address = device.address
                    printerList.add(PrinterDevice(name, address))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar dispositivos pareados", e)
        }
        return printerList
    }

    /**
     * Checks if Bluetooth is supported and enabled on the devices.
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter ?: BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter != null && bluetoothAdapter.isEnabled
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Saves selected printer info in SharedPreferences
     */
    fun saveSelectedPrinter(context: Context, macAddress: String, name: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_PRINTER_MAC, macAddress)
            putString(KEY_PRINTER_NAME, name)
            apply()
        }
    }

    /**
     * Retrieves stored selected printer credentials.
     */
    fun getSelectedPrinter(context: Context): PrinterDevice? {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mac = sharedPref.getString(KEY_PRINTER_MAC, null)
        val name = sharedPref.getString(KEY_PRINTER_NAME, null)
        return if (mac != null && name != null) {
            PrinterDevice(name, mac)
        } else {
            null
        }
    }

    /**
     * Clears stored chosen printer.
     */
    fun clearSelectedPrinter(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(KEY_PRINTER_MAC)
            remove(KEY_PRINTER_NAME)
            apply()
        }
    }

    fun getPrinterType(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_PRINTER_TYPE, "BLUETOOTH") ?: "BLUETOOTH"
    }

    fun savePrinterType(context: Context, type: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(KEY_PRINTER_TYPE, type).apply()
    }

    fun getWifiPrinter(context: Context): Pair<String, Int> {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ip = sharedPref.getString(KEY_WIFI_IP, "192.168.1.100") ?: "192.168.1.100"
        val port = sharedPref.getInt(KEY_WIFI_PORT, 9100)
        return Pair(ip, port)
    }

    fun saveWifiPrinter(context: Context, ip: String, port: Int) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(KEY_WIFI_IP, ip).putInt(KEY_WIFI_PORT, port).apply()
    }

    fun getUsbPrinterName(context: Context): String? {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_USB_NAME, null)
    }

    fun saveUsbPrinterName(context: Context, name: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(KEY_USB_NAME, name).apply()
    }

    fun clearUsbPrinter(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().remove(KEY_USB_NAME).apply()
    }

    fun getReceiptFormat(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_RECEIPT_FORMAT, "DETAILED") ?: "DETAILED"
    }

    fun saveReceiptFormat(context: Context, format: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(KEY_RECEIPT_FORMAT, format).apply()
    }

    /**
     * Establishes a TCP socket connection and transmits standard ESC/POS bytes asynchronously via Wi-Fi.
     */
    suspend fun printViaWifi(context: Context, ip: String, port: Int, printData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        var socket: java.net.Socket? = null
        var outputStream: java.io.OutputStream? = null
        var success = false
        try {
            socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, port), 4000) // 4 seconds timeout
            outputStream = socket.getOutputStream()
            outputStream.write(printData)
            outputStream.flush()
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing via Wi-Fi to $ip:$port", e)
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (t: Throwable) {
                // Ignore
            }
        }
        return@withContext success
    }

    /**
     * Clean accents and special characters to display perfectly without gibberish on termal printers.
     */
    fun sanitizeSpanishText(text: String): String {
        return text
            .replace('ñ', 'n')
            .replace('Ñ', 'N')
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('Á', 'A')
            .replace('É', 'E')
            .replace('Í', 'I')
            .replace('Ó', 'O')
            .replace('Ú', 'U')
            .replace('ü', 'u')
            .replace('Ü', 'U')
            .replace("¿", "")
            .replace("¡", "")
    }

    /**
     * Generates ESC/POS bytes for a standard printed invoice ticket (58mm or 80mm widths supported).
     * 58mm uses ~32 characters per line, 80mm uses ~48 characters per line.
     * We'll default to 58mm sizing limit but dynamically align nicely.
     */
    fun buildEscPosReceipt(
        restaurantName: String,
        restaurantAddress: String,
        restaurantPhone: String,
        restaurantSlogan: String,
        order: Order,
        items: List<OrderItem>,
        isSimplified: Boolean = false
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // ESC/POS Command constants
        val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
        val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0) // ESC a 0
        val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 1) // ESC a 1
        val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 2) // ESC a 2
        
        // Font attributes: ESC ! n
        val FONT_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
        val FONT_BOLD = byteArrayOf(0x1B, 0x21, 0x08)
        val FONT_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)
        val FONT_BOLD_DOUBLE = byteArrayOf(0x1B, 0x21, 0x38)

        // Utility to add string to bytes with sanitizer
        fun addTextLine(text: String, alignment: ByteArray = CMD_ALIGN_LEFT, font: ByteArray = FONT_NORMAL) {
            bytes.addAll(alignment.toList())
            bytes.addAll(font.toList())
            val sanitized = sanitizeSpanishText(text) + "\n"
            bytes.addAll(sanitized.toByteArray(Charsets.US_ASCII).toList())
        }

        fun addNewLine() {
            bytes.addAll("\n".toByteArray(Charsets.US_ASCII).toList())
        }

        fun addSeparator() {
            // Standard 58mm has 32 chars width separator
            bytes.addAll(CMD_ALIGN_LEFT.toList())
            bytes.addAll(FONT_NORMAL.toList())
            bytes.addAll("--------------------------------\n".toByteArray(Charsets.US_ASCII).toList())
        }

        fun addDoubleSeparator() {
            bytes.addAll(CMD_ALIGN_LEFT.toList())
            bytes.addAll(FONT_NORMAL.toList())
            bytes.addAll("================================\n".toByteArray(Charsets.US_ASCII).toList())
        }

        // Initialize Printer
        bytes.addAll(CMD_INIT.toList())

        val isMesaGral = order.tableNumber?.trim()?.lowercase() == "mesa gral" || order.tableNumber?.trim()?.lowercase() == "mesa general" || order.tableNumber.isNullOrBlank()
        val displayChannelLabelForPrinting = if (order.isDelivery) {
            "DOMICILIO"
        } else if (order.tableNumber != null && order.tableNumber?.lowercase()?.contains("llevar") == true) {
            "PARA LLEVAR"
        } else {
            if (isMesaGral) {
                "COMER AQUÍ"
            } else {
                val tableTrimmed = order.tableNumber?.trim()?.uppercase() ?: ""
                if (tableTrimmed.contains("MESA")) {
                    tableTrimmed
                } else {
                    "MESA $tableTrimmed"
                }
            }
        }

        if (isSimplified) {
            // ==========================================
            // SIMPLIFIED TICKET
            // ==========================================
            // Centered Header
            addTextLine(restaurantName.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD)
            if (restaurantPhone.isNotEmpty()) {
                addTextLine("Tel: $restaurantPhone", CMD_ALIGN_CENTER, FONT_NORMAL)
            }
            addSeparator()

            // Large Bold Channel Assignment
            addTextLine(displayChannelLabelForPrinting, CMD_ALIGN_CENTER, FONT_BOLD_DOUBLE)
            val clientNameForPrinting = order.customerName?.trim()
            if (!clientNameForPrinting.isNullOrBlank()) {
                addTextLine(clientNameForPrinting.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD)
            }
            addSeparator()

            // Essential sales metadata
            addTextLine("TICKET #${order.id}", CMD_ALIGN_LEFT, FONT_BOLD)
            val formatDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            addTextLine("Fecha: ${formatDateTime.format(Date(order.timestamp))}", CMD_ALIGN_LEFT, FONT_NORMAL)
            addSeparator()

            // Compact purchase details (Simple qty and names on one line)
            items.forEach { item ->
                val qtyStr = "${item.quantity}x ".padEnd(4)
                val cleanName = sanitizeSpanishText(item.dishName)
                val totalDouble = item.price * item.quantity
                val priceStr = String.format(Locale.US, "$%.2f", totalDouble)

                // Limit characters to avoid wrap issues in 58mm
                val maxNameLength = 18
                val displayName = if (cleanName.length > maxNameLength) cleanName.substring(0, maxNameLength) else cleanName.padEnd(maxNameLength)
                addTextLine(qtyStr + displayName + priceStr.padStart(10), CMD_ALIGN_LEFT, FONT_NORMAL)
            }
            addSeparator()

            // Shorter totals area
            val totalFormatted = String.format(Locale.US, "$%.2f", order.totalAmount)
            addTextLine("TOTAL: $totalFormatted", CMD_ALIGN_RIGHT, FONT_BOLD)
            addTextLine("Metodo Pago: ${order.paymentMethod}", CMD_ALIGN_RIGHT, FONT_NORMAL)

            if (order.paymentMethod == "Efectivo") {
                val recFormatted = String.format(Locale.US, "$%.2f", order.amountReceived)
                val chgFormatted = String.format(Locale.US, "$%.2f", order.changeGiven)
                addTextLine("Efectivo Entregado: $recFormatted", CMD_ALIGN_RIGHT, FONT_NORMAL)
                addTextLine("Cambio: $chgFormatted", CMD_ALIGN_RIGHT, FONT_BOLD)
            }

            addSeparator()
            addTextLine("¡Gracias por su Preferencia!", CMD_ALIGN_CENTER, FONT_NORMAL)
        } else {
            // ==========================================
            // DETAILED TICKET
            // ==========================================
            // 1. ENCABEZADO COMERCIAL
            addTextLine(restaurantName.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD_DOUBLE)
            if (restaurantSlogan.isNotEmpty()) {
                addTextLine("\"$restaurantSlogan\"", CMD_ALIGN_CENTER, FONT_NORMAL)
            }
            if (restaurantAddress.isNotEmpty()) {
                addTextLine("Dir: $restaurantAddress", CMD_ALIGN_CENTER, FONT_NORMAL)
            }
            if (restaurantPhone.isNotEmpty()) {
                addTextLine("Tel: $restaurantPhone", CMD_ALIGN_CENTER, FONT_NORMAL)
            }
            addSeparator()

            // 2. IDENTIFICACION FISCAL (DOCUMENT NATURE, METADATA & CHANNEL)
            addTextLine("TICKET DE VENTA", CMD_ALIGN_CENTER, FONT_BOLD)

            val formatDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateString = formatDateTime.format(Date(order.timestamp))
            addTextLine("Ticket Nro:  #${order.id}", CMD_ALIGN_LEFT, FONT_BOLD)
            addTextLine("Fecha/Hora:  $dateString", CMD_ALIGN_LEFT, FONT_NORMAL)
            
            val detailService = if (order.isDelivery) {
                "Entrega a Domicilio: ${order.deliveryAddress ?: ""}"
            } else if (order.tableNumber != null && order.tableNumber != "Para Llevar") {
                "Servicio de Mesa: ${order.tableNumber}"
            } else {
                "Retira en Sucursal"
            }
            addTextLine("Referencia:  $detailService", CMD_ALIGN_LEFT, FONT_NORMAL)
            addSeparator()

            // LARGE BOLD CHANNEL ROW (CANAL DE ASIGNACION)
            addTextLine(displayChannelLabelForPrinting, CMD_ALIGN_CENTER, FONT_BOLD_DOUBLE)
            val clientNameDetailedForPrinting = order.customerName?.trim()
            if (!clientNameDetailedForPrinting.isNullOrBlank()) {
                addTextLine(clientNameDetailedForPrinting.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD)
            }
            addSeparator()

            // 3. DETALLE DE COMPRA TABULAR
            // Columns: Cant (5) Platillo (17) Importe (10)
            addTextLine("Cant  Platillo          Importe", CMD_ALIGN_LEFT, FONT_BOLD)
            addSeparator()

            items.forEach { item ->
                val qtyStr = "${item.quantity}x".padEnd(5)
                val cleanName = sanitizeSpanishText(item.dishName)
                val totalDouble = item.price * item.quantity
                val priceStr = String.format(Locale.US, "$%.2f", totalDouble)

                val maxNameChar = 17
                val finalName = if (cleanName.length > maxNameChar) cleanName.substring(0, maxNameChar) else cleanName.padEnd(maxNameChar)
                val line = qtyStr + finalName + priceStr.padStart(10)
                
                bytes.addAll(CMD_ALIGN_LEFT.toList())
                bytes.addAll(FONT_NORMAL.toList())
                bytes.addAll((line + "\n").toByteArray(Charsets.US_ASCII).toList())
            }
            addSeparator()

            // 4. CUERPO FINANCIERO (SUBTOTAL, TAXES & PAYMENTS)
            // Simulating a standard tax (e.g. 16% IVA included)
            val subtotalVal = order.totalAmount / 1.16
            val taxVal = order.totalAmount - subtotalVal
            
            val subtotalFormatted = String.format(Locale.US, "$%.2f", subtotalVal)
            val taxFormatted = String.format(Locale.US, "$%.2f", taxVal)
            val totalFormatted = String.format(Locale.US, "$%.2f", order.totalAmount)

            addTextLine("TOTAL NETO A PAGAR:       $totalFormatted", CMD_ALIGN_RIGHT, FONT_BOLD)
            addTextLine("Metodo de Pago: ${order.paymentMethod}", CMD_ALIGN_RIGHT, FONT_NORMAL)

            if (order.paymentMethod == "Efectivo") {
                val recFormatted = String.format(Locale.US, "$%.2f", order.amountReceived)
                val chgFormatted = String.format(Locale.US, "$%.2f", order.changeGiven)
                addTextLine("Efectivo Recibido:  $recFormatted", CMD_ALIGN_RIGHT, FONT_NORMAL)
                addTextLine("Cambio Calculado:   $chgFormatted", CMD_ALIGN_RIGHT, FONT_BOLD)
            }
            addSeparator()

            // 5. CIERRE / PIE DE TICKET
            addTextLine("¡Gracias por su Compra y Visita!", CMD_ALIGN_CENTER, FONT_BOLD)
            if (restaurantSlogan.isNotEmpty()) {
                addTextLine(restaurantSlogan, CMD_ALIGN_CENTER, FONT_NORMAL)
            }
            if (restaurantPhone.isNotEmpty()) {
                addTextLine("Comentarios: $restaurantPhone", CMD_ALIGN_CENTER, FONT_NORMAL)
            }
        }
        
        // Feed minimal lines to clear cutter and cut
        addNewLine()
        addNewLine()
        
        // Partial paper cut ESC/POS command (GS V m or GS V 0)
        bytes.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList()) // Feed and cut

        return bytes.toByteArray()
    }

    /**
     * Generates ESC/POS bytes for a consolidated cash closure report.
     */
    fun buildEscPosClosureReport(
        restaurantName: String,
        reportTitle: String,
        dateRange: String,
        totalSales: Double,
        totalCost: Double,
        paymentsBreakdown: Map<String, Double>,
        productStats: List<com.example.ui.RestaurantViewModel.ProductStat>
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        val CMD_INIT = byteArrayOf(0x1B, 0x40)
        val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0)
        val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 1)
        
        val FONT_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
        val FONT_BOLD = byteArrayOf(0x1B, 0x21, 0x08)
        val FONT_BOLD_DOUBLE = byteArrayOf(0x1B, 0x21, 0x38)

        fun sanitizeText(text: String): String {
            return text.replace('á', 'a')
                .replace('é', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ú', 'u')
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U')
                .replace('ü', 'u')
                .replace('Ü', 'U')
                .replace("¿", "")
                .replace("¡", "")
                .replace('ñ', 'n')
                .replace('Ñ', 'N')
        }

        fun addTextLine(text: String, alignment: ByteArray = CMD_ALIGN_LEFT, font: ByteArray = FONT_NORMAL, newline: Boolean = true) {
            bytes.addAll(alignment.toList())
            bytes.addAll(font.toList())
            val sanitized = sanitizeText(text)
            bytes.addAll(sanitized.toByteArray(Charsets.US_ASCII).toList())
            if (newline) {
                bytes.addAll("\n".toByteArray(Charsets.US_ASCII).toList())
            }
        }

        fun drawDivider() {
            addTextLine("--------------------------------", CMD_ALIGN_LEFT, FONT_NORMAL)
        }

        fun drawDoubleDivider() {
            addTextLine("================================", CMD_ALIGN_LEFT, FONT_NORMAL)
        }

        bytes.addAll(CMD_INIT.toList())

        addTextLine(restaurantName.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD_DOUBLE)
        addTextLine(reportTitle.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD)
        addTextLine("Periodo: $dateRange", CMD_ALIGN_CENTER, FONT_NORMAL)
        drawDoubleDivider()

        addTextLine("RESUMEN DE METRICAS", CMD_ALIGN_CENTER, FONT_BOLD)
        drawDivider()
        
        val netProfit = totalSales - totalCost
        addTextLine("Ventas Totales: " + String.format(Locale.US, "$%.2f", totalSales), CMD_ALIGN_LEFT, FONT_BOLD)
        addTextLine("Costo Insumos:  " + String.format(Locale.US, "$%.2f", totalCost), CMD_ALIGN_LEFT, FONT_NORMAL)
        addTextLine("Utilidad Neta:  " + String.format(Locale.US, "$%.2f", netProfit), CMD_ALIGN_LEFT, FONT_BOLD)
        
        drawDivider()
        addTextLine("METODOS DE PAGO", CMD_ALIGN_CENTER, FONT_BOLD)
        drawDivider()
        paymentsBreakdown.forEach { (method, amt) ->
            val spaces = 32 - method.length - String.format(Locale.US, "$%.2f", amt).length - 2
            val pad = " ".repeat(maxOf(1, spaces))
            addTextLine("• $method:$pad" + String.format(Locale.US, "$%.2f", amt), CMD_ALIGN_LEFT, FONT_NORMAL)
        }

        if (productStats.isNotEmpty()) {
            drawDivider()
            addTextLine("PLATILLOS MAS VENDIDOS", CMD_ALIGN_CENTER, FONT_BOLD)
            drawDivider()
            productStats.take(10).forEach { stat ->
                val qtyStr = "x${stat.quantitySold}"
                val revStr = String.format(Locale.US, "$%.2f", stat.revenue)
                addTextLine(stat.name, CMD_ALIGN_LEFT, FONT_BOLD)
                addTextLine("   Cant: $qtyStr | Total: $revStr", CMD_ALIGN_LEFT, FONT_NORMAL)
            }
        }

        drawDoubleDivider()
        addTextLine("Sabor y Gestion Benitez", CMD_ALIGN_CENTER, FONT_NORMAL)
        addTextLine("Reporte emitido localmente", CMD_ALIGN_CENTER, FONT_NORMAL)
        addTextLine(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()), CMD_ALIGN_CENTER, FONT_NORMAL)
        addTextLine("\n\n\n\n")

        // Feed and cut
        bytes.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())

        return bytes.toByteArray()
    }

    /**
     * Establishes a Bluetooth socket connection and transmits standard ESC/POS bytes asynchronously.
     */
    @SuppressLint("MissingPermission")
    suspend fun printDirect(context: Context, macAddress: String, printData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission(context)) {
            Log.e(TAG, "printDirect: Sin autorización de Bluetooth")
            return@withContext false
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        var success = false

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "printDirect: Adaptador Bluetooth apagado o inactivo")
                return@withContext false
            }

            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
            
            // Standard UUID for SPP (Serial Port Profile) connect
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // Connect socket (blocking call - hence IO dispatcher run)
            socket.connect()
            
            outputStream = socket.outputStream
            outputStream.write(printData)
            outputStream.flush()
            
            // Brief sleep for socket streaming completion
            Thread.sleep(1000)
            
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la impresion Bluetooth directa", e)
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (t: Throwable) {
                // Ignore
            }
        }
        return@withContext success
    }

    /**
     * Conducts a diagnostic test page transmission to the configured printer.
     */
    suspend fun printTestPage(
        context: Context,
        macAddress: String,
        printerName: String
    ): Boolean {
        val bytes = mutableListOf<Byte>()
        val CMD_INIT = byteArrayOf(0x1B, 0x40)
        val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 1)
        val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0)
        val FONT_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
        val FONT_BOLD_DOUBLE = byteArrayOf(0x1B, 0x21, 0x38)

        bytes.addAll(CMD_INIT.toList())
        bytes.addAll(CMD_ALIGN_CENTER.toList())
        bytes.addAll(FONT_BOLD_DOUBLE.toList())
        bytes.addAll("IMPRESION DE PRUEBA\n".toByteArray(Charsets.US_ASCII).toList())
        
        bytes.addAll(CMD_ALIGN_CENTER.toList())
        bytes.addAll(FONT_NORMAL.toList())
        bytes.addAll("Sabor y Gestion Benitez\n".toByteArray(Charsets.US_ASCII).toList())
        bytes.addAll("--------------------------------\n".toByteArray(Charsets.US_ASCII).toList())
        
        bytes.addAll(CMD_ALIGN_LEFT.toList())
        bytes.addAll("Impresora: $printerName\n".toByteArray(Charsets.US_ASCII).toList())
        bytes.addAll("MAC: $macAddress\n".toByteArray(Charsets.US_ASCII).toList())
        bytes.addAll("Estado: Conexion Exitosa\n".toByteArray(Charsets.US_ASCII).toList())
        
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        bytes.addAll("Hora: ${format.format(Date())}\n".toByteArray(Charsets.US_ASCII).toList())
        bytes.addAll("--------------------------------\n".toByteArray(Charsets.US_ASCII).toList())
        bytes.addAll("¡COMPATIBILIDAD BLUETOOTH OK!\n\n\n\n".toByteArray(Charsets.US_ASCII).toList())
        
        // Cut
        bytes.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())

        return printDirect(context, macAddress, bytes.toByteArray())
    }
}
