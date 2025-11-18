# Tax Breakdown Implementation Guide

## Overview
This document explains how the tax breakdown system works with product-level tax calculation in the DolphinPOS system.

## How It Works

### 1. Tax Calculation Flow

#### Step 1: Product-Level Tax Calculation (`PricingCalculationUseCase`)
When items are added to the cart, `PricingCalculationUseCase` calculates taxes for each item:

```kotlin
// For each CartItem:
1. Calculate taxable amount (after all discounts applied proportionally)
2. Apply store-level taxes (default taxes from location)
3. Apply product-level taxes (non-default taxes from product/variant)
4. Store results in CartItem:
   - productTaxAmount: Total tax for this item
   - productTaxRate: Combined tax rate
   - productTaxableAmount: Taxable amount after discounts
   - productTaxDetails: List of all taxes applied (store + product)
```

**Example:**
- Item: "Product A" - Price: $24.99, Quantity: 2
- Store taxes (default): 
  - ILLINOIS STATE TAX: 6.25%
  - COOK COUNTY TAX: 1.75%
- Product taxes (non-default):
  - REGIONAL TRANSPORT AUTHORITY (RTA): 1.0%
  - SCHAUMBURG CITY TAX: 1.0%

**Calculation:**
- Item subtotal: $24.99 × 2 = $49.98
- After discounts: $49.98 (assuming no discounts)
- Store taxes: $49.98 × (6.25% + 1.75%) = $3.998
- Product taxes: $49.98 × (1.0% + 1.0%) = $0.9996
- **Total tax for this item: $4.9976** → stored in `productTaxAmount`
- **Tax details stored in `productTaxDetails`** with all 4 taxes

### 2. Order Creation Flow

#### Step 2: Mapping CartItem → CheckOutOrderItem (`HomeViewModel.createOrder()`)
When creating an order, cart items are converted to order items:

```kotlin
CheckOutOrderItem(
    productId = cartItem.productId,
    quantity = cartItem.quantity,
    price = selectedPrice,
    // ... other fields ...
    totalTax = cartItem.productTaxAmount,  // Total tax amount for this item
    appliedTaxes = cartItem.productTaxDetails  // Full tax breakdown
)
```

**Example:**
```kotlin
// CartItem has:
productTaxAmount = 4.9976
productTaxDetails = [
    TaxDetail(title="ILLINOIS STATE TAX", value=6.25, type="Percentage", isDefault=true, amount=3.12375),
    TaxDetail(title="COOK COUNTY TAX", value=1.75, type="Percentage", isDefault=true, amount=0.87465),
    TaxDetail(title="REGIONAL TRANSPORT AUTHORITY (RTA)", value=1.0, type="Percentage", isDefault=false, amount=0.4998),
    TaxDetail(title="SCHAUMBURG CITY TAX", value=1.0, type="Percentage", isDefault=false, amount=0.4998)
]

// Maps to CheckOutOrderItem:
totalTax = 4.9976
appliedTaxes = [same 4 TaxDetail objects]
```

#### Step 3: Store-Level Tax Breakdown (`HomeViewModel.createOrder()`)
Store-level default taxes are calculated for the entire order:

```kotlin
// Calculate store-level tax details (default taxes only) for order-level breakdown
val storeTaxDetails = location.taxDetails
    ?.filter { it.isDefault == true }
    ?.map { taxDetail ->
        val taxAmount = when (taxDetail.type?.lowercase()) {
            "percentage" -> finalSubtotal * (taxDetail.value / 100.0)
            "fixed amount" -> taxDetail.value
            else -> finalSubtotal * (taxDetail.value / 100.0)
        }
        TaxDetail(
            type = taxDetail.type,
            title = taxDetail.title,
            value = taxDetail.value,
            amount = taxAmount,  // Calculated based on final subtotal
            isDefault = true
        )
    }
```

**Example:**
- Final subtotal (after all discounts): $100.00
- Store default taxes:
  - ILLINOIS STATE TAX (6.25%): $100.00 × 6.25% = $6.25
  - COOK COUNTY TAX (1.75%): $100.00 × 1.75% = $1.75
- Stored in `CreateOrderRequest.taxDetails`

#### Step 4: Save to Database (`PendingOrderRepositoryImpl`)
Order is saved with tax breakdown:

```kotlin
PendingOrderEntity(
    // ... other fields ...
    taxDetails = gson.toJson(storeTaxDetails),  // JSON string of store-level taxes
    taxExempt = false
)
```

### 3. Receipt Generation Flow

#### Step 5: Receipt Tax Breakdown (`GenerateReceiptTextUseCase`)
Receipt generation aggregates and displays all taxes:

```kotlin
// 1. Get store-level taxes from order.taxDetails
order.taxDetails?.filter { it.isDefault == true }?.forEach { taxDetail ->
    taxMap[taxDescription] = taxAmount
}

// 2. Get product-level taxes from orderItems.appliedTaxes
orderItems.forEach { orderItem ->
    orderItem.appliedTaxes?.forEach { productTaxDetail ->
        taxMap[productTaxDescription] += productTaxAmount
    }
}

// 3. Display aggregated taxes
taxMap.forEach { (description, amount) ->
    append("$description: $amount\n")
}
```

