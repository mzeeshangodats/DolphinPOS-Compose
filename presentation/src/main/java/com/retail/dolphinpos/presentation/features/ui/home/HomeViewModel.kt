package com.retail.dolphinpos.presentation.features.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.CreateOrderTransactionDao
import com.retail.dolphinpos.data.entities.transaction.CreateOrderTransactionEntity
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import com.retail.dolphinpos.data.repositories.hold_cart.HoldCartRepository
import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import com.retail.dolphinpos.domain.model.home.create_order.CardDetails
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
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CancelTransactionUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ProcessTransactionUseCase
import com.retail.dolphinpos.domain.usecases.order.GetLatestOnlineOrderUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.PrintOrderReceiptUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.OpenCashDrawerUseCase
import com.retail.dolphinpos.domain.usecases.tax.PricingCalculationUseCase
import com.retail.dolphinpos.domain.usecases.tax.DiscountOrder
import com.retail.dolphinpos.domain.usecases.tax.PricingConfiguration
import com.retail.dolphinpos.domain.model.TaxDetail
import com.retail.dolphinpos.domain.model.home.cart.applyTax
import com.retail.dolphinpos.domain.model.home.cart.price
import com.retail.dolphinpos.data.customer_display.CustomerDisplayManager
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val orderRepository: OrderRepositoryImpl,
    private val createOrderTransactionDao: CreateOrderTransactionDao,
    private val gson: Gson,
    private val networkMonitor: NetworkMonitor,
    private val storeRegistersRepository: StoreRegistersRepository,
    private val verifyPinRepository: VerifyPinRepository,
    private val batchReportRepository: BatchReportRepository,
    private val initializeTerminalUseCase: InitializeTerminalUseCase,
    private val processTransactionUseCase: ProcessTransactionUseCase,
    private val cancelTransactionUseCase: CancelTransactionUseCase,
    private val getLatestOnlineOrderUseCase: GetLatestOnlineOrderUseCase,
    private val printOrderReceiptUseCase: PrintOrderReceiptUseCase,
    private val openCashDrawerUseCase: OpenCashDrawerUseCase,
    private val customerDisplayManager: CustomerDisplayManager,
    private val pricingCalculationUseCase: PricingCalculationUseCase,
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

    private val _cashTotal = MutableStateFlow(0.0)
    val cashTotal: StateFlow<Double> = _cashTotal.asStateFlow()

    private val _cardTotal = MutableStateFlow(0.0)
    val cardTotal: StateFlow<Double> = _cardTotal.asStateFlow()

    private val _isTaxExempt = MutableStateFlow(false)
    val isTaxExempt: StateFlow<Boolean> = _isTaxExempt.asStateFlow()

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

    // PAX Terminal Session ID (stored as domain model, not SDK type)
    private var currentSessionId: String? = null

    init {
        loadCategories()
        loadMenus()
        resetOrderDiscountValues()  // Reset order discount values on initialization
        // Start customer display server if enabled
        customerDisplayManager.restartServerIfNeeded()
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
            ), BottomMenu(
                menuName = "Cash Drawer", destinationId = R.id.cashDrawerScreen
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

    fun handleBarcodeScan(barcode: String) {
        viewModelScope.launch {
            if (barcode.isBlank()) {
                return@launch
            }

            try {
                val product = homeRepository.searchProductByBarcode(barcode)
                if (product != null) {
                    // Check if the barcode matches a variant SKU
                    val matchingVariant = product.variants?.find { it.sku == barcode }

                    if (matchingVariant != null) {
                        // Add variant to cart
                        val success = addVariantToCart(product, matchingVariant)
                        if (success) {
//                            _homeUiEvent.emit(HomeUiEvent.ShowSuccess("Product added to cart: ${product.name} - ${matchingVariant.title}"))
                        } else {
                            _homeUiEvent.emit(HomeUiEvent.ShowError("Cannot add product to cart. Cash discount may be applied."))
                        }
                    } else {
                        // Add product to cart
                        val success = addToCart(product)
                        if (success) {
//                            _homeUiEvent.emit(HomeUiEvent.ShowSuccess("Product added to cart: ${product.name}"))
                        } else {
                            _homeUiEvent.emit(HomeUiEvent.ShowError("Cannot add product to cart. Cash discount may be applied."))
                        }
                    }
                } else {
                    _homeUiEvent.emit(HomeUiEvent.ShowError("Product not found for barcode: $barcode"))
                }
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("Error scanning barcode: ${e.message}"))
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
                    selectedPrice = product.cardPrice.toDouble(),  // Always add new products at card price
                    productTaxDetails = product.taxDetails  // Include product tax details
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
                    selectedPrice = variantCardPrice,  // Always add new products at card price
                    productTaxDetails = variant.taxDetails ?: product.taxDetails  // Prefer variant tax details, fallback to product
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

    fun removeFromCart(productId: Int, variantId: Int?): Boolean {
        if (!canRemoveItemFromCart()) {
            return false  // Cannot remove item after cash discount applied
        }

        val updatedCart = _cartItems.value.filter { cartItem ->
            // If variantId is provided, remove only that specific variant
            // Otherwise, remove all items with that productId (non-variant products)
            if (variantId != null) {
                cartItem.productId != productId || cartItem.productVariantId != variantId
            } else {
                cartItem.productId != productId
            }
        }
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
        // Find index based on both productId and variantId
        val index = currentList.indexOfFirst { item ->
            item.productId == updatedItem.productId && item.productVariantId == updatedItem.productVariantId
        }
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

    fun toggleTaxExempt() {
        val newValue = !_isTaxExempt.value
        Log.d("HomeViewModel", "=== TAX EXEMPT TOGGLE ===")
        Log.d("HomeViewModel", "Previous state: ${_isTaxExempt.value}")
        Log.d("HomeViewModel", "New state: $newValue")
        _isTaxExempt.value = newValue
        Log.d("HomeViewModel", "Recalculating totals with tax exempt: $newValue")
        // Recalculate totals after toggling tax exempt status
        calculateSubtotal(_cartItems.value)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        isCashSelected = false  // Set default to card when cart is cleared
        _isTaxExempt.value = false  // Reset tax exempt status when cart is cleared
        resetOrderDiscountValues()  // Reset order discount values when cart is cleared
        calculateSubtotal(emptyList())
    }

    fun openCashDrawer(reason: String) {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowLoading)
            try {
                val (success, message) = openCashDrawerUseCase { statusMessage ->
                    // Status updates can be handled here if needed
                }
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                if (success) {
                    _homeUiEvent.emit(HomeUiEvent.ShowSuccess(message))
                } else {
                    _homeUiEvent.emit(HomeUiEvent.ShowError(message))
                }
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to open cash drawer: ${e.message}"))
            }
        }
    }

    fun printLatestOnlineOrder() {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowLoading)
            try {
                // Get latest synced online order only (not pending orders)
                val latestOrder = getLatestOnlineOrderUseCase()

                if (latestOrder == null) {
                    Log.d("HomeViewModel", "No online orders found to print")
                    _homeUiEvent.emit(HomeUiEvent.ShowError("No online orders found to print."))
                } else {
                    Log.d("HomeViewModel", "Printing latest online order: ${latestOrder.orderNumber}, Subtotal: ${latestOrder.subTotal}, Tax: ${latestOrder.taxValue}, Total: ${latestOrder.total}")
                    val statusMessages = mutableListOf<String>()
                    val result = printOrderReceiptUseCase(latestOrder) { statusMessages.add(it) }
                    if (result.isSuccess) {
                        val successMessage = statusMessages.lastOrNull { it.contains("success", ignoreCase = true) }
                            ?: "Print command sent successfully."
                        _homeUiEvent.emit(HomeUiEvent.ShowSuccess(successMessage))
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message
                            ?: statusMessages.lastOrNull { message ->
                                val normalized = message.lowercase(Locale.US)
                                normalized.contains("error") || normalized.contains("fail")
                            }
                            ?: "Failed to print receipt."
                        _homeUiEvent.emit(HomeUiEvent.ShowError(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error printing order: ${e.message}", e)
                _homeUiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Failed to print receipt."))
            } finally {
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
            }
        }
    }

    private fun calculateCashDiscount(cartItems: List<CartItem>) {
        // Reset cash discount if cart is empty or cash is not selected
        if (cartItems.isEmpty() || !isCashSelected) {
            _cashDiscountTotal.value = 0.0
            return
        }

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

            // Cash discount should never be negative - if cash prices are higher, there's no discount
            _cashDiscountTotal.value = (cardBasedSubtotal - cashBasedSubtotal).coerceAtLeast(0.0)
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
        _orderLevelDiscounts.value = emptyList()
        // Recalculate subtotal to reflect discount removal
        calculateSubtotal(_cartItems.value)
    }

    fun removeAllOrderDiscounts() {
        _orderLevelDiscounts.value = emptyList()
        calculateSubtotal(_cartItems.value)
    }

    private fun calculateSubtotal(cartItems: List<CartItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1️⃣ Base subtotal (card prices minus product-level discounts only)
            cartItems.sumOf { it.cardPrice * it.quantity }

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

            // 4️⃣ Recalculate cash discount first (before using it in calculation)
            calculateCashDiscount(cartItems)

            // Get the current cash discount value after recalculation
            var currentCashDiscount = _cashDiscountTotal.value

            // Don't apply cash discount if subtotal is already 0 or less
            if (discountedSubtotal <= 0) {
                currentCashDiscount = 0.0
                withContext(Dispatchers.Main) {
                    _cashDiscountTotal.value = 0.0
                }
            }

            // 5️⃣ Apply cash discount (after order-level discounts)
            val finalSubtotal = discountedSubtotal - currentCashDiscount

            // 6️⃣ Tax calculation using PricingCalculationUseCase (supports fixed and percentage tax)
            var taxValue: Double
            var updatedCartItems: List<CartItem>

            try {
                Log.d("HomeViewModel", "=== TAX CALCULATION START ===")
                Log.d("HomeViewModel", "Cart items: ${cartItems.size}")
                Log.d("HomeViewModel", "Final subtotal (after discounts): $finalSubtotal")
                Log.d("HomeViewModel", "Is cash selected: $isCashSelected")
                Log.d("HomeViewModel", "Tax exempt: ${_isTaxExempt.value}")

                // Get location tax details
                val locationId = preferenceManager.getOccupiedLocationID()
                val location = verifyPinRepository.getLocationByLocationID(locationId)
                val locationTaxDetails = location.taxDetails
                Log.d("HomeViewModel", "Location tax details count: ${locationTaxDetails?.size ?: 0}")
                locationTaxDetails?.forEach { tax ->
                    Log.d("HomeViewModel", "  Store tax: ${tax.title} - ${tax.value}% (isDefault: ${tax.isDefault})")
                }

                // Convert OrderDiscount to DiscountOrder for PricingCalculationUseCase
                val discountOrder = if (_orderLevelDiscounts.value.isNotEmpty()) {
                    val firstDiscount = _orderLevelDiscounts.value.first()
                    Log.d("HomeViewModel", "Order discount: ${firstDiscount.type} - ${firstDiscount.value}")
                    DiscountOrder(
                        discountType = when (firstDiscount.type) {
                            DiscountType.PERCENTAGE -> "percentage"
                            DiscountType.AMOUNT -> "amount"
                        },
                        percentage = if (firstDiscount.type == DiscountType.PERCENTAGE) firstDiscount.value else 0.0,
                        amount = if (firstDiscount.type == DiscountType.AMOUNT) firstDiscount.value else 0.0
                    )
                } else {
                    Log.d("HomeViewModel", "No order-level discount")
                    null
                }

                // Calculate tax rate from location (fallback to 10% if not available)
                val taxRate = location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                Log.d("HomeViewModel", "Tax rate: $taxRate (${location.taxValue}%)")

                // Log each cart item's tax details
                cartItems.forEach { item ->
                    Log.d("HomeViewModel", "Item: ${item.name}, applyTax: ${item.applyTax}, productTaxDetails: ${item.productTaxDetails?.size ?: 0}")
                    item.productTaxDetails?.forEach { tax ->
                        Log.d("HomeViewModel", "  Product tax: ${tax.title} - ${tax.value}% (isDefault: ${tax.isDefault})")
                    }
                }

                // Use PricingCalculationUseCase to calculate tax
                val pricingConfig = PricingConfiguration(
                    isTaxApplied = true,
                    taxRate = taxRate,
                    useCardPricing = !isCashSelected,
                    taxDetails = locationTaxDetails,
                    taxExempt = _isTaxExempt.value  // Use tax exempt state
                )
                Log.d("HomeViewModel", "Pricing config - isTaxApplied: true, taxExempt: ${_isTaxExempt.value}, useCardPricing: ${!isCashSelected}")

                val pricingResult = pricingCalculationUseCase.calculatePricing(
                    cartItems = cartItems,
                    discountOrder = discountOrder,
                    rewardAmount = 0.0, // TODO: Add reward discount if needed
                    config = pricingConfig
                )

                taxValue = pricingResult.tax
                updatedCartItems = pricingResult.cartItemsWithTax

                Log.d("HomeViewModel", "Tax calculation result:")
                Log.d("HomeViewModel", "  Total tax: $taxValue")
                Log.d("HomeViewModel", "  Items with tax: ${updatedCartItems.size}")
                updatedCartItems.forEach { item ->
                    Log.d("HomeViewModel", "    ${item.name}: tax=${item.productTaxAmount}, taxable=${item.productTaxableAmount}")
                }
                Log.d("HomeViewModel", "=== TAX CALCULATION END ===")

                // Update cart items with tax information
                withContext(Dispatchers.Main) {
                    _cartItems.value = updatedCartItems
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Tax calculation failed, using fallback: ${e.message}")
                e.printStackTrace()

                // Fallback to simple tax calculation if PricingCalculationUseCase fails
                // Check if tax is exempt first
                if (_isTaxExempt.value) {
                    Log.d("HomeViewModel", "Fallback: Tax exempt, setting tax to 0")
                    taxValue = 0.0
                    updatedCartItems = cartItems.map { it.copy(
                        productTaxAmount = 0.0,
                        productTaxRate = 0.0,
                        productTaxableAmount = 0.0
                    )}
                } else {
                    // Get location tax rate for fallback calculation
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val location = try {
                        verifyPinRepository.getLocationByLocationID(locationId)
                    } catch (ex: Exception) {
                        null
                    }
                    val taxRate = location?.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    Log.d("HomeViewModel", "Fallback: Using tax rate: $taxRate (${location?.taxValue ?: 10}%)")

                    // Calculate taxable base (only items that should be taxed)
                    val taxableBase = cartItems.sumOf { cart ->
                        if (cart.applyTax == true) {
                            val itemPrice = if (isCashSelected) {
                                if (cart.isDiscounted && cart.cashDiscountedPrice > 0.0) {
                                    cart.cashDiscountedPrice
                                } else {
                                    cart.cashPrice
                                }
                            } else {
                                if (cart.isDiscounted && cart.discountPrice != null) {
                                    cart.discountPrice!!
                                } else {
                                    cart.price ?: 0.0
                                }
                            }
                            itemPrice * (cart.quantity ?: 1)
                        } else {
                            0.0
                        }
                    }

                    // Apply proportion of discounts to taxable amount
                    val taxableAfterOrderDiscounts = if (productDiscountedSubtotal > 0 && taxableBase > 0) {
                        taxableBase * (finalSubtotal / productDiscountedSubtotal)
                    } else {
                        finalSubtotal  // If no taxable items, use final subtotal
                    }

                    // Calculate tax using actual tax rate
                    taxValue = taxableAfterOrderDiscounts * taxRate
                    Log.d("HomeViewModel", "Fallback tax calculation:")
                    Log.d("HomeViewModel", "  Taxable base: $taxableBase")
                    Log.d("HomeViewModel", "  Taxable after discounts: $taxableAfterOrderDiscounts")
                    Log.d("HomeViewModel", "  Tax rate: $taxRate")
                    Log.d("HomeViewModel", "  Calculated tax: $taxValue")

                    // Update cart items with fallback tax (distributed proportionally)
                    updatedCartItems = cartItems.map { item ->
                        if (item.applyTax && taxableAfterOrderDiscounts > 0) {
                            val itemPrice = if (isCashSelected) {
                                if (item.isDiscounted && item.cashDiscountedPrice > 0.0) {
                                    item.cashDiscountedPrice
                                } else {
                                    item.cashPrice
                                }
                            } else {
                                if (item.isDiscounted && item.discountPrice != null) {
                                    item.discountPrice!!
                                } else {
                                    item.price ?: 0.0
                                }
                            }
                            val itemSubtotal = itemPrice * (item.quantity ?: 1)
                            val itemProportion = if (taxableBase > 0) itemSubtotal / taxableBase else 0.0
                            val itemTaxableAmount = taxableAfterOrderDiscounts * itemProportion
                            val itemTaxAmount = itemTaxableAmount * taxRate

                            item.copy(
                                productTaxAmount = itemTaxAmount,
                                productTaxRate = taxRate,
                                productTaxableAmount = itemTaxableAmount
                            )
                        } else {
                            item.copy(
                                productTaxAmount = 0.0,
                                productTaxRate = 0.0,
                                productTaxableAmount = 0.0
                            )
                        }
                    }
                }

                Log.d("HomeViewModel", "Fallback tax calculated: $taxValue")
            }

            // 7️⃣ Final total
            val totalAmount = finalSubtotal + taxValue

            // 8️⃣ Calculate cash and card totals separately for display
            // Calculate cash-based subtotal (using cash prices)
            val cashBasedProductDiscountedSubtotal = cartItems.sumOf { cartItem ->
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
            
            // Apply order-level discounts to cash subtotal
            var cashDiscountedSubtotal = cashBasedProductDiscountedSubtotal
            for (discount in _orderLevelDiscounts.value) {
                cashDiscountedSubtotal = when (discount.type) {
                    DiscountType.PERCENTAGE -> {
                        cashDiscountedSubtotal - (cashDiscountedSubtotal * discount.value / 100.0)
                    }
                    DiscountType.AMOUNT -> {
                        cashDiscountedSubtotal - discount.value
                    }
                }
            }
            
            // Calculate card-based subtotal (using card prices) - already calculated as productDiscountedSubtotal
            var cardDiscountedSubtotal = productDiscountedSubtotal
            for (discount in _orderLevelDiscounts.value) {
                cardDiscountedSubtotal = when (discount.type) {
                    DiscountType.PERCENTAGE -> {
                        cardDiscountedSubtotal - (cardDiscountedSubtotal * discount.value / 100.0)
                    }
                    DiscountType.AMOUNT -> {
                        cardDiscountedSubtotal - discount.value
                    }
                }
            }
            
            // Calculate tax for cash scenario
            val cashTax = if (_isTaxExempt.value) {
                0.0
            } else {
                try {
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val location = verifyPinRepository.getLocationByLocationID(locationId)
                    val taxRate = location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    
                    // Calculate taxable amount for cash
                    val cashTaxableAmount = cartItems.sumOf { cartItem ->
                        if (cartItem.chargeTaxOnThisProduct == true) {
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
                        } else {
                            0.0
                        }
                    }
                    
                    // Apply order discount proportionally to taxable amount
                    val cashTaxableAfterDiscounts = if (cashBasedProductDiscountedSubtotal > 0 && cashTaxableAmount > 0) {
                        cashTaxableAmount * (cashDiscountedSubtotal / cashBasedProductDiscountedSubtotal)
                    } else {
                        cashDiscountedSubtotal
                    }
                    
                    cashTaxableAfterDiscounts * taxRate
                } catch (e: Exception) {
                    // Fallback: use same tax rate as calculated for current selection
                    val taxRate = try {
                        val locationId = preferenceManager.getOccupiedLocationID()
                        val location = verifyPinRepository.getLocationByLocationID(locationId)
                        location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    } catch (ex: Exception) {
                        0.10
                    }
                    cashDiscountedSubtotal * taxRate
                }
            }
            
            // Calculate tax for card scenario
            val cardTax = if (_isTaxExempt.value) {
                0.0
            } else {
                try {
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val location = verifyPinRepository.getLocationByLocationID(locationId)
                    val taxRate = location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    
                    // Calculate taxable amount for card
                    val cardTaxableAmount = cartItems.sumOf { cartItem ->
                        if (cartItem.chargeTaxOnThisProduct == true) {
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
                        } else {
                            0.0
                        }
                    }
                    
                    // Apply order discount proportionally to taxable amount
                    val cardTaxableAfterDiscounts = if (productDiscountedSubtotal > 0 && cardTaxableAmount > 0) {
                        cardTaxableAmount * (cardDiscountedSubtotal / productDiscountedSubtotal)
                    } else {
                        cardDiscountedSubtotal
                    }
                    
                    cardTaxableAfterDiscounts * taxRate
                } catch (e: Exception) {
                    // Fallback: use same tax rate as calculated for current selection
                    val taxRate = try {
                        val locationId = preferenceManager.getOccupiedLocationID()
                        val location = verifyPinRepository.getLocationByLocationID(locationId)
                        location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    } catch (ex: Exception) {
                        0.10
                    }
                    cardDiscountedSubtotal * taxRate
                }
            }
            
            // Calculate final totals
            val cashTotalAmount = cashDiscountedSubtotal + cashTax
            val cardTotalAmount = cardDiscountedSubtotal + cardTax

            withContext(Dispatchers.Main) {
                _subtotal.value =
                    subtotal   // subtotal shows card prices minus product-level discounts only
                _orderDiscountTotal.value = totalOrderDiscount
                _tax.value = taxValue
                _totalAmount.value = totalAmount
                _cashTotal.value = cashTotalAmount
                _cardTotal.value = cardTotalAmount

                // Broadcast cart update to customer display
                // Determine status based on cart state
                val status = if (cartItems.isEmpty()) {
                    "WELCOME"
                } else {
                    "CHECKOUT_SCREEN"
                }

                customerDisplayManager.broadcastCartUpdate(
                    status = status,
                    cartItems = cartItems,
                    subtotal = subtotal,
                    tax = taxValue,
                    total = totalAmount,
                    cashDiscountTotal = _cashDiscountTotal.value,
                    orderDiscountTotal = totalOrderDiscount,
                    isCashSelected = isCashSelected
                )
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

    /**
     * Calculate the current subtotal after applying all existing order-level discounts.
     * This is used to validate that new discounts don't exceed the available amount.
     */
    fun getCurrentSubtotalAfterOrderDiscounts(): Double {
        val cartItems = _cartItems.value

        // Calculate product-level discounted subtotal
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

        // Apply all existing order-level discounts sequentially
        var discountedSubtotal = productDiscountedSubtotal
        for (discount in _orderLevelDiscounts.value) {
            discountedSubtotal = when (discount.type) {
                DiscountType.PERCENTAGE -> {
                    discountedSubtotal - (discountedSubtotal * discount.value / 100.0)
                }
                DiscountType.AMOUNT -> {
                    discountedSubtotal - discount.value
                }
            }
            // Ensure subtotal doesn't go below 0
            if (discountedSubtotal < 0) {
                discountedSubtotal = 0.0
                break
            }
        }

        return discountedSubtotal.coerceAtLeast(0.0)
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
        // Format with thousand separators and 2 decimal places, e.g. 1,234.56
        return String.format(Locale.US, "%,.2f", amount)
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

                holdCartRepository.saveHoldCart(
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
//                _homeUiEvent.emit(HomeUiEvent.HoldCartSuccess("Hold cart deleted successfully!"))
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to delete hold cart: ${e.message}"))
            }
        }
    }

    fun createOrder(paymentMethod: String, cardDetails: CardDetails? = null) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Show loading dialog
                _homeUiEvent.emit(HomeUiEvent.ShowLoading)

                val storeId = preferenceManager.getStoreID()
                val userId = preferenceManager.getUserID()
                val registerId = preferenceManager.getOccupiedRegisterID()
                val locationId = preferenceManager.getOccupiedLocationID()
                val customerId = preferenceManager.getCustomerID()

                // Get batch details from database
                val batch = batchReportRepository.getBatchDetails()

                // Generate order number
                val orderNumber = generateOrderNumber()
                
                // Generate invoice number
                val invoiceNo = generateInvoiceNo()

                // Capture current values before clearing cart
                val finalSubtotal = _subtotal.value
                val finalCashDiscount = _cashDiscountTotal.value
                val finalOrderDiscount = _orderDiscountTotal.value
                val finalTax = _tax.value
                val finalTotal = _totalAmount.value

                // Convert cart items to CheckOutOrderItem list
                val orderItems = _cartItems.value.map { cartItem ->
                    val selectedPrice =
                        if (paymentMethod == "cash") cartItem.cashPrice else cartItem.cardPrice
                    val hasProductDiscount =
                        cartItem.discountType != null && cartItem.discountValue != null && cartItem.discountValue!! > 0.0
                    val discountedPrice =
                        if (hasProductDiscount) cartItem.getProductDiscountedPrice() else selectedPrice
                    val discountAmount =
                        if (hasProductDiscount) cartItem.getProductDiscountAmount() else 0.0

                    // Calculate tax amounts per item based on item's price (after discounts) and quantity
                    // This ensures tax breakdown is based on product-level prices, not subtotal
                    val itemTaxDetails = cartItem.productTaxDetails?.map { taxDetail ->
                        // Calculate tax amount based on this item's price and quantity
                        // Use discounted price if available, otherwise use selectedPrice
                        val itemPrice = discountedPrice
                        val itemQuantity = cartItem.quantity ?: 1
                        val itemSubtotal = itemPrice * itemQuantity

                        // Calculate tax amount for this item based on the tax rate
                        val taxAmount = when (taxDetail.type?.lowercase()) {
                            "percentage" -> {
                                val rate = taxDetail.value / 100.0
                                itemSubtotal * rate
                            }
                            "fixed amount" -> {
                                taxDetail.value * itemQuantity
                            }
                            else -> {
                                val rate = taxDetail.value / 100.0
                                itemSubtotal * rate
                            }
                        }

                        Log.d("HomeViewModel", "Tax calculation for item: ${cartItem.name}, Price: $itemPrice, Qty: $itemQuantity, Subtotal: $itemSubtotal, Tax: ${taxDetail.title} (${taxDetail.value}%), Amount: $taxAmount")

                        // Return tax detail with calculated amount based on item price
                        TaxDetail(
                            type = taxDetail.type,
                            title = taxDetail.title,
                            value = taxDetail.value,
                            amount = taxAmount,  // Tax amount calculated from this item's price
                            isDefault = taxDetail.isDefault,
                            refundedTax = taxDetail.refundedTax
                        )
                    }?.takeIf { it.isNotEmpty() }

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
                        cardPrice = cartItem.cardPrice,
                        // Include product-level tax (totalTax = productTaxAmount per item)
                        totalTax = cartItem.productTaxAmount.takeIf { it > 0.0 },
                        // Include tax breakdown with calculated amounts based on item prices
                        appliedTaxes = itemTaxDetails
                    )
                }

                // Create dummy card details if card payment is selected
                if (cardDetails == null) {
                    CardDetails(
                        terminalInvoiceNo = "Dummy${orderNumber}",
                        transactionId = "TXN${System.currentTimeMillis()}",
                        authCode = "AUTH${(1000..9999).random()}",
                        rrn = "RRN${System.currentTimeMillis()}",
                        brand = "VISA",
                        last4 = "1234",
                        entryMethod = "SWIPE",
                        merchantId = "MERCH${storeId}",
                        terminalId = "TERM${registerId}",
                    )
                }

                // Calculate store-level tax details (default taxes only) for order-level breakdown
                // For cash payments, use cash-based subtotal; for card, use card-based subtotal
                val taxBaseSubtotal = if (paymentMethod == "cash") {
                    // Calculate subtotal based on cash prices (same logic as when creating order items)
                    val cashBaseSubtotal = _cartItems.value.sumOf { cartItem ->
                        val hasProductDiscount =
                            cartItem.discountType != null && cartItem.discountValue != null && cartItem.discountValue!! > 0.0
                        val discountedCashPrice = if (hasProductDiscount) {
                            // Calculate discounted cash price using the same method as when creating order items
                            when (cartItem.discountType) {
                                DiscountType.PERCENTAGE -> {
                                    cartItem.cashPrice - ((cartItem.cashPrice * (cartItem.discountValue ?: 0.0)) / 100.0)
                                }
                                DiscountType.AMOUNT -> {
                                    cartItem.cashPrice - (cartItem.discountValue ?: 0.0)
                                }
                                else -> cartItem.cashPrice
                            }
                        } else {
                            cartItem.cashPrice
                        }
                        discountedCashPrice * (cartItem.quantity ?: 1)
                    }

                    // Apply order-level discounts (same as card calculation)
                    var discountedCashSubtotal = cashBaseSubtotal
                    for (discount in _orderLevelDiscounts.value) {
                        discountedCashSubtotal = when (discount.type) {
                            DiscountType.PERCENTAGE -> {
                                discountedCashSubtotal - (discountedCashSubtotal * discount.value / 100.0)
                            }
                            DiscountType.AMOUNT -> {
                                discountedCashSubtotal - discount.value
                            }
                        }
                    }

                    // Apply cash discount (cash discount is already applied, so just use the result)
                    discountedCashSubtotal - finalCashDiscount
                } else {
                    // For card payments, use the card-based finalSubtotal
                    finalSubtotal
                }

                Log.d("HomeViewModel", "Tax calculation base - Payment: $paymentMethod, Card subtotal: $finalSubtotal, Tax base subtotal: $taxBaseSubtotal")

                val storeTaxDetails = try {
                    val location = verifyPinRepository.getLocationByLocationID(locationId)
                    location.taxDetails?.filter { it.isDefault == true }?.map { taxDetail ->
                        // Calculate tax amount based on payment-method-specific subtotal
                        val taxAmount = when (taxDetail.type?.lowercase()) {
                            "percentage" -> {
                                val rate = taxDetail.value / 100.0
                                taxBaseSubtotal * rate
                            }
                            "fixed amount" -> taxDetail.value
                            else -> {
                                val rate = taxDetail.value / 100.0
                                taxBaseSubtotal * rate
                            }
                        }
                        // Return taxDetail with calculated amount
                        TaxDetail(
                            type = taxDetail.type,
                            title = taxDetail.title,
                            value = taxDetail.value,
                            amount = taxAmount,
                            isDefault = taxDetail.isDefault,
                            refundedTax = taxDetail.refundedTax
                        )
                    } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                // Create order request using captured values
                // If tax is exempt, set applyTax = false and taxDetails = emptyList()
                val isTaxExempt = _isTaxExempt.value
                val orderApplyTax = !isTaxExempt
                val orderTaxDetails = if (isTaxExempt) {
                    emptyList()
                } else {
                    storeTaxDetails
                }

                Log.d("HomeViewModel", "Creating order - Payment: $paymentMethod," +
                        " Subtotal: $finalSubtotal, Tax: $finalTax, Total: $finalTotal")
                Log.d("HomeViewModel", "Tax exempt: $isTaxExempt, ApplyTax: $orderApplyTax")
                if (!isTaxExempt) {
                    Log.d("HomeViewModel", "Tax details count: ${orderTaxDetails.size}")
                    orderTaxDetails.forEach { tax ->
                        Log.d("HomeViewModel", "  Tax: ${tax.title} - ${tax.value}% - Amount: ${tax.amount}")
                    }
                } else {
                    Log.d("HomeViewModel", "Tax is exempt - taxDetails will be empty")
                }

                val orderRequest = CreateOrderRequest(
                    orderNumber = orderNumber,
                    invoiceNo = invoiceNo,
                    customerId = if (customerId > 0) customerId else null,
                    storeId = storeId,
                    locationId = locationId,
                    storeRegisterId = registerId,
                    batchNo = batch.batchNo,
                    paymentMethod = paymentMethod,
                    isRedeemed = false,
                    source = "point-of-sale",
                    items = orderItems,
                    subTotal = finalSubtotal,
                    total = finalTotal,
                    applyTax = orderApplyTax,  // Set to false if tax is exempt
                    taxValue = finalTax,
                    discountAmount = finalOrderDiscount,
                    cashDiscountAmount = finalCashDiscount,
                    rewardDiscount = 0.0,
                    userId = userId,
                    cardDetails = cardDetails,
                    taxDetails = orderTaxDetails,  // Empty list if tax is exempt, otherwise store-level default taxes breakdown
                    taxExempt = isTaxExempt  // Use tax exempt state
                )

                // Always save to local database first (with isSynced = false, status = "pending")
                val orderId = orderRepository.saveOrderToLocal(orderRequest)
                Log.d("Order", "Order saved locally with ID: $orderId")
                
                // Deduct product quantities from local database
                try {
                    _cartItems.value.forEach { cartItem ->
                        val quantityToDeduct = cartItem.quantity ?: 1
                        val productVariantId = cartItem.productVariantId
                        val productId = cartItem.productId
                        
                        if (productVariantId != null) {
                            // Deduct from variant quantity
                            homeRepository.deductVariantQuantity(productVariantId, quantityToDeduct)
                        } else if (productId != null) {
                            // Deduct from product quantity
                            homeRepository.deductProductQuantity(productId, quantityToDeduct)
                        }
                    }
                    Log.d("Order", "Product quantities deducted successfully")
                } catch (e: Exception) {
                    Log.e("Order", "Failed to deduct product quantities: ${e.message}")
                    // Continue with order processing even if quantity deduction fails
                }
                
                // Save transaction to transactions table
                try {
                    val paymentMethodEnum = PaymentMethod.fromString(paymentMethod)
                    // If no internet and payment is cash, set status as "paid"
                    // Otherwise, set as "pending" to be synced later
                    val transactionStatus = if (!networkMonitor.isNetworkAvailable() && paymentMethod == "cash") {
                        "paid"
                    } else {
                        "pending"
                    }
                    
                    val transactionEntity = CreateOrderTransactionEntity(
                        orderNo = orderNumber, // orderId will be updated when order is synced to server and we get the server order ID
                        storeId = storeId,
                        locationId = locationId,
                        paymentMethod = paymentMethodEnum,
                        status = transactionStatus, // "paid" for offline cash, "pending" for others
                        amount = finalTotal,
                        invoiceNo = invoiceNo,
                        batchNo = batch.batchNo,
                        userId = userId,
                        orderSource = "register", // Since source is "point-of-sale"
                        tax = finalTax,
                        cardDetails = cardDetails?.let { gson.toJson(it) }
                    )
                    createOrderTransactionDao.insertTransaction(transactionEntity)
                    Log.d("Transaction", "Transaction saved successfully with invoice: $invoiceNo, status: $transactionStatus")
                } catch (e: Exception) {
                    Log.e("Transaction", "Failed to save transaction: ${e.message}")
                }

                // Try to sync with server if internet is available
                if (networkMonitor.isNetworkAvailable()) {
                    try {
                        // Get the order we just saved
                        val savedOrder = orderRepository.getOrderById(orderId)
                        if (savedOrder != null) {
                            // Sync order to server
                            orderRepository.syncOrderToServer(savedOrder).onSuccess { response ->
                                Log.d("Order", "Order synced to server successfully. Response: ${response.message}")
                                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                                _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Order created successfully!"))
                            }.onFailure { e ->
                                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                                Log.e(
                                    "Order",
                                    "Failed to sync order: ${e.message}\n Your order has been saved locally and will sync when internet is available"
                                )
                                _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Failed to sync order: ${e.message}\nYour order has been saved locally and will sync when internet is available"))
                            }
                        } else {
                            _homeUiEvent.emit(HomeUiEvent.HideLoading)
                            _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Order saved but could not be retrieved for syncing"))
                        }
                    } catch (e: Exception) {
                        Log.e("Order", "Failed to sync order: ${e.message}")
                        _homeUiEvent.emit(HomeUiEvent.HideLoading)
                        _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Failed to sync order: ${e.message}\nYour order has been saved locally and will sync when internet is available"))
                    }
                } else {
                    _homeUiEvent.emit(HomeUiEvent.HideLoading)
                    _homeUiEvent.emit(HomeUiEvent.OrderCreatedSuccessfully("Order saved offline and will sync when internet is available"))
                }

                // Clear cart after successful order creation
                clearCart()

            } catch (e: Exception) {
                Log.e("Order", "Failed to create order: ${e.message}")
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to create order: ${e.message}"))
                // Reset order level discounts when order fails
                resetOrderDiscountValues()
            }
        }
    }

    fun initCardPayment() {
        viewModelScope.launch {

            if (!networkMonitor.isNetworkConnected()) {
                _homeUiEvent.emit(HomeUiEvent.ShowError("No internet connection"))
                return@launch
            }

            _homeUiEvent.emit(HomeUiEvent.ShowLoading)

            initializeTerminalUseCase { result ->
                viewModelScope.launch(Dispatchers.Main) {
                    if (result.isSuccess && result.session != null) {
                        currentSessionId = result.session?.sessionId
                        processTransaction(
                            sessionId = result.session?.sessionId ?: "session_id_default",
                            amount = _totalAmount.value.toString()
                        )
                    } else {
                        _homeUiEvent.emit(HomeUiEvent.HideLoading)
                        _homeUiEvent.emit(
                            HomeUiEvent.ShowError(
                                result.message ?: "Failed to initialize terminal"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun processTransaction(sessionId: String, amount: String) {
        viewModelScope.launch {
            _homeUiEvent.emit(
                HomeUiEvent.ShowError("Please check Pax Terminal Screen and Pay with Card to Proceed")
            )

            processTransactionUseCase(
                sessionId = sessionId,
                amount = amount
            ) { result ->
                viewModelScope.launch(Dispatchers.Main) {
                    if (result.isSuccess && result.cardDetails != null) {
                        handleTransactionSuccess(result.cardDetails ?: CardDetails())
                    } else {
                        _homeUiEvent.emit(HomeUiEvent.HideLoading)
                        _homeUiEvent.emit(
                            HomeUiEvent.ShowError(result.message ?: "Transaction failed")
                        )
                    }
                }
            }
        }
    }

    private fun handleTransactionSuccess(cardDetails: CardDetails) {
        viewModelScope.launch {
            _homeUiEvent.emit(
                HomeUiEvent.ShowSuccess("Transaction Successful!")
            )
            delay(100)
            createOrder("card", cardDetails)
        }
    }

    private fun cancelTransaction() {
        viewModelScope.launch {
            val sessionId = currentSessionId
            if (sessionId != null) {
                cancelTransactionUseCase(sessionId)
                currentSessionId = null
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

    /**
     * Generates a unique invoice number based on store, location, register, user IDs and timestamp
     * Format: INV_S{storeId}L{locationId}R{registerId}U{userId}-{epochMillis}
     * Follows the same pattern as batch number generation but with "INV" prefix
     * @return Unique invoice number string
     */
    fun generateInvoiceNo(): String {
        val storeId = preferenceManager.getStoreID()
        val locationId = preferenceManager.getOccupiedLocationID()
        val registerId = preferenceManager.getOccupiedRegisterID()
        val userId = preferenceManager.getUserID()
        val epochMillis = System.currentTimeMillis()

        return "INV_S${storeId}L${locationId}R${registerId}U${userId}-$epochMillis"
    }

    private suspend fun getTaxPercentage(): Double {
        return try {
            val activeUserDetails = verifyPinRepository.getActiveUserDetails()
            val taxValueString = activeUserDetails?.taxValue
            taxValueString?.toDoubleOrNull() ?: 10.0
        } catch (e: Exception) {
            // If any error occurs, default to 0.0
            0.0
        }
    }
}
