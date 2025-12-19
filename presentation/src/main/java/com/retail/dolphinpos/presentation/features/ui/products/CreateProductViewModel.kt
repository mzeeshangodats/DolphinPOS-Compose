package com.retail.dolphinpos.presentation.features.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.domain.model.product.CreateProductRequest
import com.retail.dolphinpos.domain.model.product.ProductImageRequest
import com.retail.dolphinpos.domain.model.product.ProductVariantRequest
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.product.VendorItem
import com.retail.dolphinpos.domain.usecases.product.CreateProductUseCase
import com.retail.dolphinpos.domain.usecases.product.GetCategoriesUseCase
import com.retail.dolphinpos.domain.usecases.product.GetVendorsUseCase
import com.retail.dolphinpos.domain.usecases.product.SyncProductUseCase
import com.retail.dolphinpos.domain.usecases.product.UploadFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateProductViewModel @Inject constructor(
    private val createProductUseCase: CreateProductUseCase,
    private val syncProductUseCase: SyncProductUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val getVendorsUseCase: GetVendorsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val preferenceManager: PreferenceManager,
    private val userDao: UserDao
) : ViewModel() {

    private var dualPricePercentage: Double = 2.0 // Default to 2% if not found

    init {
        loadVendors()
        loadCategories()
        loadDualPricePercentage()
    }

    private fun loadDualPricePercentage() {
        viewModelScope.launch {
            try {
                val locationId = preferenceManager.getOccupiedLocationID()
                val location = userDao.getLocationByLocationId(locationId)
                dualPricePercentage = location.dualPricePercentage?.toDoubleOrNull() ?: 2.0
            } catch (e: Exception) {
                // If location not found or error, use default 2%
                dualPricePercentage = 2.0
            }
        }
    }

    // UI State
    private val _uiState = MutableStateFlow(CreateProductUiState())
    val uiState: StateFlow<CreateProductUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CreateProductUiEvent>()
    val uiEvent: SharedFlow<CreateProductUiEvent> = _uiEvent.asSharedFlow()

    fun updateProductName(name: String) {
        _uiState.value = _uiState.value.copy(productName = name)
    }

    fun updateBarcode(barcode: String) {
        _uiState.value = _uiState.value.copy(barcode = barcode)
    }

    fun generateProductBarcode() {
        val storeId = preferenceManager.getStoreID()
        val locationId = preferenceManager.getOccupiedLocationID()
        val barcode = generateBarcode(storeId, locationId, 'T')
        _uiState.value = _uiState.value.copy(barcode = barcode)
    }

    fun generateVariantBarcode(variant: ProductVariantData) {
        val storeId = preferenceManager.getStoreID()
        val locationId = preferenceManager.getOccupiedLocationID()
        val barcode = generateVariantBarcode(storeId, locationId, 'T')
        val updatedVariant = variant.copy(barcode = barcode)
        updateVariant(updatedVariant)
    }

    private fun generateBarcode(
        storeId: Int,
        locationId: Int,
        source: Char // 'A' or 'T'
    ): String {
        val time = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"))

        return "B" +
                storeId.toString().padStart(2, '0') +
                locationId.toString().padStart(2, '0') +
                source +
                time
    }

    private fun generateVariantBarcode(
        storeId: Int,
        locationId: Int,
        source: Char // 'A' or 'T'
    ): String {
        val time = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"))

        return "BV" +
                storeId.toString().padStart(2, '0') +
                locationId.toString().padStart(2, '0') +
                source +
                time
    }

    fun updateAlternateBarcode(alternateBarcode: String) {
        _uiState.value = _uiState.value.copy(alternateBarcode = alternateBarcode)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateQuantity(quantity: String) {
        _uiState.value = _uiState.value.copy(quantity = quantity)
    }

    fun updateTrackQuantity(trackQuantity: Boolean) {
        _uiState.value = _uiState.value.copy(trackQuantity = trackQuantity)
    }

    fun updateContinueSellingWhenOutOfStock(continueSelling: Boolean) {
        _uiState.value = _uiState.value.copy(continueSellingWhenOutOfStock = continueSelling)
    }

    fun updateHasSkuOrBarcode(hasSkuOrBarcode: Boolean) {
        _uiState.value = _uiState.value.copy(hasSkuOrBarcode = hasSkuOrBarcode)
    }

    fun updateProductVendor(vendorId: Int?) {
        val vendorName = _uiState.value.vendors.find { it.id == vendorId }?.title ?: ""
        _uiState.value = _uiState.value.copy(
            productVendorId = vendorId,
            productVendor = vendorName
        )
    }
    
    fun loadVendors() {
        viewModelScope.launch {
            getVendorsUseCase().onSuccess { response ->
                _uiState.value = _uiState.value.copy(vendors = response.data.list)
            }.onFailure { error ->
                _uiEvent.emit(CreateProductUiEvent.ShowError("Failed to load vendors: ${error.message}"))
            }
        }
    }


    fun updateCategory(categoryId: Int?) {
        val categoryName = _uiState.value.categories.find { it.id == categoryId }?.title ?: ""
        _uiState.value = _uiState.value.copy(
            categoryId = categoryId ?: 1,
            categoryName = categoryName
        )
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = getCategoriesUseCase()
                _uiState.value = _uiState.value.copy(categories = categories)
            } catch (e: Exception) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Failed to load categories: ${e.message}"))
            }
        }
    }

    fun updateIsEBTEligible(isEBTEligible: Boolean) {
        _uiState.value = _uiState.value.copy(isEBTEligible = isEBTEligible)
    }

    fun updateIsIDRequired(isIDRequired: Boolean) {
        _uiState.value = _uiState.value.copy(isIDRequired = isIDRequired)
    }

    fun updatePrice(price: String) {
        // Limit price to 8 characters
        val limitedPrice = if (price.length > 8) price.take(8) else price
        val currentState = _uiState.value
        val priceValue = limitedPrice.toDoubleOrNull()
        
        // Calculate Dual Price Card using dualPricePercentage from location table
        val compareAtPrice = if (priceValue != null && priceValue > 0) {
            val multiplier = 1.0 + (dualPricePercentage / 100.0)
            String.format("%.2f", priceValue * multiplier)
        } else {
            ""
        }
        
        // Calculate Profit (Price - Cost Per Item) - calculate if both values are valid
        val costPerItemValue = currentState.costPerItem.toDoubleOrNull()
        val profit = if (priceValue != null && costPerItemValue != null) {
            String.format("%.2f", priceValue - costPerItemValue)
        } else {
            ""
        }
        
        // Calculate Markup % = (Selling Price − Cost Price) ÷ Cost Price × 100
        val markup = if (priceValue != null && costPerItemValue != null && costPerItemValue > 0) {
            val markupValue = ((priceValue - costPerItemValue) / costPerItemValue) * 100
            String.format("%.2f", markupValue)
        } else {
            ""
        }
        
        _uiState.value = currentState.copy(
            price = limitedPrice,
            compareAtPrice = compareAtPrice,
            profit = profit,
            margin = markup
        )
    }

    fun updateCompareAtPrice(compareAtPrice: String) {
        // This field is now read-only, calculated automatically
    }

    fun updateChargeTax(chargeTax: Boolean) {
        _uiState.value = _uiState.value.copy(chargeTaxOnThisProduct = chargeTax)
    }

    fun updateCostPerItem(costPerItem: String) {
        val currentState = _uiState.value
        val costPerItemValue = costPerItem.toDoubleOrNull()
        val priceValue = currentState.price.toDoubleOrNull()
        
        // Calculate Profit (Price - Cost Per Item) - calculate if both values are valid
        val profit = if (priceValue != null && costPerItemValue != null) {
            String.format("%.2f", priceValue - costPerItemValue)
        } else {
            ""
        }
        
        // Calculate Markup % = (Selling Price − Cost Price) ÷ Cost Price × 100
        val markup = if (priceValue != null && costPerItemValue != null && costPerItemValue > 0) {
            val markupValue = ((priceValue - costPerItemValue) / costPerItemValue) * 100
            String.format("%.2f", markupValue)
        } else {
            ""
        }
        
        _uiState.value = currentState.copy(
            costPerItem = costPerItem,
            profit = profit,
            margin = markup
        )
    }

    fun updateProfit(profit: String) {
        // This field is now read-only, calculated automatically
    }

    fun updateMargin(margin: String) {
        // This field is now read-only, calculated automatically
    }

    fun updateCashPrice(cashPrice: String) {
        _uiState.value = _uiState.value.copy(cashPrice = cashPrice)
    }

    fun updateCardPrice(cardPrice: String) {
        _uiState.value = _uiState.value.copy(cardPrice = cardPrice)
    }