**Example Receipt Output:**
```
TAX BREAKDOWN:
ILLINOIS STATE TAX (6.25%):              $6.25
COOK COUNTY TAX (1.75%):                $1.75
REGIONAL TRANSPORT AUTHORITY (RTA) (1.0%): $0.50
SCHAUMBURG CITY TAX (1.0%):              $0.50
```

## Complete Example Scenario

### Scenario: Order with 2 Items

**Item 1: "Product A"**
- Price: $24.99, Quantity: 2
- Subtotal: $49.98
- Store taxes: ILLINOIS STATE TAX (6.25%), COOK COUNTY TAX (1.75%)
- Product taxes: RTA (1.0%), SCHAUMBURG CITY TAX (1.0%)
- Item tax: $4.9976

**Item 2: "Product B"**
- Price: $50.00, Quantity: 1
- Subtotal: $50.00
- Store taxes: ILLINOIS STATE TAX (6.25%), COOK COUNTY TAX (1.75%)
- No product taxes
- Item tax: $4.00

**Order Totals:**
- Subtotal: $99.98
- Total tax: $8.9976
- Total: $108.9776

**CartItem Structure (for Item 1):**
```kotlin
CartItem(
    productTaxAmount = 4.9976,
    productTaxDetails = [
        TaxDetail("ILLINOIS STATE TAX", 6.25, "Percentage", amount=3.12375, isDefault=true),
        TaxDetail("COOK COUNTY TAX", 1.75, "Percentage", amount=0.87465, isDefault=true),
        TaxDetail("REGIONAL TRANSPORT AUTHORITY (RTA)", 1.0, "Percentage", amount=0.4998, isDefault=false),
        TaxDetail("SCHAUMBURG CITY TAX", 1.0, "Percentage", amount=0.4998, isDefault=false)
    ]
)
```

**CheckOutOrderItem Structure (for Item 1):**
```kotlin
CheckOutOrderItem(
    totalTax = 4.9976,
    appliedTaxes = [same 4 TaxDetail objects as above]
)
```

**CreateOrderRequest Structure:**
```kotlin
CreateOrderRequest(
    items = [
        CheckOutOrderItem(totalTax=4.9976, appliedTaxes=[...]),
        CheckOutOrderItem(totalTax=4.00, appliedTaxes=[...])
    ],
    taxValue = 8.9976,  // Sum of all items[].totalTax
    taxDetails = [
        TaxDetail("ILLINOIS STATE TAX", 6.25, amount=6.24875, isDefault=true),
        TaxDetail("COOK COUNTY TAX", 1.75, amount=1.74965, isDefault=true)
    ],
    taxExempt = false
)
```

**PendingOrder Structure:**
```kotlin
PendingOrder(
    items = [same CheckOutOrderItems],
    taxValue = 8.9976,
    taxDetails = [same store-level taxes],
    taxExempt = false
)
```

**Receipt Output:**
```
SUBTOTAL:                          $99.98

TAX BREAKDOWN:
ILLINOIS STATE TAX (6.25%):         $6.25
COOK COUNTY TAX (1.75%):            $1.75
REGIONAL TRANSPORT AUTHORITY (RTA) (1.0%): $0.50
SCHAUMBURG CITY TAX (1.0%):         $0.50

TOTAL:                             $108.98
```

## Key Points

1. **Product-Level Tax Calculation**: Each item's tax is calculated individually based on its taxable amount (after discounts)

2. **Tax Breakdown Storage**:
   - `items[].totalTax`: Total tax amount per item
   - `items[].appliedTaxes`: Full tax breakdown per item (store + product taxes)
   - `taxDetails`: Store-level default taxes breakdown (order-level)

3. **Receipt Generation**: Aggregates all taxes from both sources and displays them together

4. **Database Storage**: Tax details are stored as JSON strings in the database

5. **Backward Compatibility**: All new fields are nullable/optional, so existing orders without tax breakdown will still work

## API Request Structure

The final API request matches the structure you provided:

```json
{
    "items": [
        {
            "productId": 42,
            "quantity": 2,
            "price": 24.99,
            "totalTax": 2.59
        }
    ],
    "taxDetails": [
        {
            "amount": 1.561875,
            "isDefault": true,
            "title": "ILLINOIS STATE TAX",
            "type": "Percentage",
            "value": 6.25
        },
        {
            "amount": 0.437325,
            "isDefault": true,
            "title": "COOK COUNTY TAX",
            "type": "Percentage",
            "value": 1.75
        }
    ],
    "taxValue": 8.25,
    "taxExempt": false
}
```

Note: The `appliedTaxes` field in `CheckOutOrderItem` is used internally for receipt generation but may not be sent to the API (depending on API requirements).

