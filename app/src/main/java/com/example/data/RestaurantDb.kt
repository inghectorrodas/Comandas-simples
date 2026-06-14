package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ENTITIES
// ==========================================

@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val cost: Double,
    val category: String,
    val dailyStock: Int,
    val initialDailyStock: Int,
    val imageBase64: String? = null,
    val minStockThreshold: Int = 5
)

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey val name: String,
    val iconBase64: String? = null
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val status: String, // "PENDING", "DELIVERY", "DELIVERED", "COMPLETED"
    val customerName: String? = null,
    val tableNumber: String? = null,
    val isDelivery: Boolean = false,
    val deliveryAddress: String? = null,
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val closureId: Int? = null, // null if part of the current open register session
    val paymentMethod: String = "Efectivo",
    val amountReceived: Double = 0.0,
    val changeGiven: Double = 0.0,
    val notes: String? = null
)

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val dishId: Int,
    val dishName: String,
    val quantity: Int,
    val price: Double,
    val cost: Double
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dishId: Int,
    val dishName: String,
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double,
    val timestamp: Long
)

@Entity(tableName = "cash_closures")
data class CashClosure(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val openTimestamp: Long,
    val closeTimestamp: Long,
    val totalSales: Double,
    val totalCost: Double,
    val netProfit: Double,
    val totalOrdersCount: Int,
    val initialCash: Double = 0.0,
    val actualCashAtClose: Double = 0.0,
    val expectedCashAtClose: Double = 0.0,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val transferSales: Double = 0.0
)

// ==========================================
// DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface RestaurantDao {

    // --- Dishes Menu ---
    @Query("SELECT * FROM dishes ORDER BY name ASC")
    fun getAllDishes(): Flow<List<Dish>>

    @Query("SELECT * FROM dishes WHERE id = :id LIMIT 1")
    suspend fun getDishById(id: Int): Dish?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDish(dish: Dish): Long

    @Update
    suspend fun updateDish(dish: Dish)

    @Delete
    suspend fun deleteDish(dish: Dish)

    @Query("UPDATE dishes SET dailyStock = dailyStock + :quantity WHERE id = :id")
    suspend fun addDishStock(id: Int, quantity: Int)

    @Query("UPDATE dishes SET dailyStock = :stock, initialDailyStock = :stock WHERE id = :id")
    suspend fun resetDishStock(id: Int, stock: Int)

    // --- Custom Categories ---
    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllCustomCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCustomCategory(category: CustomCategory)

    // --- Orders ---
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE closureId IS NULL ORDER BY timestamp DESC")
    fun getActiveOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun deleteOrder(order: Order)

    @Query("UPDATE orders SET closureId = :closureId WHERE closureId IS NULL")
    suspend fun tagOpenOrdersWithClosure(closureId: Int)

    // --- Order Items ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItem)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsForOrder(orderId: Int): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItemsForOrder(orderId: Int)

    @Query("UPDATE orders SET totalAmount = :totalAmount, totalCost = :totalCost WHERE id = :orderId")
    suspend fun updateOrderTotals(orderId: Int, totalAmount: Double, totalCost: Double)

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItems(): List<OrderItem>

    // --- Purchases ---
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    // --- Cash Closures ---
    @Query("SELECT * FROM cash_closures ORDER BY closeTimestamp DESC")
    fun getAllClosures(): Flow<List<CashClosure>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosure(closure: CashClosure): Long
}

// ==========================================
// DATABASE HOLDER
// ==========================================

@Database(
    entities = [Dish::class, Order::class, OrderItem::class, Purchase::class, CashClosure::class, CustomCategory::class],
    version = 6,
    exportSchema = false
)
abstract class RestaurantDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao
}