//    fun addVariantOption(optionName: String, optionValues: List<String>) {
//        val currentOptions = _uiState.value.variantOptions.toMutableMap()
//        currentOptions[optionName] = optionValues
//        _uiState.value = _uiState.value.copy(variantOptions = currentOptions)
//    }

//    fun removeVariantOption(optionName: String) {
//        val currentOptions = _uiState.value.variantOptions.toMutableMap()
//        currentOptions.remove(optionName)
//        _uiState.value = _uiState.value.copy(variantOptions = currentOptions)
//    }

    // Variant option management
    fun setSelectedVariantType(type: String?) {
        if (type != null) {
            val currentActive = _uiState.value.activeVariantTypes.toMutableSet()
            currentActive.add(type)
            _uiState.value = _uiState.value.copy(
                selectedVariantType = type,
                activeVariantTypes = currentActive
            )
        }
    }

    fun addSizeValue(size: String) {
        if (size.isNotBlank()) {
            val currentSizes = _uiState.value.sizeValues.toMutableList()
            if (!currentSizes.contains(size)) {
                currentSizes.add(size)
                _uiState.value = _uiState.value.copy(sizeValues = currentSizes)
            }
        }
    }

    fun removeSizeValue(size: String) {
        val currentSizes = _uiState.value.sizeValues.toMutableList()
        currentSizes.remove(size)
        _uiState.value = _uiState.value.copy(sizeValues = currentSizes)
    }

    fun addColorValue(color: String) {
        if (color.isNotBlank()) {
            val currentColors = _uiState.value.colorValues.toMutableList()
            if (!currentColors.contains(color)) {
                currentColors.add(color)
                _uiState.value = _uiState.value.copy(colorValues = currentColors)
            }
        }
    }

    fun removeColorValue(color: String) {
        val currentColors = _uiState.value.colorValues.toMutableList()
        currentColors.remove(color)
        _uiState.value = _uiState.value.copy(colorValues = currentColors)
    }

    fun addCustomAttribute(name: String, value: String) {
        if (name.isNotBlank() && value.isNotBlank()) {
            val currentCustoms = _uiState.value.customAttributes.toMutableList()
            val customKey = "$name:$value"
            if (!currentCustoms.any { it.first == name && it.second == value }) {
                currentCustoms.add(Pair(name, value))
                _uiState.value = _uiState.value.copy(customAttributes = currentCustoms)
            }
        }
    }

    fun removeCustomAttribute(name: String, value: String) {
        val currentCustoms = _uiState.value.customAttributes.toMutableList()
        currentCustoms.remove(Pair(name, value))
        _uiState.value = _uiState.value.copy(customAttributes = currentCustoms)
    }

    fun generateVariants() {
        val state = _uiState.value
        val sizes = state.sizeValues
        val colors = state.colorValues
        val customs = state.customAttributes

        // Generate all combinations using cartesian product
        // Start with a list containing one empty combination
        var combinations = listOf<List<String>>(emptyList())
        
        // Multiply by sizes
        if (sizes.isNotEmpty()) {
            combinations = combinations.flatMap { base ->
                sizes.map { size -> base + size }
            }
        }
        
        // Multiply by colors
        if (colors.isNotEmpty()) {
            combinations = combinations.flatMap { base ->
                colors.map { color -> base + color }
            }
        }
        
        // Multiply by custom attributes
        if (customs.isNotEmpty()) {
            combinations = combinations.flatMap { base ->
                customs.map { (name, value) -> base + "$name $value" }
            }
        }

        // If no options selected, create one empty variant
        if (combinations.isEmpty() || (combinations.size == 1 && combinations[0].isEmpty())) {
            combinations = listOf(emptyList())
        }

        // Create variant data for each combination
        val newVariants = combinations.map { combination ->
            val title = if (combination.isEmpty()) {
                "Default"
            } else {
                combination.joinToString(" - ")
            }
            val attributes = mutableMapOf<String, String>()
            
            // Build attributes map
            combination.forEach { item ->
                if (sizes.contains(item)) {
                    attributes["Size"] = item
                } else if (colors.contains(item)) {
                    attributes["Color"] = item
                } else {
                    // Check if it's a custom attribute
                    customs.forEach { (name, value) ->
                        if (item == "$name $value") {
                            attributes[name] = value
                        }
                    }
                }
            }

            val variantPrice = state.price.ifEmpty { "0.0" }
            val priceValue = variantPrice.toDoubleOrNull()
            val calculatedDualPrice = if (priceValue != null && priceValue > 0) {
                val multiplier = 1.0 + (dualPricePercentage / 100.0)
                String.format("%.2f", priceValue * multiplier)
            } else {
                ""
            }
            
            ProductVariantData(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                price = variantPrice,
                costPrice = state.costPerItem.ifEmpty { "0.0" },
                quantity = "0",
                barcode = "",
                sku = "",
                dualPrice = calculatedDualPrice,
                images = null
            )
        }

        // Replace existing variants with generated ones
        _uiState.value = _uiState.value.copy(variants = newVariants)
    }

    private fun generateSku(title: String): String {
        // Generate a simple SKU from title (can be improved)
        val cleaned = title.replace(" - ", "").replace(" ", "").lowercase()
        return "SKU-${cleaned.take(12)}-${System.currentTimeMillis().toString().takeLast(4)}"
    }

    fun addVariant(variant: ProductVariantData) {
        val currentVariants = _uiState.value.variants.toMutableList()
        currentVariants.add(variant)
        _uiState.value = _uiState.value.copy(variants = currentVariants)
    }

    fun removeVariant(variant: ProductVariantData) {
        val currentVariants = _uiState.value.variants.toMutableList()
        currentVariants.remove(variant)
        _uiState.value = _uiState.value.copy(variants = currentVariants)
    }

    fun updateVariant(variant: ProductVariantData) {
        val currentVariants = _uiState.value.variants.toMutableList()
        val index = currentVariants.indexOfFirst { it.id == variant.id }
        if (index >= 0) {
            // Limit price to 8 characters
            val limitedPrice = if (variant.price.length > 8) variant.price.take(8) else variant.price
            
            // Calculate dual price for variant when price changes
            val priceValue = limitedPrice.toDoubleOrNull()
            val calculatedDualPrice = if (priceValue != null && priceValue > 0) {
                val multiplier = 1.0 + (dualPricePercentage / 100.0)
                String.format("%.2f", priceValue * multiplier)
            } else {
                ""
            }
            val updatedVariant = variant.copy(price = limitedPrice, dualPrice = calculatedDualPrice)
            currentVariants[index] = updatedVariant
            _uiState.value = _uiState.value.copy(variants = currentVariants)
        }
    }

    fun uploadVariantImage(variant: ProductVariantData, file: java.io.File, originalName: String) {
        viewModelScope.launch {
            val uploadResult = uploadFileUseCase(listOf(file), "product")
            uploadResult.onSuccess { response ->
                val uploadedImageList = response.data.images.images
                if (uploadedImageList.isNotEmpty()) {
                    val uploadedImage = uploadedImageList[0]
                    val currentImages = variant.images?.toMutableList() ?: mutableListOf()
                    currentImages.add(
                        ProductImageRequest(
                            fileURL = uploadedImage.url,
                            originalName = uploadedImage.originalname ?: originalName
                        )
                    )
                    val updatedVariant = variant.copy(images = currentImages)
                    updateVariant(updatedVariant)
                }
            }.onFailure { error ->
                _uiEvent.emit(CreateProductUiEvent.ShowError("Failed to upload variant image: ${error.message}"))
            }
        }
    }

    fun addProductImage(imageUrl: String, originalName: String) {
        val currentImages = _uiState.value.productImages.toMutableList()
        currentImages.add(ProductImageData(imageUrl, originalName))
        _uiState.value = _uiState.value.copy(productImages = currentImages)
    }

    fun removeProductImage(image: ProductImageData) {
        val currentImages = _uiState.value.productImages.toMutableList()
        currentImages.remove(image)
        _uiState.value = _uiState.value.copy(productImages = currentImages)
    }

    fun createProduct() {
        viewModelScope.launch {
            // Validate required fields
            val state = _uiState.value
            if (state.productName.isBlank()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please enter Product name"))
                return@launch
            }
            if (state.barcode.isBlank()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please enter Barcode"))
                return@launch
            }
            if (state.description.isBlank()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please enter Product Description"))
                return@launch
            }
            if (state.quantity.isBlank()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please enter Quantity"))
                return@launch
            }
            if (state.productVendorId == null) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please select Product Vendor"))
                return@launch
            }
            if (state.categoryName.isBlank() || state.categoryId <= 0) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please select Choose Category"))
                return@launch
            }
            if (state.price.isBlank()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please enter Price"))
                return@launch
            }
            if (state.costPerItem.isBlank()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please enter Cost Per Item"))
                return@launch
            }
            if (state.productImages.isEmpty()) {
                _uiEvent.emit(CreateProductUiEvent.ShowError("Please add at least one product image"))
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Step 1: Upload images first (if any are local files)
                val uploadedImages = mutableListOf<ProductImageData>()
                val localFiles = mutableListOf<Pair<java.io.File, String>>() // File and originalName
                
                // Separate local files from remote URLs
                state.productImages.forEach { imageData ->
                    val isLocalFile = !imageData.url.startsWith("http://") && 
                                     !imageData.url.startsWith("https://")
                    
                    if (isLocalFile) {
                        val file = java.io.File(imageData.url)
                        if (file.exists()) {
                            localFiles.add(Pair(file, imageData.originalName))
                        } else {
                            _uiEvent.emit(CreateProductUiEvent.ShowError("Image file not found: ${imageData.originalName}"))
                        }
                    } else {
                        // Already a remote URL, use as-is
                        uploadedImages.add(imageData)
                    }
                }
                
                // Upload all local files in a single request
                if (localFiles.isNotEmpty()) {
                    val filesToUpload = localFiles.map { it.first }
                    val uploadResult = uploadFileUseCase(filesToUpload, "product")
                    
                    uploadResult.onSuccess { response ->
                        // Parse response - response.data.images.images is a List<FileUploadImage>
                        val uploadedImageList = response.data.images.images
                        
                        // Match uploaded images with their original file names
                        uploadedImageList.forEachIndexed { index, uploadedImage ->
                            val originalName = if (index < localFiles.size) {
                                localFiles[index].second
                            } else {
                                uploadedImage.originalname ?: "image_${System.currentTimeMillis()}.jpg"
                            }
                            
                            uploadedImages.add(
                                ProductImageData(
                                    url = uploadedImage.url,
                                    originalName = originalName
                                )
                            )
                        }
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _uiEvent.emit(CreateProductUiEvent.ShowError("Failed to upload images: ${error.message}"))
                        return@launch
                    }
                }
                
//                // Ensure at least one image (add placeholder if none)
//                val finalImages = if (uploadedImages.isEmpty()) {
//                    listOf(
//                        ProductImageData(
//                            url = "https://dummyimage.com/600x400/cccccc/000000&text=Product",
//                            originalName = "placeholder.jpg"
//                        )
//                    )
//                } else {
//                    uploadedImages
//                }
                
                // Step 2: Update state with uploaded image URLs
                val updatedState = state.copy(productImages = uploadedImages)
                _uiState.value = updatedState
                
                // Validate that we have at least one image after upload
                if (uploadedImages.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _uiEvent.emit(CreateProductUiEvent.ShowError("Please add at least one product image"))
                    return@launch
                }
                
                // Step 3: Create product with uploaded image URLs
                val request = buildCreateProductRequest()
                val result = createProductUseCase(request)
                
                result.onSuccess { productId ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _uiEvent.emit(CreateProductUiEvent.ProductCreated(productId))
                    
                    // Try to sync immediately
                    syncProduct(productId)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _uiEvent.emit(CreateProductUiEvent.ShowError(error.message ?: "Failed to create product"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _uiEvent.emit(CreateProductUiEvent.ShowError(e.message ?: "Failed to create product"))
            }
        }
    }

    private fun syncProduct(productId: Long) {
        viewModelScope.launch {
            syncProductUseCase(productId).onSuccess {
                _uiEvent.emit(CreateProductUiEvent.ProductSynced)
            }.onFailure { error ->
                // Product is saved locally, sync can happen later
                _uiEvent.emit(CreateProductUiEvent.SyncFailed(error.message ?: "Sync failed"))
            }
        }
    }

    private fun buildCreateProductRequest(): CreateProductRequest {
        val state = _uiState.value
        val storeId = preferenceManager.getStoreID()
        val locationId = preferenceManager.getOccupiedLocationID()
        
        // Build variants from variant options
        val variants = state.variants.map { variantData ->
            ProductVariantRequest(
                title = variantData.title,
                price = variantData.price,
                costPrice = variantData.costPrice,
                quantity = variantData.quantity.toIntOrNull() ?: 0,
                barCode = variantData.barcode,
                sku = variantData.sku,
                cashPrice = variantData.price, // Use price as cashPrice
                cardPrice = variantData.dualPrice, // Use dualPrice as cardPrice
                locationId = locationId,
                images = variantData.images
            )
        }
        
        // Build variant options map (varients)
//        val varients = state.variantOptions.mapValues { (_, values) -> values }


        val images =  state.productImages.map {
            ProductImageRequest(it.url, it.originalName)
        }
        
        // Ensure categoryId is a valid integer (default to 1 if not set)
        val categoryId = if (state.categoryId > 0) state.categoryId else 1
        
        return CreateProductRequest(
            name = state.productName,
            description = state.description,
            images = images,
            status = "active",
            price = state.price,
            costPrice = state.costPerItem,
            trackQuantity = state.trackQuantity,
            quantity = state.quantity.toIntOrNull() ?: 0,
            continueSellingWhenOutOfStock = state.continueSellingWhenOutOfStock,
            salesChannel = listOf("point-of-sale"),
            productVendorId = state.productVendorId,
            currentVendorId = null,
            categoryId = categoryId, // Ensure it's always an integer
            storeId = storeId,
            locationId = locationId,
            barCode = state.barcode,
            secondaryBarCodes = if (state.alternateBarcode.isNotEmpty()) 
                listOf(state.alternateBarcode) else emptyList(),
            //varients = varients,
            cashPrice = state.price,
            cardPrice = state.compareAtPrice,
            variants = variants
        )
    }

    fun logout() {
        viewModelScope.launch {
            // Just clear preferences and navigate, no API call
            _uiEvent.emit(CreateProductUiEvent.NavigateToPinCode)
        }
    }
}

