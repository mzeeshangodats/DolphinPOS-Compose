package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.model.product.FileUploadResponse
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import java.io.File
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(files: List<File>, type: String = "product"): Result<FileUploadResponse> {
        return productRepository.uploadFiles(files, type)
    }
}

