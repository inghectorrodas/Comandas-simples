package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RestaurantRepository(private val dao: RestaurantDao) {

    // --- Dishes Menu ---
    val allDishes: Flow<List<Dish>> = dao.getAllDishes()

    suspend fun getDishById(id: Int): Dish? = dao.getDishById(id)

    suspend fun insertDish(dish: Dish): Long = dao.insertDish(dish)

    suspend fun updateDish(dish: Dish) = dao.updateDish(dish)

    suspend fun deleteDish(dish: Dish) = dao.deleteDish(dish)

    suspend fun updateDishStock(id: Int, stock: Int) = dao.resetDishStock(id, stock)

    // --- Purchasing ---
    val allPurchases: Flow<List<Purchase>> = dao.getAllPurchases()

    suspend fun recordPurchase(dishId: Int, quantity: Int, unitCost: Double): Long {
        val dish = dao.getDishById(dishId) ?: return -1
        val purchase = Purchase(
            dishId = dishId,
            dishName = dish.name,
            quantity = quantity,
            unitCost = unitCost,
            totalCost = quantity * unitCost,
            timestamp = System.currentTimeMillis()
        )
        // Record purchase
        val purchaseId = dao.insertPurchase(purchase)
        // Add stock
        dao.addDishStock(dishId, quantity)
        return purchaseId
    }

    // --- Orders ---
    val allOrders: Flow<List<Order>> = dao.getAllOrders()
    val activeOrders: Flow<List<Order>> = dao.getActiveOrders()

    suspend fun getOrderById(id: Int): Order? = dao.getOrderById(id)

    suspend fun getOrderItems(orderId: Int): List<OrderItem> = dao.getOrderItemsForOrder(orderId)

    suspend fun createOrder(order: Order, items: List<OrderItem>): Long {
        // Compute total amount and cost from snapshot if needed, or use order fields
        val orderId = dao.insertOrder(order).toInt()
        
        for (item in items) {
            val finalItem = item.copy(orderId = orderId)
            dao.insertOrderItem(finalItem)
            
            // Deduct inventory stock for the day
            dao.addDishStock(finalItem.dishId, -finalItem.quantity)
        }
        return orderId.toLong()
    }

    suspend fun modifyOrderItems(orderId: Int, newItems: List<OrderItem>) {
        val existingItems = dao.getOrderItemsForOrder(orderId)
        for (item in existingItems) {
            dao.addDishStock(item.dishId, item.quantity)
        }
        dao.deleteOrderItemsForOrder(orderId)
        
        var totalAmount = 0.0
        var totalCost = 0.0
        for (item in newItems) {
            val finalItem = item.copy(orderId = orderId)
            dao.insertOrderItem(finalItem)
            dao.addDishStock(finalItem.dishId, -finalItem.quantity)
            totalAmount += finalItem.price * finalItem.quantity
            totalCost += finalItem.cost * finalItem.quantity
        }
        dao.updateOrderTotals(orderId, totalAmount, totalCost)
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        val order = dao.getOrderById(orderId)
        if (order != null) {
            dao.updateOrder(order.copy(status = status))
        }
    }

    suspend fun deleteOrder(order: Order) {
        // Optional: restore stock if order is canceled/deleted
        val items = dao.getOrderItemsForOrder(order.id)
        for (item in items) {
            dao.addDishStock(item.dishId, item.quantity)
        }
        dao.deleteOrder(order)
    }

    // --- Cash Closures ---
    val allClosures: Flow<List<CashClosure>> = dao.getAllClosures()

    suspend fun performCashClosure(dateString: String, openTime: Long, closeTime: Long): Long {
        // Query active orders (where closureId is null) to calculate sums
        val unclosedOrders = dao.getActiveOrders().first()
        if (unclosedOrders.isEmpty()) {
            return -1 // No sales to close
        }

        val totalSales = unclosedOrders.sumOf { it.totalAmount }
        val totalCost = unclosedOrders.sumOf { it.totalCost }
        val netProfit = totalSales - totalCost
        val count = unclosedOrders.size

        val closure = CashClosure(
            dateString = dateString,
            openTimestamp = openTime,
            closeTimestamp = closeTime,
            totalSales = totalSales,
            totalCost = totalCost,
            netProfit = netProfit,
            totalOrdersCount = count
        )

        val closureId = dao.insertClosure(closure).toInt()
        
        // Tag all these unclosed orders with the new closureId
        dao.tagOpenOrdersWithClosure(closureId)
        
        return closureId.toLong()
    }

    suspend fun getOrderItemsForClosure(closureId: Int): List<OrderItem> {
        // Find all order items that belong to orders with closureId
        val allItems = dao.getAllOrderItems()
        val allOrdersUnderClosure = dao.getAllOrders().first()
            .filter { it.closureId == closureId }
            .map { it.id }
            .toSet()
        
        return allItems.filter { it.orderId in allOrdersUnderClosure }
    }

    // --- Custom Categories ---
    val allCustomCategories: Flow<List<CustomCategory>> = dao.getAllCustomCategories()

    suspend fun insertCustomCategory(category: CustomCategory) {
        dao.insertCustomCategory(category)
    }

    suspend fun deleteCustomCategory(category: CustomCategory) {
        dao.deleteCustomCategory(category)
    }
}
