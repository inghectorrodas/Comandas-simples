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
        items: List<OrderItem>
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

        // Initialize Printer
        bytes.addAll(CMD_INIT.toList())

        // Header (centered & bold double height)
        addTextLine(restaurantName.uppercase(Locale.getDefault()), CMD_ALIGN_CENTER, FONT_BOLD_DOUBLE)
        if (restaurantSlogan.isNotEmpty()) {
            addTextLine(restaurantSlogan, CMD_ALIGN_CENTER, FONT_NORMAL)
        }
        if (restaurantAddress.isNotEmpty()) {
            addTextLine("Dir: $restaurantAddress", CMD_ALIGN_CENTER, FONT_NORMAL)
        }
        if (restaurantPhone.isNotEmpty()) {
            addTextLine("Tel: $restaurantPhone", CMD_ALIGN_CENTER, FONT_NORMAL)
        }
        
        addSeparator()

        // Ticket Details
        addTextLine("TICKET DE VENTA N: #${order.id}", CMD_ALIGN_LEFT, FONT_BOLD)
        
        val formatDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateString = formatDateTime.format(Date(order.timestamp))
        addTextLine("Fecha: $dateString", CMD_ALIGN_LEFT, FONT_NORMAL)
        
        val modeStr = if (order.isDelivery) "Domi: ${order.deliveryAddress ?: ""}" else if (order.tableNumber != null) "Mesa: ${order.tableNumber}" else "Para Llevar"
        addTextLine("Servicio: $modeStr", CMD_ALIGN_LEFT, FONT_NORMAL)
        addTextLine("Cliente: ${order.customerName ?: "Mostrador"}", CMD_ALIGN_LEFT, FONT_NORMAL)
        addTextLine("Estado: ${if (order.status == "PENDING") "PND (Cocina)" else "Completado"}", CMD_ALIGN_LEFT, FONT_NORMAL)
        
        addSeparator()

        // Items Header Row
        // Column mapping: Qty (3) Name (18) Subtotal (9) -> totals 30 characters plus spacing
        addTextLine("Cant  Platillo          Subtot", CMD_ALIGN_LEFT, FONT_BOLD)
        addSeparator()

        // Items Rows
        items.forEach { item ->
            // Format line beautifully
            val qtyStr = "${item.quantity}x".padEnd(5) // (5 chars)
            val cleanName = sanitizeSpanishText(item.dishName)
            val subtotalDouble = item.price * item.quantity
            val subtotalStr = String.format(Locale.US, "$%.2f", subtotalDouble)

            // If name is too long, wrap or truncate it for 58mm layout
            val maxNameChar = 16
            val finalName = if (cleanName.length > maxNameChar) cleanName.substring(0, maxNameChar) else cleanName.padEnd(maxNameChar)
            
            val rightPaddedSubtotal = subtotalStr.padStart(9) // (9 chars)
            val line = qtyStr + finalName + rightPaddedSubtotal
            
            bytes.addAll(CMD_ALIGN_LEFT.toList())
            bytes.addAll(FONT_NORMAL.toList())
            bytes.addAll((line + "\n").toByteArray(Charsets.US_ASCII).toList())
        }

        addSeparator()

        // Totals
        val totalFormatted = String.format(Locale.US, "$%.2f", order.totalAmount)
        addTextLine("TOTAL A PAGAR:    $totalFormatted", CMD_ALIGN_RIGHT, FONT_BOLD)
        addTextLine("Metodo de Pago: ${order.paymentMethod}", CMD_ALIGN_RIGHT, FONT_NORMAL)

        if (order.paymentMethod == "Efectivo") {
            val recFormatted = String.format(Locale.US, "$%.2f", order.amountReceived)
            val chgFormatted = String.format(Locale.US, "$%.2f", order.changeGiven)
            addTextLine("Recibido:    $recFormatted", CMD_ALIGN_RIGHT, FONT_NORMAL)
            addTextLine("Cambio:      $chgFormatted", CMD_ALIGN_RIGHT, FONT_NORMAL)
        }

        addSeparator()

        // Footer
        addTextLine("GRACIAS POR SU PREFERENCIA", CMD_ALIGN_CENTER, FONT_BOLD)
        addTextLine("Sabor y Gestion - Panes con Gallina", CMD_ALIGN_CENTER, FONT_NORMAL)
        
        // Feed lines to clear cutter and cut
        addNewLine()
        addNewLine()
        addNewLine()
        addNewLine()
        
        // Partial paper cut ESC/POS command (GS V m or GS V 0)
        bytes.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList()) // Feed and cut

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
