# Offline Image Caching Implementation

This document describes the offline image caching feature implemented for the DolphinPOS application.

## Overview

The offline image caching system allows the app to download and store product images locally when online, so they can be displayed even when the device is offline. **The system now stores both the original URL and the local cached path directly in the product and variant image entities.**

## Components

### 1. Updated Product Images Entities
- **ProductImagesEntity**: Now includes `localPath` and `isCached` fields
- **VariantImagesEntity**: Now includes `localPath` and `isCached` fields
- **Purpose**: Store both original URL and local cached path for each image

### 2. CachedImageEntity
- **Location**: `data/src/main/java/com/retail/dolphinpos/data/entities/products/CachedImageEntity.kt`
- **Purpose**: Room entity for storing cached image metadata
- **Fields**:
  - `originalUrl`: The original image URL from the API
  - `localPath`: Local file path where the image is stored
  - `fileName`: Generated filename for the cached image
  - `downloadedAt`: Timestamp when the image was downloaded
  - `fileSize`: Size of the cached image file
  - `isDownloaded`: Boolean flag indicating download success

### 3. ImageDownloadService
- **Location**: `data/src/main/java/com/retail/dolphinpos/data/service/ImageDownloadService.kt`
- **Purpose**: Handles downloading images from URLs and storing them locally
- **Key Methods**:
  - `downloadImage(url: String)`: Downloads image from URL and returns local path
  - `isImageCached(localPath: String)`: Checks if image file exists locally
  - `deleteImage(localPath: String)`: Deletes cached image file
  - `getFileSize(localPath: String)`: Gets file size of cached image

### 4. Updated Repository Methods
- **Location**: `data/src/main/java/com/retail/dolphinpos/data/repositories/auth/StoreRegisterRepositoryImpl.kt`
- **New Methods**:
  - `downloadAndCacheImages(imageUrls: List<String>)`: Downloads multiple images in parallel and updates product/variant entities
  - `getCachedImagePath(imageUrl: String)`: Returns local path if image is cached
  - `clearOldCachedImages()`: Removes images older than 7 days
  - `getProductImagesWithLocalPaths(productId: Int)`: Returns product images with local paths
  - `getVariantImagesWithLocalPaths(variantId: Int)`: Returns variant images with local paths

### 5. Updated HomeRepository
- **Location**: `data/src/main/java/com/retail/dolphinpos/data/repositories/home/HomeRepositoryImpl.kt`
- **Changes**: 
  - Now uses `StoreRegistersRepository` to get cached image paths
  - `getProductsByCategoryID()` and `searchProducts()` now return products with cached image paths
  - Images are loaded with local paths instead of empty lists

### 6. Database Updates
- **Location**: `data/src/main/java/com/retail/dolphinpos/data/room/DolphinDatabase.kt`
- **Changes**: Added `CachedImageEntity` to database entities and incremented version to 3

### 7. DAO Updates
- **Location**: `data/src/main/java/com/retail/dolphinpos/data/dao/ProductsDao.kt`
- **New Methods**:
  - `insertCachedImage()`: Insert cached image metadata
  - `getCachedImage()`: Retrieve cached image by URL
  - `getCachedImages()`: Retrieve multiple cached images
  - `deleteCachedImage()`: Delete specific cached image
  - `deleteOldCachedImages()`: Delete images older than specified timestamp
  - `updateProductImageLocalPath()`: Update product image with local path
  - `updateVariantImageLocalPath()`: Update variant image with local path
  - `getProductImagesByProductId()`: Get product images by product ID
  - `getVariantImagesByVariantId()`: Get variant images by variant ID

## Usage

### Automatic Image Downloading and Storage
When `getProducts()` is called in `SelectRegisterViewModel`, the system:
1. Collects all image URLs from products and variants
2. Downloads images in parallel using `downloadAndCacheImages()`
3. Stores metadata in Room database
4. **Updates product and variant image entities with local paths**
5. Clears old cached images (older than 7 days)

### Displaying Images with Local Paths
Use the `ImageCacheHelper` utility class:
```kotlin
// Get product images with local paths (if cached)
val productImages = imageCacheHelper.getProductImagesWithLocalPaths(productId)

// Get variant images with local paths (if cached)
val variantImages = imageCacheHelper.getVariantImagesWithLocalPaths(variantId)

// Get the appropriate image path (cached or original URL)
val imagePath = imageCacheHelper.getImagePath(imageUrl)

// Check if image is cached
val isCached = imageCacheHelper.isImageCached(imageUrl)
```

## Storage Management

- Images are stored in the app's internal storage under `cached_images/` directory
- **Local paths are stored directly in product and variant image entities**
- Automatic cleanup removes images older than 7 days
- File naming uses URL hash to prevent conflicts
- Images are downloaded in parallel for better performance

## Error Handling

- Download failures don't interrupt the main product loading process
- Missing cached images fall back to original URLs
- Corrupted cache entries are automatically cleaned up
- Database operations are wrapped in try-catch blocks

## Benefits

1. **Offline Support**: Images display even when offline using stored local paths
2. **Performance**: Faster image loading from local storage
3. **Bandwidth Savings**: Images downloaded only once
4. **Automatic Management**: Old images are automatically cleaned up
5. **Parallel Downloads**: Efficient downloading of multiple images
6. **Direct Access**: Local paths stored directly in product entities for easy access
7. **Fallback Support**: Automatic fallback to original URLs if local path is unavailable

## Database Migration

The database version has been incremented from 2 to 3 to include the new fields in `ProductImagesEntity` and `VariantImagesEntity`. Room will handle the migration automatically.

## Key Features

- **Dual Storage**: Both original URLs and local paths are stored
- **Automatic Updates**: Product/variant entities are updated with local paths after download
- **Easy Access**: Direct methods to get images with local paths
- **Backward Compatibility**: Falls back to original URLs if local paths are not available
