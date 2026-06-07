package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize Room Database and Repository
    private val db = Room.databaseBuilder(
        application,
        RestaurantDatabase::class.java,
        "restaurant_management_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    private val repository = RestaurantRepository(db.restaurantDao())

    // --- Shift & Shift Cash management ---
    private val shiftPrefs = application.getSharedPreferences("restaurant_shift_prefs", Context.MODE_PRIVATE)

    private val _isShiftActive = MutableStateFlow(shiftPrefs.getBoolean("is_shift_active", false))
    val isShiftActive: StateFlow<Boolean> = _isShiftActive.asStateFlow()

    private val _initialCash = MutableStateFlow(shiftPrefs.getFloat("initial_cash", 100f).toDouble())
    val initialCash: StateFlow<Double> = _initialCash.asStateFlow()

    private val _shiftStartTimestamp = MutableStateFlow(shiftPrefs.getLong("shift_start_timestamp", 0L))
    val shiftStartTimestamp: StateFlow<Long> = _shiftStartTimestamp.asStateFlow()

    fun startShift(startCash: Double, startStocks: Map<Int, Int>) {
        viewModelScope.launch {
            // Apply starting stocks
            startStocks.forEach { (dishId, stock) ->
                repository.updateDishStock(dishId, stock)
            }
            
            // Set shift preferences
            shiftPrefs.edit().apply {
                putBoolean("is_shift_active", true)
                putFloat("initial_cash", startCash.toFloat())
                putLong("shift_start_timestamp", System.currentTimeMillis())
                apply()
            }
            _isShiftActive.value = true
            _initialCash.value = startCash
            _shiftStartTimestamp.value = System.currentTimeMillis()
        }
    }

    fun endShift() {
        shiftPrefs.edit().apply {
            putBoolean("is_shift_active", false)
            putFloat("initial_cash", 0f)
            putLong("shift_start_timestamp", 0L)
            apply()
        }
        _isShiftActive.value = false
        _initialCash.value = 0.0
        _shiftStartTimestamp.value = 0L
    }

    // --- State Expositions ---
    val dishes: StateFlow<List<Dish>> = repository.allDishes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val purchases: StateFlow<List<Purchase>> = repository.allPurchases.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeOrders: StateFlow<List<Order>> = repository.activeOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOrders: StateFlow<List<Order>> = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val closures: StateFlow<List<CashClosure>> = repository.allClosures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customCategories: StateFlow<List<CustomCategory>> = repository.allCustomCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val prefs = application.getSharedPreferences("restaurant_prefs", android.content.Context.MODE_PRIVATE)

    // --- Active Sale / Shopping Cart State ---
    private val _cart = MutableStateFlow<Map<Int, CartItem>>(emptyMap())
    val cart: StateFlow<Map<Int, CartItem>> = _cart.asStateFlow()

    data class CartItem(val dish: Dish, val quantity: Int)

    // Checkout configurations
    val customerName = MutableStateFlow("")
    val tableNumber = MutableStateFlow("")
    val isDelivery = MutableStateFlow(false)
    val deliveryAddress = MutableStateFlow("")
    val checkoutOrderType = MutableStateFlow("COMER_AQUI") // "COMER_AQUI", "LLEVAR", "DOMICILIO"
    val checkoutPaymentMethod = MutableStateFlow("Efectivo") // "Efectivo", "Tarjeta", "Transferencia"
    val checkoutAmountReceived = MutableStateFlow(0.0)
    val checkoutChangeGiven = MutableStateFlow(0.0)

    // Last completed order (for printing)
    private val _lastCompletedOrder = MutableStateFlow<Order?>(null)
    val lastCompletedOrder: StateFlow<Order?> = _lastCompletedOrder.asStateFlow()

    private val _lastCompletedOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val lastCompletedOrderItems: StateFlow<List<OrderItem>> = _lastCompletedOrderItems.asStateFlow()

    // --- Receipt Settings State ---
    val restaurantName = MutableStateFlow(prefs.getString("restaurant_name", "Sabor y Gestión") ?: "Sabor y Gestión")
    val restaurantAddress = MutableStateFlow(prefs.getString("restaurant_address", "Av. Principal 123, Zona Gourmet") ?: "Av. Principal 123, Zona Gourmet")
    val restaurantPhone = MutableStateFlow(prefs.getString("restaurant_phone", "+1 234 567 890") ?: "+1 234 567 890")
    val logoType = MutableStateFlow(prefs.getString("logo_type", "chef_hat") ?: "chef_hat")
    val restaurantLogoBase64 = MutableStateFlow(prefs.getString("restaurant_logo_base64", "") ?: "") // Custom PNG logo Base64
    val restaurantSlogan = MutableStateFlow(prefs.getString("restaurant_slogan", "Cocinando con pasión todos los días.") ?: "Cocinando con pasión todos los días.")

    fun saveRestaurantSlogan(slogan: String) {
        prefs.edit().putString("restaurant_slogan", slogan).apply()
        restaurantSlogan.value = slogan
    }

    fun saveRestaurantSettings(name: String, address: String, phone: String, logo: String, logoBase64: String) {
        prefs.edit().apply {
            putString("restaurant_name", name)
            putString("restaurant_address", address)
            putString("restaurant_phone", phone)
            putString("logo_type", logo)
            putString("restaurant_logo_base64", logoBase64)
            apply()
        }
        restaurantName.value = name
        restaurantAddress.value = address
        restaurantPhone.value = phone
        logoType.value = logo
        restaurantLogoBase64.value = logoBase64
    }

    // --- Historical Closure Selection ---
    private val _selectedClosure = MutableStateFlow<CashClosure?>(null)
    val selectedClosure: StateFlow<CashClosure?> = _selectedClosure.asStateFlow()

    private val _selectedClosureItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val selectedClosureItems: StateFlow<List<OrderItem>> = _selectedClosureItems.asStateFlow()

    init {
        // Populate default menu items if database is totally empty
        viewModelScope.launch {
            dishes.first() // Wait to load
            if (dishes.value.isEmpty()) {
                val defaults = listOf(
                    Dish(name = "Hamburguesa Clásica", price = 12.50, cost = 5.00, category = "Hamburguesas", dailyStock = 30, initialDailyStock = 30),
                    Dish(name = "Pizza Familiar Pepperoni", price = 18.00, cost = 7.50, category = "Pizzas", dailyStock = 15, initialDailyStock = 15),
                    Dish(name = "Tacos al Pastor (Orden x5)", price = 10.00, cost = 4.00, category = "Tacos", dailyStock = 50, initialDailyStock = 50),
                    Dish(name = "Café Americano", price = 3.50, cost = 1.00, category = "Bebidas", dailyStock = 80, initialDailyStock = 80),
                    Dish(name = "Pastel de Tres Leches", price = 4.50, cost = 1.80, category = "Postres", dailyStock = 20, initialDailyStock = 20),
                    Dish(name = "Limonada Natural", price = 3.00, cost = 0.80, category = "Bebidas", dailyStock = 60, initialDailyStock = 60)
                )
                for (d in defaults) {
                    repository.insertDish(d)
                }
            }
        }
    }

    // --- Product Management (ABM) ---
    fun addCustomCategory(name: String, iconBase64: String? = null) {
        viewModelScope.launch {
            repository.insertCustomCategory(CustomCategory(name = name, iconBase64 = iconBase64))
        }
    }

    fun deleteCustomCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(CustomCategory(name = name))
        }
    }

    fun addDish(name: String, price: Double, cost: Double, category: String, initialStock: Int, imageBase64: String? = null, minStockThreshold: Int = 5) {
        viewModelScope.launch {
            val dish = Dish(
                name = name,
                price = price,
                cost = cost,
                category = category,
                dailyStock = initialStock,
                initialDailyStock = initialStock,
                imageBase64 = imageBase64,
                minStockThreshold = minStockThreshold
            )
            repository.insertDish(dish)
        }
    }

    fun updateDish(dish: Dish) {
        viewModelScope.launch {
            repository.updateDish(dish)
        }
    }

    fun deleteDish(dish: Dish) {
        viewModelScope.launch {
            repository.deleteDish(dish)
        }
    }

    // --- Stock Resupplying (Compra) ---
    fun buyDishStock(dishId: Int, quantity: Int, unitCost: Double) {
        viewModelScope.launch {
            repository.recordPurchase(dishId, quantity, unitCost)
        }
    }

    // --- Inventory Adjustment (Edición de Stock Diario) ---
    fun setDailyStock(dishId: Int, newStock: Int) {
        viewModelScope.launch {
            repository.updateDishStock(dishId, newStock)
        }
    }

    // --- Shopping Cart Operations ---
    fun addToCart(dish: Dish) {
        val current = _cart.value.toMutableMap()
        val existing = current[dish.id]
        if (existing != null) {
            current[dish.id] = CartItem(dish, existing.quantity + 1)
        } else {
            current[dish.id] = CartItem(dish, 1)
        }
        _cart.value = current
    }

    fun removeFromCart(dish: Dish) {
        val current = _cart.value.toMutableMap()
        val existing = current[dish.id] ?: return
        if (existing.quantity > 1) {
            current[dish.id] = CartItem(dish, existing.quantity - 1)
        } else {
            current.remove(dish.id)
        }
        _cart.value = current
    }

    fun deleteProductFromCart(dish: Dish) {
        val current = _cart.value.toMutableMap()
        current.remove(dish.id)
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
        customerName.value = ""
        tableNumber.value = ""
        isDelivery.value = false
        deliveryAddress.value = ""
        checkoutOrderType.value = "COMER_AQUI"
        checkoutPaymentMethod.value = "Efectivo"
        checkoutAmountReceived.value = 0.0
        checkoutChangeGiven.value = 0.0
    }

    // --- Check Out (Venta) ---
    fun checkoutCart(customStatus: String? = null, onSuccess: (Order) -> Unit) {
        viewModelScope.launch {
            val itemsInCart = _cart.value.values.toList()
            if (itemsInCart.isEmpty()) return@launch

            val totalAmount = itemsInCart.sumOf { it.dish.price * it.quantity }
            val totalCost = itemsInCart.sumOf { it.dish.cost * it.quantity }

            val orderType = checkoutOrderType.value
            val isDel = orderType == "DOMICILIO"
            val tabNum = when (orderType) {
                "COMER_AQUI" -> tableNumber.value.trim().ifBlank { "Mesa Gral" }
                "LLEVAR" -> "Para Llevar"
                else -> null
            }
            val delAddress = if (orderType == "DOMICILIO") deliveryAddress.value.trim().ifBlank { "Domicilio" } else null

            // Create initial Order status:
            // "PENDING" for Eat-In (Comer Aquí), "DELIVERY" for active Delivery (Domicilio), "COMPLETED" for To-Go (Llevar, immediate sale)
            val status = customStatus ?: when (orderType) {
                "COMER_AQUI" -> "PENDING"
                "DOMICILIO" -> "DELIVERY"
                else -> "COMPLETED"
            }

            val order = Order(
                timestamp = System.currentTimeMillis(),
                status = status,
                customerName = customerName.value.trim().ifBlank { "Cliente Mostrador" },
                tableNumber = tabNum,
                isDelivery = isDel,
                deliveryAddress = delAddress,
                totalAmount = totalAmount,
                totalCost = totalCost,
                paymentMethod = checkoutPaymentMethod.value,
                amountReceived = checkoutAmountReceived.value,
                changeGiven = checkoutChangeGiven.value
            )

            val orderItems = itemsInCart.map {
                OrderItem(
                    orderId = 0, // setup during insertion
                    dishId = it.dish.id,
                    dishName = it.dish.name,
                    quantity = it.quantity,
                    price = it.dish.price,
                    cost = it.dish.cost
                )
            }

            val orderId = repository.createOrder(order, orderItems)
            val completedOrder = order.copy(id = orderId.toInt())

            _lastCompletedOrder.value = completedOrder
            _lastCompletedOrderItems.value = orderItems.map { it.copy(orderId = orderId.toInt()) }

            clearCart()
            onSuccess(completedOrder)
        }
    }

    // Get order items for preview
    fun loadOrderItems(orderId: Int, onLoaded: (List<OrderItem>) -> Unit) {
        viewModelScope.launch {
            val items = repository.getOrderItems(orderId)
            onLoaded(items)
        }
    }

    // --- Comanda Delivery/Kitchen Status Transition ---
    fun advanceOrderStatus(order: Order) {
        viewModelScope.launch {
            val newStatus = when (order.status) {
                "PENDING" -> "COMPLETED" // Prepared -> Done
                "DELIVERY" -> "DELIVERED" // Out on dispatch -> Delivered (Completed)
                "DELIVERED" -> "COMPLETED"
                else -> "COMPLETED"
            }
            repository.updateOrderStatus(order.id, newStatus)
        }
    }

    fun modifyOrder(orderId: Int, newItems: List<OrderItem>, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.modifyOrderItems(orderId, newItems)
            onSaved?.invoke()
        }
    }

    fun startSalesDay(stocksMap: Map<Int, Int>) {
        viewModelScope.launch {
            stocksMap.forEach { (dishId, stock) ->
                repository.updateDishStock(dishId, stock)
            }
        }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    // --- Cash Closures ---
    fun closeCashRegister(actualCashAtClose: Double, onSuccess: (CashClosure) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Find open orders
            val unclosedSales = activeOrders.value
            if (unclosedSales.isEmpty()) {
                onError("No hay ventas registradas en el turno actual para cerrar.")
                return@launch
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val openTime = _shiftStartTimestamp.value.takeIf { it > 0 } ?: (unclosedSales.mapNotNull { it.timestamp }.minOrNull() ?: System.currentTimeMillis())
            val closeTime = System.currentTimeMillis()

            val closureId = repository.performCashClosure(
                dateString = dateStr, 
                openTime = openTime, 
                closeTime = closeTime,
                initialCash = _initialCash.value,
                actualCashAtClose = actualCashAtClose
            )
            if (closureId > 0) {
                val allClosuresList = repository.allClosures.first()
                val newlyCreated = allClosuresList.firstOrNull { it.id == closureId.toInt() }
                if (newlyCreated != null) {
                    endShift()
                    onSuccess(newlyCreated)
                } else {
                    onError("Error de sincronización de cierre.")
                }
            } else {
                onError("No se pudo procesar el cierre. Intente de nuevo.")
            }
        }
    }

    fun selectClosure(closure: CashClosure?) {
        _selectedClosure.value = closure
        if (closure != null) {
            viewModelScope.launch {
                val items = repository.getOrderItemsForClosure(closure.id)
                _selectedClosureItems.value = items
            }
        } else {
            _selectedClosureItems.value = emptyList()
        }
    }

    // --- Get Detailed Statistics for Product Sales ---
    data class ProductStat(
        val name: String,
        val quantitySold: Int,
        val revenue: Double,
        val cost: Double,
        val profit: Double
    )

    fun getActiveSessionStats(): List<ProductStat> {
        val activeOrdersList = activeOrders.value
        // We need to fetch items for active orders
        // Since compiling standard flow, let's load synchronously if we cache or fetch items
        // Let's implement an elegant in-memory calculation or fallback
        // To make it instant in UI without blocking, we can compute it from our orders & pre-loaded details
        // Instead of waiting, we will fetch order items when active orders update if needed.
        // Let's write an async or flow-based statistics calculator:
        return emptyList()
    }

    // Better support: load active order items reactively
    private val _activeOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val activeOrderItems: StateFlow<List<OrderItem>> = _activeOrderItems.asStateFlow()

    init {
        // Collect active orders to keep active items up-to-date
        viewModelScope.launch {
            activeOrders.collect { ordersList ->
                val itemsAccumulator = mutableListOf<OrderItem>()
                for (order in ordersList) {
                    val items = repository.getOrderItems(order.id)
                    itemsAccumulator.addAll(items)
                }
                _activeOrderItems.value = itemsAccumulator
            }
        }
    }

    fun computeStats(items: List<OrderItem>): List<ProductStat> {
        return items.groupBy { it.dishId }
            .map { (dishId, itemsForDish) ->
                val name = itemsForDish.firstOrNull()?.dishName ?: "Producto Desc."
                val qty = itemsForDish.sumOf { it.quantity }
                val rev = itemsForDish.sumOf { it.price * it.quantity }
                val cost = itemsForDish.sumOf { it.cost * it.quantity }
                ProductStat(
                    name = name,
                    quantitySold = qty,
                    revenue = rev,
                    cost = cost,
                    profit = rev - cost
                )
            }
            .sortedByDescending { it.quantitySold }
    }

    fun getStatsForClosures(closuresList: List<CashClosure>, onComplete: (List<ProductStat>) -> Unit) {
        viewModelScope.launch {
            val combinedItems = mutableListOf<OrderItem>()
            for (closure in closuresList) {
                val items = repository.getOrderItemsForClosure(closure.id)
                combinedItems.addAll(items)
            }
            onComplete(computeStats(combinedItems))
        }
    }
}
