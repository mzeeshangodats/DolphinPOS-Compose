package com.retail.dolphinpos.presentation.features.ui.home

import android.content.Context
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
import com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.cart.DiscountType
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountedPrice
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountAmount
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
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
import com.retail.dolphinpos.domain.usecases.tax.PricingSummaryUseCase
import com.retail.dolphinpos.domain.usecases.tax.DynamicTaxCalculationUseCase
import com.retail.dolphinpos.domain.usecases.tax.DiscountOrder
import com.retail.dolphinpos.domain.usecases.tax.PricingConfiguration
import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail
import com.retail.dolphinpos.domain.model.home.cart.applyTax
import com.retail.dolphinpos.domain.model.home.cart.price
import com.retail.dolphinpos.data.customer_display.CustomerDisplayManager
import com.retail.dolphinpos.data.repositories.sync.PosSyncRepository
import com.retail.dolphinpos.domain.usecases.sync.ScheduleSyncUseCase
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.select_registers.request.VerifyRegisterRequest
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
    val pricingSummaryUseCase: PricingSummaryUseCase,
    private val dynamicTaxCalculationUseCase: DynamicTaxCalculationUseCase,
    private val posSyncRepository: PosSyncRepository,
    private val scheduleSyncUseCase: ScheduleSyncUseCase,
    private val apiService: ApiService,
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

    // Store original chargeTaxOnThisProduct values for restoration when tax is applied again
    private val originalTaxStatusMap = mutableMapOf<String, Boolean>()

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

    // Closing amount dialog state
    private val _showClosingAmountDialog = MutableStateFlow(false)
    val showClosingAmountDialog: StateFlow<Boolean> = _showClosingAmountDialog.asStateFlow()

    // Split Payment state management
    private val _isSplitPaymentEnabled = MutableStateFlow(false)
    val isSplitPaymentEnabled: StateFlow<Boolean> = _isSplitPaymentEnabled.asStateFlow()

    private val _splitTransactions = MutableStateFlow<List<CheckoutSplitPaymentTransactions>>(emptyList())
    val splitTransactions: StateFlow<List<CheckoutSplitPaymentTransactions>> = _splitTransactions.asStateFlow()

    // Store the original total amount when split payment is enabled (to prevent recalculation issues)
    private val _splitPaymentTotal = MutableStateFlow(0.0)
    
    private val _remainingAmount = MutableStateFlow(0.0)
    val remainingAmount: StateFlow<Double> = _remainingAmount.asStateFlow()

    private val _currentTenderAmount = MutableStateFlow(0.0)
    val currentTenderAmount: StateFlow<Double> = _currentTenderAmount.asStateFlow()

    // Payment success dialog state for split payments
    private val _showSplitPaymentSuccessDialog = MutableStateFlow(false)
    val showSplitPaymentSuccessDialog: StateFlow<Boolean> = _showSplitPaymentSuccessDialog.asStateFlow()

    private val _splitPaymentSuccessData = MutableStateFlow<Pair<Double, Double>>(Pair(0.0, 0.0)) // (tendered, remaining)
    val splitPaymentSuccessData: StateFlow<Pair<Double, Double>> = _splitPaymentSuccessData.asStateFlow()

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
            ), BottomMenu(
                menuName = "Quick Access", destinationId = R.id.quickAccessScreen
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

    private val _priceCheckProduct = MutableStateFlow<Products?>(null)
    val priceCheckProduct: StateFlow<Products?> = _priceCheckProduct.asStateFlow()

    fun searchProductForPriceCheck(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _priceCheckProduct.value = null
                return@launch
            }

            try {
                // First try searching by barcode
                val productByBarcode = homeRepository.searchProductByBarcode(query)
                if (productByBarcode != null) {
                    _priceCheckProduct.value = productByBarcode
                    return@launch
                }

                // If not found by barcode, try searching by name
                val searchResults = homeRepository.searchProducts(query)
                if (searchResults.isNotEmpty()) {
                    // Take the first matching product
                    _priceCheckProduct.value = searchResults[0]
                } else {
                    _priceCheckProduct.value = null
                    _homeUiEvent.emit(HomeUiEvent.ShowError("Product not found"))
                }
            } catch (e: Exception) {
                _priceCheckProduct.value = null
                _homeUiEvent.emit(HomeUiEvent.ShowError("Error searching product: ${e.message}"))
            }
        }
    }

    fun clearPriceCheckProduct() {
        _priceCheckProduct.value = null
    }

    private val _pluSearchResult = MutableStateFlow<Products?>(null)
    val pluSearchResult: StateFlow<Products?> = _pluSearchResult.asStateFlow()

    private val _isSearchingPLU = MutableStateFlow(false)
    val isSearchingPLU: StateFlow<Boolean> = _isSearchingPLU.asStateFlow()

    fun searchProductByPLU(plu: String) {
        viewModelScope.launch {
            if (plu.length != 4) {
                _pluSearchResult.value = null
                return@launch
            }

            _isSearchingPLU.value = true
            _pluSearchResult.value = null

            try {
                val storeId = preferenceManager.getStoreID()
                val locationId = preferenceManager.getOccupiedLocationID()

                val result = homeRepository.searchProductByPLU(plu, storeId, locationId)
                result.onSuccess { product ->
                    _pluSearchResult.value = product
                    _isSearchingPLU.value = false
                    if (product == null) {
                        _homeUiEvent.emit(HomeUiEvent.ShowError("No product found for PLU: $plu"))
                    }
                }.onFailure { error ->
                    _isSearchingPLU.value = false
                    _homeUiEvent.emit(HomeUiEvent.ShowError("Error searching PLU: ${error.message}"))
                }
            } catch (e: Exception) {
                _isSearchingPLU.value = false
                _homeUiEvent.emit(HomeUiEvent.ShowError("Error searching PLU: ${e.message}"))
            }
        }
    }

    fun clearPLUSearchResult() {
        _pluSearchResult.value = null
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
            val originalChargeTax = product.chargeTaxOnThisProduct ?: true
            val newChargeTax = if (_isTaxExempt.value) {
                // Store original value for restoration when tax is applied again
                val key = "${product.id}_null"
                if (!originalTaxStatusMap.containsKey(key)) {
                    originalTaxStatusMap[key] = originalChargeTax
                }
                false  // If tax is exempt, set to false
            } else {
                originalChargeTax  // Otherwise use product's original value
            }
            cartItemList.add(
                CartItem(
                    productId = product.id,
                    name = product.name,
                    quantity = 1,
                    imageUrl = product.images?.firstOrNull()?.fileURL,
                    productVariantId = null,
                    cardPrice = product.cardPrice.toDouble(),
                    cashPrice = product.cashPrice.toDouble(),
                    chargeTaxOnThisProduct = newChargeTax,
                    selectedPrice = product.cardPrice.toDouble(),  // Always add new products at card price
                    productTaxDetails = product.taxDetails,  // Include product tax details
                    cardTax = product.cardTax.toDouble(),
                    cashTax = product.cashTax.toDouble()
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

            val originalChargeTax = product.chargeTaxOnThisProduct ?: true
            val newChargeTax = if (_isTaxExempt.value) {
                // Store original value for restoration when tax is applied again
                val key = "${product.id}_${variant.id}"
                if (!originalTaxStatusMap.containsKey(key)) {
                    originalTaxStatusMap[key] = originalChargeTax
                }
                false  // If tax is exempt, set to false
            } else {
                originalChargeTax  // Otherwise use product's original value
            }

            // Calculate tax dynamically based on variant's actual prices
            // Get tax details from Room database
            val locationId = preferenceManager.getOccupiedLocationID()
            val taxDetails = runBlocking(Dispatchers.IO) {
                try {
                    verifyPinRepository.getTaxDetailsByLocationId(locationId)
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error getting tax details: ${e.message}")
                    emptyList()
                }
            }

            // Calculate tax based on variant's actual prices (not product's base price)
            val taxResult = dynamicTaxCalculationUseCase.calculateTax(
                cardPrice = variantCardPrice,
                cashPrice = variantCashPrice,
                taxDetails = taxDetails,
                chargeTaxOnThisProduct = newChargeTax
            )

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
                    chargeTaxOnThisProduct = newChargeTax,
                    selectedPrice = variantCardPrice,  // Always add new products at card price
                    productTaxDetails = variant.taxDetails
                        ?: product.taxDetails,  // Prefer variant tax details, fallback to product
                    cardTax = taxResult.cardTax,  // Calculate tax based on variant's actual price
                    cashTax = taxResult.cashTax  // Calculate tax based on variant's actual price
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
        viewModelScope.launch {
            val currentList = _cartItems.value.toMutableList()
            // Find index based on both productId and variantId
            val index = currentList.indexOfFirst { item ->
                item.productId == updatedItem.productId && item.productVariantId == updatedItem.productVariantId
            }
            if (index >= 0) {
                if (updatedItem.quantity <= 0) {
                    currentList.removeAt(index)
                } else {
                    // Calculate discounted prices
                    val discountedCardPrice = calculateDiscountedPrice(
                        basePrice = updatedItem.cardPrice,
                        discountType = updatedItem.discountType,
                        discountValue = updatedItem.discountValue
                    )
                    val discountedCashPrice = calculateDiscountedPrice(
                        basePrice = updatedItem.cashPrice,
                        discountType = updatedItem.discountType,
                        discountValue = updatedItem.discountValue
                    )

                    // Get tax details from Room database
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val taxDetails = try {
                        verifyPinRepository.getTaxDetailsByLocationId(locationId)
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error getting tax details: ${e.message}")
                        emptyList()
                    }

                    // Recalculate tax dynamically based on discounted prices
                    val taxResult = dynamicTaxCalculationUseCase.calculateTax(
                        cardPrice = discountedCardPrice,
                        cashPrice = discountedCashPrice,
                        taxDetails = taxDetails,
                        chargeTaxOnThisProduct = updatedItem.chargeTaxOnThisProduct
                    )

                    // Update cart item with recalculated tax
                    val itemWithRecalculatedTax = updatedItem.copy(
                        cardTax = taxResult.cardTax,
                        cashTax = taxResult.cashTax
                    )
                    currentList[index] = itemWithRecalculatedTax
                }
            }
            _cartItems.value = currentList
            if (currentList.isEmpty()) {
                isCashSelected = false  // Set default to card when cart becomes empty
                resetOrderDiscountValues()  // Reset order discount values when cart becomes empty
            }
            calculateSubtotal(currentList)
        }
    }

    /**
     * Calculate discounted price based on discount type and value
     */
    private fun calculateDiscountedPrice(
        basePrice: Double,
        discountType: DiscountType?,
        discountValue: Double?
    ): Double {
        if (discountType == null || discountValue == null || discountValue <= 0.0) {
            return basePrice
        }

        return when (discountType) {
            DiscountType.PERCENTAGE -> {
                basePrice - ((basePrice * discountValue) / 100.0)
            }

            DiscountType.AMOUNT -> {
                basePrice - discountValue
            }
        }
    }

    fun toggleTaxExempt() {
        val newValue = !_isTaxExempt.value
        Log.d("HomeViewModel", "=== TAX EXEMPT TOGGLE ===")
        Log.d("HomeViewModel", "Previous state: ${_isTaxExempt.value}")
        Log.d("HomeViewModel", "New state: $newValue")

        val cartItemList = _cartItems.value.toMutableList()

        if (newValue) {
            // Exempting tax: Store original values and set all items to not charge tax
            cartItemList.forEachIndexed { index, cartItem ->
                // Create unique key for each cart item (productId + variantId if exists)
                val key = if (cartItem.productVariantId != null) {
                    "${cartItem.productId}_${cartItem.productVariantId}"
                } else {
                    "${cartItem.productId}_null"
                }

                // Store original value if not already stored
                if (!originalTaxStatusMap.containsKey(key)) {
                    originalTaxStatusMap[key] = cartItem.chargeTaxOnThisProduct ?: true
                }

                // Set chargeTaxOnThisProduct to false for all items
                cartItemList[index] = cartItem.copy(chargeTaxOnThisProduct = false)
            }
        } else {
            // Applying tax: Restore original values from the map
            cartItemList.forEachIndexed { index, cartItem ->
                val key = if (cartItem.productVariantId != null) {
                    "${cartItem.productId}_${cartItem.productVariantId}"
                } else {
                    "${cartItem.productId}_null"
                }

                // Restore original value if stored, otherwise default to true
                val originalValue = originalTaxStatusMap[key] ?: true
                cartItemList[index] = cartItem.copy(chargeTaxOnThisProduct = originalValue)
            }

            // Clear the map after restoration
            originalTaxStatusMap.clear()
        }

        _cartItems.value = cartItemList
        _isTaxExempt.value = newValue
        Log.d("HomeViewModel", "Recalculating totals with tax exempt: $newValue")
        // Recalculate totals after toggling tax exempt status
        calculateSubtotal(cartItemList)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        isCashSelected = false  // Set default to card when cart is cleared
        _isTaxExempt.value = false  // Reset tax exempt status when cart is cleared
        originalTaxStatusMap.clear()  // Clear stored original tax status values
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

    fun printLatestOrder() {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowLoading)
            try {
                // Get latest order regardless of sync status
                val latestOrder = getLatestOnlineOrderUseCase()

                if (latestOrder == null) {
                    Log.d("HomeViewModel", "No online orders found to print")
                    _homeUiEvent.emit(HomeUiEvent.ShowError("No online orders found to print."))
                } else {
                    Log.d(
                        "HomeViewModel",
                        "Printing latest online order: ${latestOrder.orderNumber}, Subtotal: ${latestOrder.subTotal}, Tax: ${latestOrder.taxValue}, Total: ${latestOrder.total}"
                    )
                    val statusMessages = mutableListOf<String>()
                    val result = printOrderReceiptUseCase(latestOrder) { statusMessages.add(it) }
                    if (result.isSuccess) {
                        val successMessage =
                            statusMessages.lastOrNull { it.contains("success", ignoreCase = true) }
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
            // Cap discounted price at 0.0 to prevent negative subtotals
            val cardBasedSubtotal = cartItems.sumOf { cartItem ->
                val cardPrice = cartItem.cardPrice
                val discountedCardPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        (cardPrice - ((cardPrice * (cartItem.discountValue ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                    }

                    DiscountType.AMOUNT -> {
                        (cardPrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
                    }

                    else -> cardPrice
                }
                discountedCardPrice * cartItem.quantity
            }

            val cashBasedSubtotal = cartItems.sumOf { cartItem ->
                val cashPrice = cartItem.cashPrice
                val discountedCashPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        (cashPrice - ((cashPrice * (cartItem.discountValue ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                    }

                    DiscountType.AMOUNT -> {
                        (cashPrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
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
            // Cap discounted price at 0.0 to prevent negative subtotals
            val productDiscountedSubtotal = cartItems.sumOf { cartItem ->
                val cardPrice = cartItem.cardPrice
                val discountedPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        (cardPrice - ((cardPrice * (cartItem.discountValue ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                    }

                    DiscountType.AMOUNT -> {
                        (cardPrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
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
            // Cap at 0.0 to prevent negative subtotals
            val finalSubtotal = (discountedSubtotal - currentCashDiscount).coerceAtLeast(0.0)

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
                Log.d(
                    "HomeViewModel",
                    "Location tax details count: ${locationTaxDetails?.size ?: 0}"
                )
                locationTaxDetails?.forEach { tax ->
                    Log.d(
                        "HomeViewModel",
                        "  Store tax: ${tax.title} - ${tax.value}% (isDefault: ${tax.isDefault})"
                    )
                }

                // Convert OrderDiscount to DiscountOrder for PricingCalculationUseCase
                val discountOrder = if (_orderLevelDiscounts.value.isNotEmpty()) {
                    val firstDiscount = _orderLevelDiscounts.value.first()
                    Log.d(
                        "HomeViewModel",
                        "Order discount: ${firstDiscount.type} - ${firstDiscount.value}"
                    )
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
                    Log.d(
                        "HomeViewModel",
                        "Item: ${item.name}, applyTax: ${item.applyTax}, productTaxDetails: ${item.productTaxDetails?.size ?: 0}"
                    )
                    item.productTaxDetails?.forEach { tax ->
                        Log.d(
                            "HomeViewModel",
                            "  Product tax: ${tax.title} - ${tax.value}% (isDefault: ${tax.isDefault})"
                        )
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
                Log.d(
                    "HomeViewModel",
                    "Pricing config - isTaxApplied: true, taxExempt: ${_isTaxExempt.value}, useCardPricing: ${!isCashSelected}"
                )

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
                    Log.d(
                        "HomeViewModel",
                        "    ${item.name}: tax=${item.productTaxAmount}, taxable=${item.productTaxableAmount}"
                    )
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
                    updatedCartItems = cartItems.map {
                        it.copy(
                            productTaxAmount = 0.0,
                            productTaxRate = 0.0,
                            productTaxableAmount = 0.0
                        )
                    }
                } else {
                    // Get location tax rate for fallback calculation
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val location = try {
                        verifyPinRepository.getLocationByLocationID(locationId)
                    } catch (ex: Exception) {
                        null
                    }
                    val taxRate = location?.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    Log.d(
                        "HomeViewModel",
                        "Fallback: Using tax rate: $taxRate (${location?.taxValue ?: 10}%)"
                    )

                    // Rule: Calculate tax directly on the discounted subtotal (after order-level discounts)
                    // discountedSubtotal already has order-level discounts applied
                    // If discounted subtotal is 0, tax must be 0
                    if (discountedSubtotal <= 0) {
                        taxValue = 0.0
                    } else {
                        taxValue = discountedSubtotal * taxRate
                    }
                    Log.d("HomeViewModel", "Fallback tax calculation:")
                    Log.d("HomeViewModel", "  Discounted subtotal: $discountedSubtotal")
                    Log.d("HomeViewModel", "  Tax rate: $taxRate")
                    Log.d("HomeViewModel", "  Calculated tax: $taxValue")

                    // Update cart items with fallback tax (distributed proportionally based on discounted prices)
                    // Calculate total discounted subtotal for proportion calculation
                    val totalDiscountedSubtotal = cartItems.sumOf { item ->
                        if (item.applyTax) {
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
                            itemPrice * (item.quantity ?: 1)
                        } else {
                            0.0
                        }
                    }

                    // Apply order-level discount proportionally to get item-level discounted amounts
                    val discountRatio = if (totalDiscountedSubtotal > 0 && discountedSubtotal > 0) {
                        discountedSubtotal / totalDiscountedSubtotal
                    } else {
                        0.0
                    }

                    updatedCartItems = cartItems.map { item ->
                        if (item.applyTax && discountedSubtotal > 0 && totalDiscountedSubtotal > 0) {
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
                            val itemDiscountedAmount = itemSubtotal * discountRatio
                            val itemTaxAmount = itemDiscountedAmount * taxRate

                            item.copy(
                                productTaxAmount = itemTaxAmount,
                                productTaxRate = taxRate,
                                productTaxableAmount = itemDiscountedAmount
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
            // Cap discounted price at 0.0 to prevent negative subtotals
            val cashBasedProductDiscountedSubtotal = cartItems.sumOf { cartItem ->
                val cashPrice = cartItem.cashPrice
                val discountedCashPrice = when (cartItem.discountType) {
                    DiscountType.PERCENTAGE -> {
                        (cashPrice - ((cashPrice * (cartItem.discountValue ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                    }

                    DiscountType.AMOUNT -> {
                        (cashPrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
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

            // Apply cash discount to cash subtotal (after order-level discounts)
            // Get the current cash discount value
            var currentCashDiscountForCash = _cashDiscountTotal.value
            // Don't apply cash discount if subtotal is already 0 or less
            if (cashDiscountedSubtotal <= 0) {
                currentCashDiscountForCash = 0.0
            }
            // Cap at 0.0 to prevent negative subtotals
            val cashSubtotalAfterAllDiscounts = (cashDiscountedSubtotal - currentCashDiscountForCash).coerceAtLeast(0.0)

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
            // Card doesn't have cash discount, so cardSubtotalAfterAllDiscounts = cardDiscountedSubtotal
            // Cap at 0.0 to prevent negative subtotals
            val cardSubtotalAfterAllDiscounts = cardDiscountedSubtotal.coerceAtLeast(0.0)

            // Calculate tax for cash scenario
            // Rule: Calculate tax directly on the discounted subtotal (after order-level discounts and cash discount)
            // If discounted subtotal is 0, tax must be 0
            val cashTax = if (_isTaxExempt.value) {
                0.0
            } else {
                try {
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val location = verifyPinRepository.getLocationByLocationID(locationId)
                    val taxRate = location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10

                    // Calculate tax directly on the subtotal after all discounts
                    if (cashSubtotalAfterAllDiscounts <= 0) {
                        0.0
                    } else {
                        cashSubtotalAfterAllDiscounts * taxRate
                    }
                } catch (e: Exception) {
                    // Fallback: use same tax rate as calculated for current selection
                    val taxRate = try {
                        val locationId = preferenceManager.getOccupiedLocationID()
                        val location = verifyPinRepository.getLocationByLocationID(locationId)
                        location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    } catch (ex: Exception) {
                        0.10
                    }
                    // Calculate tax directly on discounted subtotal
                    if (cashSubtotalAfterAllDiscounts <= 0) {
                        0.0
                    } else {
                        cashSubtotalAfterAllDiscounts * taxRate
                    }
                }
            }

            // Calculate tax for card scenario
            // Rule: Calculate tax directly on the discounted subtotal (after order-level discounts)
            // If discounted subtotal is 0, tax must be 0
            val cardTax = if (_isTaxExempt.value) {
                0.0
            } else {
                try {
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val location = verifyPinRepository.getLocationByLocationID(locationId)
                    val taxRate = location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10

                    // Calculate tax directly on the subtotal after all discounts
                    if (cardSubtotalAfterAllDiscounts <= 0) {
                        0.0
                    } else {
                        cardSubtotalAfterAllDiscounts * taxRate
                    }
                } catch (e: Exception) {
                    // Fallback: use same tax rate as calculated for current selection
                    val taxRate = try {
                        val locationId = preferenceManager.getOccupiedLocationID()
                        val location = verifyPinRepository.getLocationByLocationID(locationId)
                        location.taxValue?.toDoubleOrNull()?.div(100.0) ?: 0.10
                    } catch (ex: Exception) {
                        0.10
                    }
                    // Calculate tax directly on discounted subtotal
                    if (cardSubtotalAfterAllDiscounts <= 0) {
                        0.0
                    } else {
                        cardSubtotalAfterAllDiscounts * taxRate
                    }
                }
            }

            // Calculate final totals
            val cashTotalAmount = cashDiscountedSubtotal + cashTax
            val cardTotalAmount = cardDiscountedSubtotal + cardTax

            withContext(Dispatchers.Main) {
                _subtotal.value =
                    subtotal.coerceAtLeast(0.0)   // subtotal shows card prices minus product-level discounts only, capped at 0.0
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

    fun saveCustomer(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        birthday: String
    ) {
        viewModelScope.launch {
            try {
                _homeUiEvent.emit(HomeUiEvent.ShowLoading)

                val userId = preferenceManager.getUserID()
                val storeId = preferenceManager.getStoreID()
                val locationId = preferenceManager.getOccupiedLocationID()

                // Save customer to local database (always save locally first)
                val customerId = homeRepository.insertCustomerDetailsIntoLocalDB(
                    userId = userId,
                    storeId = storeId,
                    locationId = locationId,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phoneNumber = phoneNumber,
                    birthday = birthday
                )

                // Save customer ID to preferences for later use in orders
                preferenceManager.setCustomerID(customerId.toInt())

                // Try to sync with server if internet is available
                if (networkMonitor.isNetworkAvailable()) {
                    try {
                        val syncResult = homeRepository.syncCustomerToServer(customerId.toInt())
                        syncResult.onSuccess { response ->
                            _homeUiEvent.emit(HomeUiEvent.HideLoading)
                            _homeUiEvent.emit(HomeUiEvent.ShowSuccess("Customer Added Successfully"))
                        }.onFailure { e ->
                            _homeUiEvent.emit(HomeUiEvent.HideLoading)
                            Log.e("HomeViewModel", "Failed to sync customer: ${e.message}")
                            // Show proper error message from server response
                            val errorMessage = e.message ?: "Customer sync failed"
                            _homeUiEvent.emit(HomeUiEvent.ShowError(errorMessage))
                        }
                    } catch (e: Exception) {
                        _homeUiEvent.emit(HomeUiEvent.HideLoading)
                        Log.e("HomeViewModel", "Error syncing customer: ${e.message}")
                        _homeUiEvent.emit(HomeUiEvent.ShowSuccess("Error syncing customer: ${e.message}"))
                    }
                } else {
                    // No internet - customer saved locally, will sync when internet is restored
                    _homeUiEvent.emit(HomeUiEvent.HideLoading)
                    _homeUiEvent.emit(HomeUiEvent.ShowSuccess("Customer Added Successfully"))
                }
            } catch (e: Exception) {
                _homeUiEvent.emit(HomeUiEvent.HideLoading)
                Log.e("HomeViewModel", "Error saving customer: ${e.message}")
                _homeUiEvent.emit(HomeUiEvent.ShowError("Failed to save customer: ${e.message}"))
            }
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
        // Cap discounted price at 0.0 to prevent negative subtotals
        val productDiscountedSubtotal = cartItems.sumOf { cartItem ->
            val cardPrice = cartItem.cardPrice
            val discountedPrice = when (cartItem.discountType) {
                DiscountType.PERCENTAGE -> {
                    (cardPrice - ((cardPrice * (cartItem.discountValue ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                }

                DiscountType.AMOUNT -> {
                    (cardPrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
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

                // Get active batch from database (only active batch should be used for orders)
                val batch = posSyncRepository.getActiveBatch()
                    ?: throw IllegalStateException("No active batch found. Please open a batch before creating orders.")

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

                // For cash payments, calculate cash-based values using PricingSummaryUseCase
                val cashSummaryResult = if (paymentMethod == "cash") {
                    pricingSummaryUseCase.calculatePricingSummary(
                        cartItems = _cartItems.value,
                        subtotal = finalSubtotal,
                        cashDiscountTotal = finalCashDiscount,
                        orderDiscountTotal = finalOrderDiscount,
                        isCashSelected = true
                    )
                } else {
                    null
                }

                // Calculate tax base subtotal for store-level tax calculation
                // For cash payments, use cash-based subtotal from PricingSummaryUseCase; for card, use card-based subtotal
                val taxBaseSubtotal = if (paymentMethod == "cash") {
                    // Use cash subtotal after all discounts (cashSubtotal - totalCashDiscount) for accurate tax calculation
                    // Tax should be calculated on the subtotal after all discounts are applied
                    cashSummaryResult?.let { it.cashSubtotal - it.totalCashDiscount } ?: run {
                        // Fallback calculation if PricingSummaryUseCase result is not available
                        val cashBaseSubtotal = _cartItems.value.sumOf { cartItem ->
                            val hasProductDiscount =
                                cartItem.discountType != null && cartItem.discountValue != null && cartItem.discountValue!! > 0.0
                            // Cap discounted price at 0.0 to prevent negative subtotals
                            val discountedCashPrice = if (hasProductDiscount) {
                                when (cartItem.discountType) {
                                    DiscountType.PERCENTAGE -> {
                                        (cartItem.cashPrice - ((cartItem.cashPrice * (cartItem.discountValue
                                            ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                                    }

                                    DiscountType.AMOUNT -> {
                                        (cartItem.cashPrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
                                    }

                                    else -> cartItem.cashPrice
                                }
                            } else {
                                cartItem.cashPrice
                            }
                            discountedCashPrice * (cartItem.quantity ?: 1)
                        }
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
                        discountedCashSubtotal - finalCashDiscount
                    }
                } else {
                    // For card payments, use the card-based finalSubtotal
                    finalSubtotal
                }

                // Calculate store-level tax details (needed for cash tax calculation)
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

                // Use cash values if payment is cash, otherwise use card values
                // For cash: subtotal after all discounts, tax from store-level taxes, total = subtotal + tax
                // For card: use existing values
                val orderSubtotal = if (paymentMethod == "cash") {
                    // Use cash subtotal after all discounts (cashSubtotal - totalCashDiscount)
                    cashSummaryResult?.let { it.cashSubtotal - it.totalCashDiscount }
                        ?: finalSubtotal
                } else {
                    finalSubtotal
                }

                val orderTax = if (paymentMethod == "cash" && !_isTaxExempt.value) {
                    // For cash payments, calculate tax from store-level tax details to ensure it matches taxDetails
                    storeTaxDetails.sumOf { it.amount ?: 0.0 }
                } else if (paymentMethod == "cash") {
                    // If tax exempt, use 0
                    0.0
                } else {
                    // For card payments, use the existing tax calculation
                    finalTax
                }

                val orderTotal = if (paymentMethod == "cash") {
                    // Total = subtotal after discounts + tax
                    orderSubtotal + orderTax
                } else {
                    finalTotal
                }

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

                        Log.d(
                            "HomeViewModel",
                            "Tax calculation for item: ${cartItem.name}, Price: $itemPrice, Qty: $itemQuantity, Subtotal: $itemSubtotal, Tax: ${taxDetail.title} (${taxDetail.value}%), Amount: $taxAmount"
                        )

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
                        // Include product-level tax (totalTax = cashTax or cardTax based on payment method)
                        totalTax = if (paymentMethod == "cash") {
                            // For cash payments, use cash tax value
                            val cashTaxValue = cartItem.cashTax * (cartItem.quantity ?: 1)
                            cashTaxValue.takeIf { it > 0.0 }
                        } else {
                            // For card payments, use productTaxAmount (which is calculated based on card price)
                            cartItem.productTaxAmount.takeIf { it > 0.0 }
                        },
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

                Log.d(
                    "HomeViewModel",
                    "Tax calculation base - Payment: $paymentMethod, Card subtotal: $finalSubtotal, Tax base subtotal: $taxBaseSubtotal"
                )

                // Create order request using captured values
                // If tax is exempt, set applyTax = false and taxDetails = emptyList()
                val isTaxExempt = _isTaxExempt.value
                val orderApplyTax = !isTaxExempt
                val orderTaxDetails = if (isTaxExempt) {
                    emptyList()
                } else {
                    storeTaxDetails
                }

                Log.d(
                    "HomeViewModel", "Creating order - Payment: $paymentMethod," +
                            " Subtotal: $orderSubtotal, Tax: $orderTax, Total: $orderTotal"
                )
                Log.d("HomeViewModel", "Tax exempt: $isTaxExempt, ApplyTax: $orderApplyTax")
                if (!isTaxExempt) {
                    Log.d("HomeViewModel", "Tax details count: ${orderTaxDetails.size}")
                    orderTaxDetails.forEach { tax ->
                        Log.d(
                            "HomeViewModel",
                            "  Tax: ${tax.title} - ${tax.value}% - Amount: ${tax.amount}"
                        )
                    }
                } else {
                    Log.d("HomeViewModel", "Tax is exempt - taxDetails will be empty")
                }

                // Check if split payment is enabled and has transactions
                val splitPaymentTransactions = if (_isSplitPaymentEnabled.value && _splitTransactions.value.isNotEmpty()) {
                    // Update invoice numbers for all split transactions
                    _splitTransactions.value.map { transaction ->
                        transaction.copy(invoiceNo = invoiceNo)
                    }
                } else {
                    null
                }

                // Use split payment transactions if available, otherwise use single payment method
                val finalPaymentMethod = if (splitPaymentTransactions != null && splitPaymentTransactions.isNotEmpty()) {
                    // Use first payment method as primary (for backward compatibility)
                    splitPaymentTransactions.first().paymentMethod
                } else {
                    paymentMethod
                }

                val orderRequest = CreateOrderRequest(
                    orderNumber = orderNumber,
                    invoiceNo = invoiceNo,
                    customerId = if (customerId > 0) customerId else null,
                    storeId = storeId,
                    locationId = locationId,
                    storeRegisterId = registerId,
                    batchNo = batch.batchNo,
                    paymentMethod = finalPaymentMethod,  // Use primary payment method from split or single payment
                    isRedeemed = false,
                    source = "point-of-sale",
                    items = orderItems,
                    subTotal = orderSubtotal,  // Use cash subtotal for cash payments, card subtotal for card payments
                    total = orderTotal,  // Use cash total for cash payments, card total for card payments
                    applyTax = orderApplyTax,  // Set to false if tax is exempt
                    taxValue = orderTax,  // Use cash tax for cash payments, card tax for card payments
                    discountAmount = finalOrderDiscount,
                    cashDiscountAmount = finalCashDiscount,
                    rewardDiscount = 0.0,
                    userId = userId,
                    cardDetails = cardDetails,
                    transactions = splitPaymentTransactions,  // Include split payment transactions if available
                    taxDetails = orderTaxDetails,  // Empty list if tax is exempt, otherwise store-level default taxes breakdown
                    taxExempt = isTaxExempt  // Use tax exempt state
                )

                // Always save to local database first (with isSynced = false, status = "pending")
                val orderId = orderRepository.saveOrderToLocal(orderRequest)
                Log.d("Order", "Order saved locally with ID: $orderId")

                // Enqueue order sync command to sync_command table (for offline-first sync)
                try {
                    posSyncRepository.enqueueOrderSyncCommand(
                        orderId = orderNumber,
                        batchId = batch.batchNo
                    )
                    Log.d("Order", "Order sync command enqueued successfully for order: $orderNumber")
                    
                    // Schedule sync worker to process the command
                    scheduleSyncUseCase.scheduleSync(context)
                    Log.d("Order", "Sync worker scheduled")
                } catch (e: Exception) {
                    Log.e("Order", "Failed to enqueue order sync command: ${e.message}", e)
                    // Continue with order processing even if sync command enqueue fails
                    // The order is still saved locally and can be synced later
                }

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
                    val transactionStatus =
                        if (!networkMonitor.isNetworkAvailable() && paymentMethod == "cash") {
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
                    Log.d(
                        "Transaction",
                        "Transaction saved successfully with invoice: $invoiceNo, status: $transactionStatus"
                    )
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
                                Log.d(
                                    "Order",
                                    "Order synced to server successfully. Response: ${response.message}"
                                )
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
                
                // Reset split payment state after order is created
                resetSplitPayment()

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
            // Check if split payment is enabled
            if (_isSplitPaymentEnabled.value) {
                // For split payments, add the card payment and show dialog
                // Order will be created when dialog DONE is clicked
                val remaining = _remainingAmount.value
                val finalTenderAmount = if (remaining > 0.01) remaining else _currentTenderAmount.value
                
                if (finalTenderAmount > 0) {
                    // Add the final card payment to split transactions
                    val finalTransaction = CheckoutSplitPaymentTransactions(
                        invoiceNo = null, // Will be set when order is created
                        paymentMethod = "card",
                        amount = finalTenderAmount,
                        cardDetails = cardDetails,
                        baseAmount = null,
                        taxAmount = null,
                        dualPriceAmount = null
                    )
                    
                    val updatedTransactions = _splitTransactions.value.toMutableList()
                    updatedTransactions.add(finalTransaction)
                    _splitTransactions.value = updatedTransactions
                    
                    // Update remaining amount: remaining = total - sum of all paid amounts
                    val total = _splitPaymentTotal.value
                    val newTotalPaid = updatedTransactions.sumOf { it.amount }
                    val newRemaining = total - newTotalPaid
                    _remainingAmount.value = newRemaining.coerceAtLeast(0.0)
                    
                    // Show success dialog (order will be created when DONE is clicked)
                    _splitPaymentSuccessData.value = Pair(finalTenderAmount, _remainingAmount.value)
                    _showSplitPaymentSuccessDialog.value = true
                    
                    Log.d("SplitPayment", "Card payment added: $finalTenderAmount, Remaining: ${_remainingAmount.value}")
                }
            } else {
                // Regular (non-split) card payment - create order immediately
                _homeUiEvent.emit(
                    HomeUiEvent.ShowSuccess("Transaction Successful!")
                )
                delay(100)
                createOrder("card", cardDetails)
            }
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
            val locationId = preferenceManager.getOccupiedLocationID()
            val location = verifyPinRepository.getLocationByLocationID(locationId)
            val taxValueString = location.taxValue
            taxValueString?.toDoubleOrNull() ?: 10.0
        } catch (e: Exception) {
            // If any error occurs, default to 0.0
            0.0
        }
    }

    /**
     * Check batch status when user lands on home screen
     * Returns true if batch is closed (needs closing dialog), false if active/open, null if error
     */
    suspend fun checkBatchStatus(): Boolean? {
        Log.d("HomeViewModel", "checkBatchStatus() called")
        
        if (!preferenceManager.isLogin() || !preferenceManager.getRegister()) {
            Log.d("HomeViewModel", "Not logged in or no register, skipping batch status check")
            return null // Skip if not logged in or no register
        }

        val batchNo = preferenceManager.getBatchNo()
        if (batchNo.isEmpty()) {
            Log.d("HomeViewModel", "No batch number found, skipping batch status check")
            return null // Skip if no batch number
        }

        val isNetworkAvailable = networkMonitor.isNetworkAvailable()
        Log.d("HomeViewModel", "Network available: $isNetworkAvailable")

        return try {
            val batchReport = if (isNetworkAvailable) {
                // Call API directly to get latest batch status from server
                Log.d("HomeViewModel", "Calling getBatchReport API directly with batchNo: $batchNo")
                apiService.getBatchReport(batchNo)
            } else {
                // No network - get from local database via repository
                Log.d("HomeViewModel", "No network, getting batch report from local database with batchNo: $batchNo")
                batchReportRepository.getBatchReport(batchNo)
            }
            
            val status = batchReport.data.status?.lowercase()
            val isClosed = batchReport.data.closed != null

            Log.d("HomeViewModel", "Batch status response - status: $status, isClosed: $isClosed, source: ${if (isNetworkAvailable) "API" else "Local DB"}")

            when {
                status == "closed" || isClosed -> {
                    // Batch is closed, show closing amount dialog
                    Log.d("HomeViewModel", "Batch is closed, will show closing dialog")
                    true
                }
                else -> {
                    // Batch is active/open, do nothing
                    Log.d("HomeViewModel", "Batch is active/open")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error checking batch status: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Verify register status when user lands on home screen
     * Returns true if register is occupied, false if active (needs batch close), null if error
     */
    suspend fun verifyRegisterStatus(): Boolean? {
        if (!networkMonitor.isNetworkAvailable()) {
            return null // Skip verification if no network
        }

        if (!preferenceManager.isLogin() || !preferenceManager.getRegister()) {
            return null // Skip if not logged in or no register
        }

        return try {
            val storeId = preferenceManager.getStoreID()
            val locationId = preferenceManager.getOccupiedLocationID()
            val storeRegisterId = preferenceManager.getOccupiedRegisterID()

            if (storeId == 0 || locationId == 0 || storeRegisterId == 0) {
                Log.d("HomeViewModel", "Invalid register information")
                return null
            }

            val verifyRequest = VerifyRegisterRequest(
                storeId = storeId,
                locationId = locationId,
                storeRegisterId = storeRegisterId
            )
            val response = storeRegistersRepository.verifyStoreRegister(verifyRequest)
            val status = response.data.status.lowercase()

            Log.d("HomeViewModel", "Register status: $status")

            when (status) {
                "occupied" -> {
                    // Register is occupied, do nothing
                    true
                }
                "active" -> {
                    // Register is active, need to close batch
                    false
                }
                else -> {
                    // Unknown status, do nothing
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error verifying register: ${e.message}", e)
            null
        }
    }

    /**
     * Show closing amount dialog
     */
    fun showClosingAmountDialog() {
        _showClosingAmountDialog.value = true
    }

    /**
     * Hide closing amount dialog
     */
    fun hideClosingAmountDialog() {
        _showClosingAmountDialog.value = false
    }

    /**
     * Show batch closed dialog
     */
    fun showBatchClosedDialog() {
        viewModelScope.launch {
            _homeUiEvent.emit(HomeUiEvent.ShowBatchClosedDialog)
        }
    }

    /**
     * Toggle split payment mode on/off
     * When enabled, initializes split payment with 50:50 default split
     * Note: Should be called from UI thread to access current cardTotal
     */
    fun toggleSplitPayment(totalAmount: Double) {
        val newState = !_isSplitPaymentEnabled.value
        _isSplitPaymentEnabled.value = newState

        if (newState) {
            // Initialize split payment mode
            // Store the original total amount to use consistently (prevents recalculation issues)
            _splitPaymentTotal.value = totalAmount
            _currentTenderAmount.value = totalAmount / 2.0 // Default 50:50 split
            // Calculate remaining amount: remaining = total - tender amount
            _remainingAmount.value = totalAmount - _currentTenderAmount.value
            _splitTransactions.value = emptyList()
            Log.d("SplitPayment", "Split payment enabled. Total: $totalAmount, Initial Tender: ${_currentTenderAmount.value}, Remaining: ${_remainingAmount.value}")
        } else {
            // Reset split payment state
            _splitTransactions.value = emptyList()
            _splitPaymentTotal.value = 0.0
            _remainingAmount.value = 0.0
            _currentTenderAmount.value = 0.0
            _showSplitPaymentSuccessDialog.value = false
            Log.d("SplitPayment", "Split payment disabled. State reset.")
        }
    }

    /**
     * Update current tender amount in split payment mode
     * Automatically calculates remaining amount
     * Remaining = Total - Tender Amount (simplified calculation)
     */
    fun updateSplitPaymentTenderAmount(tenderAmount: Double) {
        if (!_isSplitPaymentEnabled.value) return

        // Use the stored split payment total (not current cardTotal which might change)
        val total = _splitPaymentTotal.value

        // Cap tender amount at total amount
        val cappedTender = tenderAmount.coerceIn(0.0, total)
        _currentTenderAmount.value = cappedTender

        // Calculate remaining amount: remaining = total - tender amount
        val remaining = total - cappedTender
        _remainingAmount.value = remaining.coerceAtLeast(0.0)

        Log.d("SplitPayment", "Tender updated: $cappedTender, Remaining: ${_remainingAmount.value}, Total: $total")
    }

    /**
     * Add a split payment transaction (cash or card)
     * Stores payment locally and updates remaining amount
     * Returns true if payment was added successfully, false if validation failed
     */
    fun addSplitPayment(paymentMethod: String, tenderAmount: Double, cardDetails: CardDetails? = null): Boolean {
        if (!_isSplitPaymentEnabled.value) {
            Log.e("SplitPayment", "Cannot add split payment: Split payment mode is not enabled")
            return false
        }

        // Use the stored split payment total (not current cardTotal which might change)
        val total = _splitPaymentTotal.value
        val totalPaid = _splitTransactions.value.sumOf { it.amount }
        val remaining = total - totalPaid

        // Validate: tender amount must be greater than 0
        if (tenderAmount <= 0) {
            Log.e("SplitPayment", "Tender amount must be greater than 0")
            return false
        }

        // Automatically cap tender amount at remaining amount if it exceeds (no error shown)
        val cappedTenderAmount = tenderAmount.coerceIn(0.0, remaining)
        if (cappedTenderAmount < tenderAmount) {
            Log.d("SplitPayment", "Tender amount ($tenderAmount) capped to remaining amount ($remaining)")
        }

        // Create split payment transaction (use capped amount)
        val transaction = CheckoutSplitPaymentTransactions(
            invoiceNo = null, // Will be set when order is created
            paymentMethod = paymentMethod.lowercase(),
            amount = cappedTenderAmount, // Use capped amount
            cardDetails = cardDetails,
            baseAmount = null,
            taxAmount = null,
            dualPriceAmount = null
        )

        // Add to split transactions list
        val updatedTransactions = _splitTransactions.value.toMutableList()
        updatedTransactions.add(transaction)
        _splitTransactions.value = updatedTransactions

            // Update remaining amount: remaining = total - sum of all paid amounts
            val newTotalPaid = updatedTransactions.sumOf { it.amount }
            val newRemaining = total - newTotalPaid
            _remainingAmount.value = newRemaining.coerceAtLeast(0.0)

            // Don't update tender here - it will be updated when dialog DONE is clicked

        // Show success dialog
        _splitPaymentSuccessData.value = Pair(cappedTenderAmount, _remainingAmount.value)
        _showSplitPaymentSuccessDialog.value = true

        Log.d("SplitPayment", "Payment added: $paymentMethod, Amount: $cappedTenderAmount, Remaining: ${_remainingAmount.value}")

        return true
    }

    /**
     * Dismiss split payment success dialog
     * Called when user clicks DONE on payment success dialog
     * Updates tender amount to remaining amount and sets remaining to 0
     */
    fun dismissSplitPaymentSuccessDialog() {
        _showSplitPaymentSuccessDialog.value = false
        
        // After payment is done, set tender amount to remaining amount and remaining to 0
        val currentRemaining = _remainingAmount.value
        _currentTenderAmount.value = currentRemaining
        _remainingAmount.value = 0.0
        
        Log.d("SplitPayment", "Dialog dismissed. Tender set to: $currentRemaining, Remaining set to: 0.0")
    }

    /**
     * Check if split payment is complete (remaining amount is 0)
     */
    fun isSplitPaymentComplete(): Boolean {
        return _isSplitPaymentEnabled.value && _remainingAmount.value <= 0.01 // Use epsilon for floating point comparison
    }

    /**
     * Reset split payment state (called after order is created or cancelled)
     */
    fun resetSplitPayment() {
        _isSplitPaymentEnabled.value = false
        _splitTransactions.value = emptyList()
        _splitPaymentTotal.value = 0.0
        _remainingAmount.value = 0.0
        _currentTenderAmount.value = 0.0
        _showSplitPaymentSuccessDialog.value = false
        Log.d("SplitPayment", "Split payment state reset")
    }

    /**
     * Close batch with closing cash amount
     */
    suspend fun closeBatch(closingCashAmount: Double?): Result<Unit> {
        return try {
            val batchNo = preferenceManager.getBatchNo()
            if (batchNo.isEmpty()) {
                return Result.failure(Exception("No batch number found"))
            }

            val userId = preferenceManager.getUserID()
            val storeId = preferenceManager.getStoreID()
            val locationId = preferenceManager.getOccupiedLocationID()

            val batchCloseRequest = BatchCloseRequest(
                cashierId = userId,
                closedBy = userId,
                closingCashAmount = closingCashAmount ?: 0.0,
                locationId = locationId,
                orders = emptyList(),
                paxBatchNo = "",
                storeId = storeId
            )

            val result = batchReportRepository.batchClose(batchNo, batchCloseRequest)
            
            result.map { response ->
                Log.d("HomeViewModel", "Batch closed successfully: ${response.message}")
                
                // Close batch in local database using PosSyncRepository
                posSyncRepository.closeBatch(batchNo, closingCashAmount)
                
                // Update preferences
                preferenceManager.setBatchStatus("closed")
                preferenceManager.setRegister(false)
                
                Unit
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error closing batch: ${e.message}", e)
            Result.failure(e)
        }
    }
}
