package com.retail.dolphinpos.presentation.features.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import com.retail.dolphinpos.data.repository.HoldCartRepository
import com.retail.dolphinpos.data.repositories.order.PendingOrderRepository
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderRequest
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.cart.DiscountType
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountedPrice
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountAmount
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
import com.retail.dolphinpos.domain.model.home.customer.Customer
import com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val preferenceManager: PreferenceManager,
    private val holdCartRepository: HoldCartRepository,
    private val pendingOrderRepository: PendingOrderRepository,
    private val networkMonitor: NetworkMonitor,
    private val storeRegistersRepository: StoreRegistersRepository
) : ViewModel() {

    var isCashSelected: Boolean = false

    private val _homeUiEvent = MutableSharedFlow<HomeUiEvent>()
    val homeUiEvent: SharedFlow<HomeUiEvent> = _homeUiEvent.asSharedFlow()

    private val _products = MutableStateFlow<List<Products>>(emptyList())
    val products: StateFlow<List<Products>> = _products.asStateFlow()

    private val _searchProductResults = MutableStateFlow<List<Products>>(emptyList())
    val searchProductResults: StateFlow<List<Products>> = _searchProductResults.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cashDiscountTotal = MutableStateFlow(0.0)
    val cashDiscountTotal: StateFlow<Double> = _cashDiscountTotal.asStateFlow()

    private val _orderLevelDiscounts = MutableStateFlow<List<OrderDiscount>>(emptyList())
    val orderLevelDiscounts: StateFlow<List<OrderDiscount>> = _orderLevelDiscounts.asStateFlow()

    private val _orderDiscountTotal = MutableStateFlow(0.0)
    val orderDiscountTotal: StateFlow<Double> = _orderDiscountTotal.asStateFlow()

    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    private val _tax = MutableStateFlow(0.0)
    val tax: StateFlow<Double> = _tax.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _categories =
        MutableStateFlow<List<com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData>>(
            emptyList()
        )
    val categories: StateFlow<List<com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData>> =
        _categories.asStateFlow()

    private val _menus = MutableStateFlow<List<BottomMenu>>(emptyList())
    val menus: StateFlow<List<BottomMenu>> = _menus.asStateFlow()

    // Hold Cart related state
    private val _holdCarts = MutableStateFlow<List<HoldCartEntity>>(emptyList())
    val holdCarts: StateFlow<List<HoldCartEntity>> = _holdCarts.asStateFlow()

    private val _holdCartCount = MutableStateFlow(0)
    val holdCartCount: StateFlow<Int> = _holdCartCount.asStateFlow()

    init {
        loadCategories()
        loadMenus()
        resetOrderDiscountValues()  // Reset order discount values on initialization
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowLoading)
            try {
                val response = homeRepository.getCategories()
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                if (response.isNotEmpty()) {
                    _categories.value = response
                    _homeUiEvent.emit(HomeUiEvent.PopulateCategoryList(response))
                } else {
                    _homeUiEvent.emit(HomeUiEvent.ShowError("No Category Found"))
                }
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                _homeUiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Something went wrong"))
            }
        }
    }

    private fun loadMenus() {
        _menus.value = listOf(
            BottomMenu(
                menuName = "Home", destinationId = R.id.homeScreen
            ), BottomMenu(
                menuName = "Products", destinationId = R.id.productsScreen
            ), BottomMenu(
                menuName = "Orders", destinationId = R.id.ordersScreen
            ), BottomMenu(
                menuName = "Inventory", destinationId = R.id.inventoryScreen
            ), BottomMenu(
                menuName = "Reports", destinationId = R.id.reportsScreen
            ), BottomMenu(
                menuName = "Setup", destinationId = R.id.setupScreen
            )
        )
    }

    fun loadProducts(categoryId: Int) {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowLoading)
            try {
                val response = homeRepository.getProductsByCategoryID(categoryId)
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                if (response.isNotEmpty()) {
                    _products.value = response
                } else {
                    _homeUiEvent.emit(HomeUiEvent.ShowError("No Products Found"))
                }
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                _homeUiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Something went wrong"))
            }
        }
    }

    fun setProducts(list: List<Products>) {
        _products.value = list
    }

    fun getName(): String {
        return preferenceManager.getName()
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            if (query.isNotEmpty()) {
                _searchProductResults.value = homeRepository.searchProducts(query)
            } else {
                _searchProductResults.value = emptyList()
            }
        }
    }

    fun addToCart(product: Products): Boolean {
        if (hasCashDiscountApplied()) {
            return false  // Cannot add products after cash discount is applied
        }

        val cartItemList = _cartItems.value.toMutableList()
        val existingProduct = cartItemList.indexOfFirst { it.productId == product.id }
        if (existingProduct >= 0) {
            // Increment quantity if product already in cart
            val updatedQuantity = cartItemList[existingProduct].copy(
                quantity = cartItemList[existingProduct].quantity + 1
            )
            cartItemList[existingProduct] = updatedQuantity
        } else {
            // Add new product
            cartItemList.add(
                CartItem(
                    productId = product.id,
                    name = product.name,
                    quantity = 1,
                    imageUrl = product.images?.firstOrNull()?.fileURL,
                    productVariantId = null,
                    cardPrice = product.cardPrice.toDouble(),
                    cashPrice = product.cashPrice.toDouble(),
                    chargeTaxOnThisProduct = product.chargeTaxOnThisProduct,
                    selectedPrice = product.cardPrice.toDouble()  // Always add new products at card price
                )
            )
        }
        _cartItems.value = cartItemList
        calculateSubtotal(cartItemList)
        return true
    }

    fun addVariantToCart(product: Products, variant: Variant): Boolean {
        if (hasCashDiscountApplied()) {
            return false  // Cannot add products after cash discount is applied
        }

        val cartItemList = _cartItems.value.toMutableList()
        val existingVariant = cartItemList.indexOfFirst { it.productVariantId == variant.id }

        if (existingVariant >= 0) {
            // Increment quantity if variant already in cart
            val updatedQuantity = cartItemList[existingVariant].copy(
                quantity = cartItemList[existingVariant].quantity + 1
            )
            cartItemList[existingVariant] = updatedQuantity
        } else {
            // Add new variant
            val variantCardPrice =
                variant.cardPrice?.toDoubleOrNull() ?: product.cardPrice.toDouble()
            val variantCashPrice =
                variant.cashPrice?.toDoubleOrNull() ?: product.cashPrice.toDouble()

            cartItemList.add(
                CartItem(
                    productId = product.id,
                    name = "${product.name} - ${variant.title}",
                    quantity = 1,
                    imageUrl = variant.images.firstOrNull()?.fileURL
                        ?: product.images?.firstOrNull()?.fileURL,
                    productVariantId = variant.id,
                    cardPrice = variantCardPrice,
                    cashPrice = variantCashPrice,
                    chargeTaxOnThisProduct = product.chargeTaxOnThisProduct,
                    selectedPrice = variantCardPrice  // Always add new products at card price
                )
            )
        }
        _cartItems.value = cartItemList
        calculateSubtotal(cartItemList)
        return true
    }

    fun updateCartPrices() {
        val updatedCart = _cartItems.value.map { cart ->
            val newSelectedPrice = if (isCashSelected) cart.cashPrice else cart.cardPrice
            cart.copy(selectedPrice = newSelectedPrice)
        }
        _cartItems.value = updatedCart
        calculateCashDiscount(updatedCart)
        calculateSubtotal(updatedCart)
    }

    fun removeFromCart(productId: Int): Boolean {
        if (!canRemoveItemFromCart()) {
            return false  // Cannot remove item after cash discount applied
        }

        val updatedCart = _cartItems.value.filter { it.productId != productId }
        _cartItems.value = updatedCart
        if (updatedCart.isEmpty()) {
            isCashSelected = false  // Set default to card when cart becomes empty
            resetOrderDiscountValues()  // Reset order discount values when cart becomes empty
        }
        calculateSubtotal(updatedCart)
        return true
    }

    fun updateCartItem(updatedItem: CartItem) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == updatedItem.productId }
        if (index >= 0) {
            if (updatedItem.quantity <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = updatedItem
            }
        }
        _cartItems.value = currentList
        if (currentList.isEmpty()) {
            isCashSelected = false  // Set default to card when cart becomes empty
            resetOrderDiscountValues()  // Reset order discount values when cart becomes empty
        }
        calculateSubtotal(currentList)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        isCashSelected = false  // Set default to card when cart is cleared
        resetOrderDiscountValues()  // Reset order discount values when cart is cleared
        calculateSubtotal(emptyList())
    }

    private fun calculateCashDiscount(cartItems: List<CartItem>) {
        if (isCashSelected) {
            // Calculate cash discount as the difference between card-based subtotal and cash-based subtotal
            val cardBasedSubtotal = cartItems.sumOf { cartItem ->
                val cardPrice = cartItem.cardPrice
                val discountedCardPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        cardPrice - ((cardPrice * (cartItem.discountValue ?: 0.0)) / 100.0)
                    }

                    DiscountType.AMOUNT -> {
                        cardPrice - (cartItem.discountValue ?: 0.0)
                    }

                    else -> cardPrice
                }
                discountedCardPrice * cartItem.quantity
            }

            val cashBasedSubtotal = cartItems.sumOf { cartItem ->
                val cashPrice = cartItem.cashPrice
                val discountedCashPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        cashPrice - ((cashPrice * (cartItem.discountValue ?: 0.0)) / 100.0)
                    }

                    DiscountType.AMOUNT -> {
                        cashPrice - (cartItem.discountValue ?: 0.0)
                    }

                    else -> cashPrice
                }
                discountedCashPrice * cartItem.quantity
            }

            _cashDiscountTotal.value = cardBasedSubtotal - cashBasedSubtotal
        } else {
            _cashDiscountTotal.value = 0.0
        }
    }

    fun setOrderLevelDiscounts(discounts: List<OrderDiscount>) {
        _orderLevelDiscounts.value = discounts
    }

    fun saveOrderDiscountValues(
        discountValue: String,
        discountType: String,
        discountReason: String
    ) {
        preferenceManager.setOrderDiscountValue(discountValue)
        preferenceManager.setOrderDiscountType(discountType)
        preferenceManager.setOrderDiscountReason(discountReason)
    }

    fun getOrderDiscountValue(): String {
        return preferenceManager.getOrderDiscountValue()
    }

    fun getOrderDiscountType(): String {
        return preferenceManager.getOrderDiscountType()
    }

    fun getOrderDiscountReason(): String {
        return preferenceManager.getOrderDiscountReason()
    }

    fun resetOrderDiscountValues() {
        preferenceManager.clearOrderDiscountValues()
    }

    fun removeAllOrderDiscounts() {
        _orderLevelDiscounts.value = emptyList()
        calculateSubtotal(_cartItems.value)
    }

    private fun calculateSubtotal(cartItems: List<CartItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1️⃣ Base subtotal (card prices minus product-level discounts only)
            val baseSubtotal = cartItems.sumOf { it.cardPrice * it.quantity }

            // Calculate product-level discounts using card prices (subtotal always shows card prices)
            val productDiscountedSubtotal = cartItems.sumOf { cartItem ->
                val cardPrice = cartItem.cardPrice
                val discountedPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        cardPrice - ((cardPrice * (cartItem.discountValue ?: 0.0)) / 100.0)
                    }

                    DiscountType.AMOUNT -> {
                        cardPrice - (cartItem.discountValue ?: 0.0)
                    }

                    else -> cardPrice
                }
                discountedPrice * cartItem.quantity
            }
            val subtotal =
                productDiscountedSubtotal  // Subtotal shows card prices minus product discounts

            // 3️⃣ Apply all order-level discounts sequentially
            var discountedSubtotal = productDiscountedSubtotal
            var totalOrderDiscount = 0.0

            for (discount in _orderLevelDiscounts.value) {
                val beforeDiscount = discountedSubtotal
                discountedSubtotal = when (discount.type) {
                    DiscountType.PERCENTAGE -> {
                        // ✅ apply percentage on current subtotal
                        discountedSubtotal - (discountedSubtotal * discount.value / 100.0)
                    }

                    DiscountType.AMOUNT -> {
                        discountedSubtotal - discount.value
                    }
                }
                totalOrderDiscount += (beforeDiscount - discountedSubtotal)
            }

            // 4️⃣ Apply cash discount (after order-level discounts)
            val finalSubtotal = discountedSubtotal - _cashDiscountTotal.value

            // 5️⃣ Tax (only for taxable products, proportional to discount)
            val taxableBase = cartItems.sumOf { cart ->
                if (cart.chargeTaxOnThisProduct!!) cart.getProductDiscountedPrice() * cart.quantity else 0.0
            }

            val taxableAfterOrderDiscounts = if (productDiscountedSubtotal > 0) {
                taxableBase * (finalSubtotal / productDiscountedSubtotal)
            } else 0.0

            val taxValue = taxableAfterOrderDiscounts * 10 / 100.0

            // 6️⃣ Final total
            val totalAmount = finalSubtotal + taxValue

            withContext(Dispatchers.Main) {
                _subtotal.value =
                    subtotal   // subtotal shows card prices minus product-level discounts only
                _orderDiscountTotal.value = totalOrderDiscount
                _tax.value = taxValue
                _totalAmount.value = totalAmount

                // Recalculate cash discount when cart changes
                calculateCashDiscount(cartItems)
            }
        }
    }

    fun saveCustomer(firstName: String, lastName: String, email: String, birthday: String) {
        viewModelScope.launch {
            val customer = Customer(
                userId = preferenceManager.getUserID(),
                storeId = preferenceManager.getStoreID(),
                locationId = preferenceManager.getOccupiedLocationID(),
                firstName = firstName,
                lastName = lastName,
                email = email,
                birthday = birthday
            )
            val customerId = homeRepository.insertCustomerDetailsIntoLocalDB(customer)
            // Save customer ID to preferences for later use in orders
            preferenceManager.setCustomerID(customerId.toInt())
            _homeUiEvent.emit(HomeUiEvent.HoldCartSuccess("Customer Added: $firstName $lastName"))
        }
    }

    fun appendDigitToAmount(currentAmount: Double, digit: String): Double {
        val currentCents = (BigDecimal.valueOf(currentAmount) * BigDecimal(100)).toBigInteger()
        val updatedCents = (currentCents.toString() + digit).toBigIntegerOrNull() ?: currentCents
        return BigDecimal(updatedCents).divide(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    // Validation functions for discount scenarios
    fun canApplyProductDiscount(): Boolean {
        return !isCashSelected || _cashDiscountTotal.value <= 0
    }

    fun canApplyOrderLevelDiscount(): Boolean {
        return !isCashSelected || _cashDiscountTotal.value <= 0
    }

    fun canRemoveItemFromCart(): Boolean {
        return !isCashSelected || _cashDiscountTotal.value <= 0
    }

    fun hasCashDiscountApplied(): Boolean {
        return isCashSelected && _cashDiscountTotal.value > 0
    }

    fun removeLastDigit(amount: Double): Double {
        val cents = (amount * 100).toLong().toString()
        val newCents = if (cents.length > 1) cents.dropLast(1).toLong() else 0L
        val result = newCents / 100.0
        // Ensure the result doesn't go below 0.00
        return maxOf(result, 0.0)
    }

    fun formatAmount(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }

    // Hold Cart functionality
    fun loadHoldCarts() {
        viewModelScope.launch {
            try {
                val userId = preferenceManager.getUserID()
                val storeId = preferenceManager.getStoreID()
                val registerId = preferenceManager.getOccupiedRegisterID()

                val holdCartsList = holdCartRepository.getHoldCarts(userId, storeId, registerId)
                _holdCarts.value = holdCartsList
                _holdCartCount.value = holdCartsList.size
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to load hold carts: ${e.message}"))
            }
        }
    }

    fun saveHoldCart(cartName: String) {
        viewModelScope.launch {
            try {
                if (_cartItems.value.isEmpty()) {
                    _homeUiEvent.emit(HomeUiEvent.ShowError("Cart is empty. Cannot save hold cart."))
                    return@launch
                }

                val userId = preferenceManager.getUserID()
                val storeId = preferenceManager.getStoreID()
                val registerId = preferenceManager.getOccupiedRegisterID()

                val holdCartId = holdCartRepository.saveHoldCart(
                    cartName = cartName,
                    cartItems = _cartItems.value,
                    subtotal = _subtotal.value,
                    tax = _tax.value,
                    totalAmount = _totalAmount.value,
                    cashDiscountTotal = _cashDiscountTotal.value,
                    orderDiscountTotal = _orderDiscountTotal.value,
                    isCashSelected = isCashSelected,
                    userId = userId,
                    storeId = storeId,
                    registerId = registerId
                )

                // Clear current cart after saving
                clearCart()

                // Reload hold carts to update count
                loadHoldCarts()

                _homeUiEvent.emit(HomeUiEvent.HoldCartSuccess("Cart saved successfully!"))
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to save hold cart: ${e.message}"))
            }
        }
    }

    fun restoreHoldCart(holdCartId: Long) {
        viewModelScope.launch {
            try {
                val holdCart = holdCartRepository.getHoldCartById(holdCartId)
                if (holdCart != null) {
                    val cartItems = holdCartRepository.parseCartItemsFromJson(holdCart.cartItems)

                    // Clear current cart first (discard any existing items)
                    _cartItems.value = emptyList()

                    // Set cart items from hold cart
                    _cartItems.value = cartItems

                    // Set pricing and discount states
                    isCashSelected = holdCart.isCashSelected
                    _subtotal.value = holdCart.subtotal
                    _tax.value = holdCart.tax
                    _totalAmount.value = holdCart.totalAmount
                    _cashDiscountTotal.value = holdCart.cashDiscountTotal
                    _orderDiscountTotal.value = holdCart.orderDiscountTotal

                    // Recalculate subtotal to ensure consistency
                    calculateSubtotal(cartItems)

                    // Delete the hold cart after successful restoration
                    holdCartRepository.deleteHoldCart(holdCartId)

                    // Reload hold carts to update count
                    loadHoldCarts()

                    // Don't show success message for restore
                } else {
                    _homeUiEvent.emit(HomeUiEvent.ShowError("Hold cart not found."))
                }
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to restore hold cart: ${e.message}"))
            }
        }
    }

    fun deleteHoldCart(holdCartId: Long) {
        viewModelScope.launch {
            try {
                holdCartRepository.deleteHoldCart(holdCartId)
                loadHoldCarts() // Reload to update count
                _homeUiEvent.emit(HomeUiEvent.HoldCartSuccess("Hold cart deleted successfully!"))
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to delete hold cart: ${e.message}"))
            }
        }
    }

    fun createOrder(paymentMethod: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storeId = preferenceManager.getStoreID()
                val userId = preferenceManager.getUserID()
                val registerId = preferenceManager.getOccupiedRegisterID()
                val locationId = preferenceManager.getOccupiedLocationID()
                val customerId = preferenceManager.getCustomerID()

                // Get batch details from database
                val batch = homeRepository.getBatchDetails()
                val batchId = batch.batchId

                // Generate order number
                val orderNo = generateOrderNumber()

                // Convert cart items to CheckOutOrderItem list
                val orderItems = _cartItems.value.map { cartItem ->
                    val selectedPrice =
                        if (paymentMethod == "cash") cartItem.cashPrice else cartItem.cardPrice
                    val hasProductDiscount = cartItem.discountType != null && cartItem.discountValue != null && cartItem.discountValue!! > 0.0
                    val discountedPrice = if (hasProductDiscount) cartItem.getProductDiscountedPrice() else selectedPrice
                    val discountAmount = if (hasProductDiscount) cartItem.getProductDiscountAmount() else 0.0
                    
                    CheckOutOrderItem(
                        productId = cartItem.productId,
                        quantity = cartItem.quantity,
                        productVariantId = cartItem.productVariantId,
                        name = cartItem.name,
                        isCustom = false,
                        price = selectedPrice,
                        barCode = null,
                        reason = cartItem.discountReason,
                        discountId = cartItem.discountId,
                        discountedPrice = discountedPrice,
                        discountedAmount = if (hasProductDiscount) discountAmount else null,
                        fixedDiscount = when (cartItem.discountType) {
                            DiscountType.AMOUNT -> cartItem.discountValue ?: 0.0
                            else -> 0.0
                        },
                        discountReason = cartItem.discountReason,
                        fixedPercentageDiscount = when (cartItem.discountType) {
                            DiscountType.PERCENTAGE -> cartItem.discountValue ?: 0.0
                            else -> 0.0
                        },
                        discountType = when (cartItem.discountType) {
                            DiscountType.AMOUNT -> "amount"
                            DiscountType.PERCENTAGE -> "percentage"
                            else -> ""
                        },
                        cardPrice = cartItem.cardPrice
                    )
                }

                // Create order request
                val orderRequest = CreateOrderRequest(
                    orderNo = orderNo,
                    customerId = if (customerId > 0) customerId else null,
                    storeId = storeId,
                    locationId = locationId,
                    storeRegisterId = registerId,
                    paymentMethod = paymentMethod,
                    isRedeemed = false,
                    source = "point-of-sale",
                    items = orderItems,
                    subTotal = _subtotal.value,
                    total = _totalAmount.value,
                    applyTax = true,
                    taxValue = _tax.value,
                    discountAmount = _orderDiscountTotal.value,
                    cashDiscountAmount = _cashDiscountTotal.value,
                    rewardDiscount = 0.0,
                    batchId = batchId,
                    cashierId = userId,
                    cardDetails = null
                )

                // Always save to local database first
                pendingOrderRepository.saveOrderToLocal(orderRequest)

                // Try to sync with server if internet is available
                if (networkMonitor.isNetworkAvailable()) {
                    try {
                        val unsyncedOrders = pendingOrderRepository.getUnsyncedOrders()
                        if (unsyncedOrders.isNotEmpty()) {
                            val lastOrder = unsyncedOrders.last()
                            pendingOrderRepository.syncOrderToServer(lastOrder).onSuccess {
                                _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Order created successfully!"))
                            }.onFailure { e ->
                                android.util.Log.e(
                                    "Order",
                                    "Failed to sync order: ${e.message}\n Your order has been saved in pending orders"
                                )
                                _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Failed to sync order: ${e.message}\nYour order has been saved in pending orders"))
                            }
                        } else {
                            _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("No Orders Found"))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Order", "Failed to sync order: ${e.message}")
                        _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Failed to sync order: ${e.message}\nYour order has been saved in pending orders"))
                    }
                } else {
                    _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Order saved offline and will sync when internet is available"))
                }

                // Clear cart after successful order creation
                clearCart()

            } catch (e: Exception) {
                android.util.Log.e("Order", "Failed to create order: ${e.message}")
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to create order: ${e.message}"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowLoading)
            try {
                val response = storeRegistersRepository.logout()
                response.message.let {
                    preferenceManager.setLogin(false)
                    _homeUiEvent.emit(HomeUiEvent.HideLoading)
                    _homeUiEvent.emit(HomeUiEvent.NavigateToLogin)
                }

            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                _homeUiEvent.emit(
                    HomeUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    /**
     * Generates a unique order number based on store, location, register, user IDs and timestamp
     * Format: S{storeId}L{locationId}R{registerId}U{userId}-{epochMillis}
     * @return Unique order number string
     */
    fun generateOrderNumber(): String {
        val storeId = preferenceManager.getStoreID()
        val locationId = preferenceManager.getOccupiedLocationID()
        val registerId = preferenceManager.getOccupiedRegisterID()
        val userId = preferenceManager.getUserID()
        val epochMillis = System.currentTimeMillis()
        
        return "S${storeId}L${locationId}R${registerId}U${userId}-$epochMillis"
    }
}
