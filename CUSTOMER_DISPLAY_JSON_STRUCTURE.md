# Customer Display JSON Structure

## JSON Format

The POS device sends JSON data with the following structure:

```json
{
    "status": "CHECKOUT_SCREEN",
    "cartItems": [...],
    "subtotal": 10.00,
    "tax": 0.80,
    "total": 10.80,
    "cashDiscountTotal": 0.50,
    "orderDiscountTotal": 1.00,
    "isCashSelected": false,
    "timestamp": 1234567890
}
```

## Status Values

The `status` field determines which screen to display on the customer display:

- **`"WELCOME"`** - Display welcome screen (when cart is empty or waiting)
- **`"CHECKOUT_SCREEN"`** - Display checkout screen with cart items and totals

## Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Screen status: "WELCOME" or "CHECKOUT_SCREEN" |
| `cartItems` | Array | List of cart items (empty array for WELCOME status) |
| `subtotal` | Number | Cart subtotal amount (before discounts) |
| `tax` | Number | Tax amount |
| `total` | Number | Total amount (subtotal - discounts + tax) |
| `cashDiscountTotal` | Number | Total cash discount applied |
| `orderDiscountTotal` | Number | Total order-level discount applied |
| `isCashSelected` | Boolean | Whether cash payment is selected (optional) |
| `timestamp` | Number | Timestamp in milliseconds (optional) |

## Cart Item Structure

Each item in `cartItems` array:

```json
{
    "productId": 123,
    "productVariantId": 456,
    "name": "Product Name - Variant",
    "cardPrice": 10.00,
    "cashPrice": 9.50,
    "selectedPrice": 10.00,
    "quantity": 2,
    "imageUrl": "https://...",
    "barCode": "123456789",
    "sku": "SKU123"
}
```

## Status Logic

The POS automatically determines the status:

- **Cart is empty** → `status: "WELCOME"`
- **Cart has items** → `status: "CHECKOUT_SCREEN"`

## Example JSON Messages

### Welcome Screen (Empty Cart)
```json
{
    "status": "WELCOME",
    "cartItems": [],
    "subtotal": 0.00,
    "tax": 0.00,
    "total": 0.00,
    "cashDiscountTotal": 0.00,
    "orderDiscountTotal": 0.00,
    "isCashSelected": false,
    "timestamp": 1234567890
}
```

### Checkout Screen (Cart with Items)
```json
{
    "status": "CHECKOUT_SCREEN",
    "cartItems": [
        {
            "productId": 1,
            "name": "Product 1",
            "selectedPrice": 10.00,
            "quantity": 2,
            "cardPrice": 10.00,
            "cashPrice": 9.50
        }
    ],
    "subtotal": 20.00,
    "tax": 2.00,
    "total": 22.00,
    "cashDiscountTotal": 1.00,
    "orderDiscountTotal": 2.00,
    "isCashSelected": false,
    "timestamp": 1234567890
}
```

## Implementation Notes

1. **Automatic Status Switching**: The customer display app automatically switches screens based on the `status` field
2. **Real-time Updates**: Status changes are sent immediately when cart state changes
3. **Initial Connection**: New connections receive a WELCOME message if no cart data exists
4. **Empty Cart**: When cart is cleared, status automatically changes to "WELCOME"

## For Dolphin-CFD App

When implementing the CFD app, check the `status` field to determine which screen to show:

```kotlin
when (cartData.status) {
    "WELCOME" -> WelcomeScreen()
    "CHECKOUT_SCREEN" -> CheckoutScreen(cartData)
    else -> DefaultScreen()
}
```