data class CreateProductUiState(
    val productName: String = "",
    val barcode: String = "",
    val alternateBarcode: String = "",
    val description: String = "",
    val quantity: String = "",
    val trackQuantity: Boolean = true,
    val continueSellingWhenOutOfStock: Boolean = true,
    val hasSkuOrBarcode: Boolean = true,
    val productVendor: String = "",
    val productVendorId: Int? = null,
    val categoryId: Int = 1,
    val categoryName: String = "",
    val isEBTEligible: Boolean = false,
    val isIDRequired: Boolean = false,
    val price: String = "",
    val compareAtPrice: String = "",
    val chargeTaxOnThisProduct: Boolean = false,
    val costPerItem: String = "",
    val profit: String = "",
    val margin: String = "",
    val cashPrice: String = "",
    val cardPrice: String = "",
//    val variantOptions: Map<String, List<String>> = emptyMap(),
    val variants: List<ProductVariantData> = emptyList(),
    val productImages: List<ProductImageData> = emptyList(),
    val vendors: List<VendorItem> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val isLoading: Boolean = false,
    // Variant option management
    val selectedVariantType: String? = null,
    val activeVariantTypes: Set<String> = emptySet(),
    val sizeValues: List<String> = emptyList(),
    val colorValues: List<String> = emptyList(),
    val customAttributes: List<Pair<String, String>> = emptyList()
)

data class ProductVariantData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val price: String = "",
    val costPrice: String = "",
    val quantity: String = "",
    val barcode: String = "",
    val sku: String = "",
    val dualPrice: String = "",
    val images: List<ProductImageRequest>?=null,
)

data class ProductImageData(
    val url: String,
    val originalName: String
)

sealed class CreateProductUiEvent {
    data class ProductCreated(val productId: Long) : CreateProductUiEvent()
    object ProductSynced : CreateProductUiEvent()
    data class SyncFailed(val message: String) : CreateProductUiEvent()
    data class ShowError(val message: String) : CreateProductUiEvent()
    object NavigateToPinCode : CreateProductUiEvent()

}

