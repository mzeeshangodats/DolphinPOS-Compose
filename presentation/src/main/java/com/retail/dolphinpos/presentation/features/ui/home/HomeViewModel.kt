package com.retail.dolphinpos.presentation.features.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.cart.DiscountType
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountedPrice
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.customer.Customer
import com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount
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
    private val homeRepository: HomeRepository, private val preferenceManager: PreferenceManager
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

    init {
        loadCategories()
        loadMenus()
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
                    _homeUiEvent.emit(HomeUiEvent.PopulateProductsList(response))
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

    fun addToCart(product: Products) {
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
                    selectedPrice = if (isCashSelected) product.cashPrice.toDouble() else product.cardPrice.toDouble()
                )
            )
        }
        _cartItems.value = cartItemList
        calculateSubtotal(cartItemList)
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

    fun removeFromCart(productId: Int) {
        val updatedCart = _cartItems.value.filter { it.productId != productId }
        _cartItems.value = updatedCart
        calculateSubtotal(updatedCart)
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
        calculateSubtotal(currentList)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        calculateSubtotal(emptyList())
    }

    private fun calculateCashDiscount(cartItems: List<CartItem>) {
        if (isCashSelected) {
            val diffTotal = cartItems.sumOf { (it.cardPrice - it.cashPrice) * it.quantity }
            _cashDiscountTotal.value = diffTotal
        } else {
            _cashDiscountTotal.value = 0.0
        }
    }

    fun setOrderLevelDiscounts(discounts: List<OrderDiscount>) {
        _orderLevelDiscounts.value = discounts
    }

    private fun calculateSubtotal(cartItems: List<CartItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1️⃣ Base subtotal
            val baseSubtotal = cartItems.sumOf { it.getProductDiscountedPrice() * it.quantity }

            // 2️⃣ Apply all order-level discounts sequentially
            var discountedSubtotal = baseSubtotal
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

            // 3️⃣ Tax (only for taxable products, proportional to discount)
            val taxableBase = cartItems.sumOf { cart ->
                if (cart.chargeTaxOnThisProduct!!) cart.getProductDiscountedPrice() * cart.quantity else 0.0
            }

            val taxableAfterOrderDiscounts = if (baseSubtotal > 0) {
                taxableBase * (discountedSubtotal / baseSubtotal)
            } else 0.0

            val taxValue = taxableAfterOrderDiscounts * 8.25 / 100.0

            // 4️⃣ Final total
            val totalAmount = discountedSubtotal + taxValue

            withContext(Dispatchers.Main) {
                _subtotal.value = baseSubtotal   // subtotal always before order-level discount
                _orderDiscountTotal.value = totalOrderDiscount
                _tax.value = taxValue
                _totalAmount.value = totalAmount
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
            homeRepository.insertCustomerDetailsIntoLocalDB(customer)
        }
    }

    fun appendDigitToAmount(currentAmount: Double, digit: String): Double {
        val currentCents = (BigDecimal.valueOf(currentAmount) * BigDecimal(100)).toBigInteger()
        val updatedCents = (currentCents.toString() + digit).toBigIntegerOrNull() ?: currentCents
        return BigDecimal(updatedCents).divide(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun removeLastDigit(amount: Double): Double {
        val cents = (amount * 100).toLong().toString()
        val newCents = if (cents.length > 1) cents.dropLast(1).toLong() else 0L
        return newCents / 100.0
    }

    fun formatAmount(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}
