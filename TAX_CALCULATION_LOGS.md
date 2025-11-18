# Tax Calculation Logs Reference

This document shows all the logs generated during tax calculation. All logs use the tag **`"PricingCalculation"`**.

## Log Flow Overview

The tax calculation follows this flow:
1. `calculatePricing()` - Main entry point
2. `calculateProductLevelTax()` - Calculate tax for all items
3. `calculateItemTax()` - Calculate tax for each individual item
4. `calculateComplexTax()` - Apply individual tax rules

---

## 1. Main Entry Point Logs

**Location:** `PricingCalculationUseCase.calculatePricing()`

### Tax Not Applied
```
PricingCalculation: Tax not applied - isTaxApplied: false, taxExempt: true
```

### Tax Applied Successfully
```
PricingCalculation: Total product tax amount: 8.25
```

---

## 2. Product-Level Tax Calculation Logs

**Location:** `calculateProductLevelTax()`

### Start of Calculation
```
PricingCalculation: === calculateProductLevelTax START ===
PricingCalculation: Cart items count: 2
PricingCalculation: TaxRate: 0.10
PricingCalculation: UseCardPricing: true
PricingCalculation: SubtotalAfterDiscounts: 100.00
PricingCalculation: OriginalSubtotal: 100.00
PricingCalculation: TaxDetails: 4
PricingCalculation: TaxExempt: false
```

### Edge Cases
```
PricingCalculation: Edge case: Empty cart - returning empty list
PricingCalculation: Product Gloves is tax-exempt - applyTax: false, storeTaxExempt: false
```

### End of Calculation
```
PricingCalculation: Total product tax amount: 8.25
PricingCalculation: === calculateProductLevelTax END ===
```

---

## 3. Item-Level Tax Calculation Logs

**Location:** `calculateItemTax()`

### Start of Item Calculation
```
PricingCalculation: === calculateItemTax START ===
PricingCalculation: Item: Gloves
PricingCalculation: ItemTaxableAmount: 49.98
PricingCalculation: FallbackTaxRate: 0.10
```

### Product Tax Details
```
PricingCalculation: ProductTaxDetails: 2
PricingCalculation:   - REGIONAL TRANSPORT AUTHORITY (RTA): 1.0% (isDefault: false)
PricingCalculation:   - SCHAUMBURG CITY TAX: 1.0% (isDefault: false)
```

### Edge Cases
```
PricingCalculation: Edge case: Zero taxable amount - returning (0.0, 0.0)
PricingCalculation: Edge case: Product is tax-exempt (applyTax: false) - returning (0.0, 0.0)
```

### Case 1: Product HAS Additional Taxes
```
PricingCalculation: CASE 1: Product HAS additional taxes - applying store taxes (default) + product taxes (non-default)
PricingCalculation: Applying store taxes (default taxes)...
PricingCalculation: Store taxes applied: Amount=3.998, Rate=0.08
PricingCalculation: Applying product taxes (non-default taxes)...
PricingCalculation: Product taxes applied: Amount=0.9996, Rate=0.02
```

### Case 2: Product HAS NO Additional Taxes
```
PricingCalculation: CASE 2: Product HAS NO additional taxes - applying ONLY store taxes (default)
PricingCalculation: Applying store taxes (default taxes) only...
PricingCalculation: Store taxes applied: Amount=4.00, Rate=0.08
```

### Fallback Tax
```
PricingCalculation: No store taxes available, using fallback tax rate...
PricingCalculation: Fallback tax applied: Amount=4.998, Rate=0.10
```

### End of Item Calculation
```
PricingCalculation: Final result: TotalTaxAmount=4.9976, TotalTaxRate=0.10
PricingCalculation: === calculateItemTax END ===
```

---

## 4. Complex Tax Calculation Logs

**Location:** `calculateComplexTax()`

### Start of Complex Tax
```
PricingCalculation: === calculateComplexTax START ===
PricingCalculation: TaxableAmount: 49.98
PricingCalculation: TaxDetails count: 4
PricingCalculation: IsStoreTax: true
```

### Applicable Taxes
```
PricingCalculation: Applicable taxes count: 2
```

### Edge Cases
```
PricingCalculation: Edge case: Zero taxable amount or empty tax details - returning (0.0, 0.0)
PricingCalculation: Edge case: No applicable taxes found - returning (0.0, 0.0)
```

### Processing Each Tax
```
PricingCalculation: Processing tax: ILLINOIS STATE TAX (Percentage, 6.25, isDefault: true)
PricingCalculation: Percentage tax applied: Rate=0.0625, Amount=3.12375

PricingCalculation: Processing tax: COOK COUNTY TAX (Percentage, 1.75, isDefault: true)
PricingCalculation: Percentage tax applied: Rate=0.0175, Amount=0.87465

PricingCalculation: Processing tax: REGIONAL TRANSPORT AUTHORITY (RTA) (Percentage, 1.0, isDefault: false)
PricingCalculation: Percentage tax applied: Rate=0.01, Amount=0.4998

PricingCalculation: Processing tax: SCHAUMBURG CITY TAX (Percentage, 1.0, isDefault: false)
PricingCalculation: Percentage tax applied: Rate=0.01, Amount=0.4998
```

### Fixed Amount Tax
```
PricingCalculation: Processing tax: FIXED TAX (Fixed Amount, 5.0, isDefault: true)
PricingCalculation: Fixed amount tax applied: Amount=5.0
```

### Unknown Type (Defaults to Percentage)
```
PricingCalculation: Processing tax: UNKNOWN TAX (Unknown, 10.0, isDefault: true)
PricingCalculation: Unknown type, defaulting to percentage: Rate=0.10, Amount=4.998
```

### End of Complex Tax
```
PricingCalculation: Final result: TotalTaxAmount=4.9976, TotalTaxRate=0.10
PricingCalculation: === calculateComplexTax END ===
```

---

## Complete Example Log Flow

Here's what you'll see in Logcat for a typical order with 2 items:

```
// Main calculation starts
PricingCalculation: === calculateProductLevelTax START ===
PricingCalculation: Cart items count: 2
PricingCalculation: TaxRate: 0.10
PricingCalculation: UseCardPricing: true
PricingCalculation: SubtotalAfterDiscounts: 100.00
PricingCalculation: OriginalSubtotal: 100.00
PricingCalculation: TaxDetails: 4
PricingCalculation: TaxExempt: false

// Item 1: Gloves (with product taxes)
PricingCalculation: === calculateItemTax START ===
PricingCalculation: Item: Gloves
PricingCalculation: ItemTaxableAmount: 49.98
PricingCalculation: FallbackTaxRate: 0.10
PricingCalculation: ProductTaxDetails: 2
PricingCalculation:   - REGIONAL TRANSPORT AUTHORITY (RTA): 1.0% (isDefault: false)
PricingCalculation:   - SCHAUMBURG CITY TAX: 1.0% (isDefault: false)
PricingCalculation: CASE 1: Product HAS additional taxes - applying store taxes (default) + product taxes (non-default)
PricingCalculation: Applying store taxes (default taxes)...
PricingCalculation: === calculateComplexTax START ===
PricingCalculation: TaxableAmount: 49.98
PricingCalculation: TaxDetails count: 4
PricingCalculation: IsStoreTax: true
PricingCalculation: Applicable taxes count: 2
PricingCalculation: Processing tax: ILLINOIS STATE TAX (Percentage, 6.25, isDefault: true)
PricingCalculation: Percentage tax applied: Rate=0.0625, Amount=3.12375
PricingCalculation: Processing tax: COOK COUNTY TAX (Percentage, 1.75, isDefault: true)
PricingCalculation: Percentage tax applied: Rate=0.0175, Amount=0.87465
PricingCalculation: Final result: TotalTaxAmount=3.9984, TotalTaxRate=0.08
PricingCalculation: === calculateComplexTax END ===
PricingCalculation: Store taxes applied: Amount=3.9984, Rate=0.08
PricingCalculation: Applying product taxes (non-default taxes)...
PricingCalculation: === calculateComplexTax START ===
PricingCalculation: TaxableAmount: 49.98
PricingCalculation: TaxDetails count: 2
PricingCalculation: IsStoreTax: false
PricingCalculation: Applicable taxes count: 2
PricingCalculation: Processing tax: REGIONAL TRANSPORT AUTHORITY (RTA) (Percentage, 1.0, isDefault: false)
PricingCalculation: Percentage tax applied: Rate=0.01, Amount=0.4998
PricingCalculation: Processing tax: SCHAUMBURG CITY TAX (Percentage, 1.0, isDefault: false)
PricingCalculation: Percentage tax applied: Rate=0.01, Amount=0.4998
PricingCalculation: Final result: TotalTaxAmount=0.9996, TotalTaxRate=0.02
PricingCalculation: === calculateComplexTax END ===
PricingCalculation: Product taxes applied: Amount=0.9996, Rate=0.02
PricingCalculation: Final result: TotalTaxAmount=4.998, TotalTaxRate=0.10
PricingCalculation: === calculateItemTax END ===

// Item 2: Product B (no product taxes)
PricingCalculation: === calculateItemTax START ===
PricingCalculation: Item: Product B
PricingCalculation: ItemTaxableAmount: 50.00
PricingCalculation: FallbackTaxRate: 0.10
PricingCalculation: ProductTaxDetails: null
PricingCalculation: CASE 2: Product HAS NO additional taxes - applying ONLY store taxes (default)
PricingCalculation: Applying store taxes (default taxes) only...
PricingCalculation: === calculateComplexTax START ===
PricingCalculation: TaxableAmount: 50.00
PricingCalculation: TaxDetails count: 4
PricingCalculation: IsStoreTax: true
PricingCalculation: Applicable taxes count: 2
PricingCalculation: Processing tax: ILLINOIS STATE TAX (Percentage, 6.25, isDefault: true)
PricingCalculation: Percentage tax applied: Rate=0.0625, Amount=3.125
PricingCalculation: Processing tax: COOK COUNTY TAX (Percentage, 1.75, isDefault: true)
PricingCalculation: Percentage tax applied: Rate=0.0175, Amount=0.875
PricingCalculation: Final result: TotalTaxAmount=4.00, TotalTaxRate=0.08
PricingCalculation: === calculateComplexTax END ===
PricingCalculation: Store taxes applied: Amount=4.00, Rate=0.08
PricingCalculation: Final result: TotalTaxAmount=4.00, TotalTaxRate=0.08
PricingCalculation: === calculateItemTax END ===

// Final summary
PricingCalculation: Total product tax amount: 8.998
PricingCalculation: === calculateProductLevelTax END ===
PricingCalculation: Total product tax amount: 8.998
```

---

## How to View Logs

### In Android Studio
1. Open **Logcat** window (View → Tool Windows → Logcat)
2. Filter by tag: `PricingCalculation`
3. Or filter by text: `tax` or `Tax`

### Using ADB
```bash
adb logcat -s PricingCalculation
```

### Filter Specific Logs
```bash
# Only tax calculation logs
adb logcat PricingCalculation:D *:S

# Include errors
adb logcat PricingCalculation:D PricingCalculation:E *:S
```

---

## Log Levels

All tax calculation logs use **Log.d()** (Debug level), so they will only show if:
- App is in debug mode, OR
- Log level is set to Debug or Verbose

---

## Key Information Logged

1. **Input Parameters:**
   - Cart items count
   - Tax rate
   - Pricing type (card/cash)
   - Subtotal values
   - Tax details count
   - Tax exempt status

2. **Item Processing:**
   - Item name
   - Taxable amount per item
   - Product tax details
   - Which case is being applied (CASE 1 or CASE 2)

3. **Tax Application:**
   - Each tax being processed
   - Tax type (Percentage/Fixed Amount)
   - Tax rate and calculated amount
   - Store vs Product tax separation

4. **Results:**
   - Total tax amount per item
   - Total tax rate per item
   - Final aggregated tax amount

5. **Edge Cases:**
   - Empty cart
   - Zero subtotal
   - Tax-exempt items
   - Zero taxable amount
   - Missing tax details

---

## Troubleshooting with Logs

### If tax is 0:
1. Check: `Tax not applied - isTaxApplied: X, taxExempt: Y`
2. Check: `Product X is tax-exempt`
3. Check: `Edge case: Zero taxable amount`

### If tax seems wrong:
1. Check: `ItemTaxableAmount: X` - Should match expected taxable amount
2. Check: `Applicable taxes count: X` - Should match number of taxes
3. Check: `Store taxes applied: Amount=X, Rate=Y` - Verify amounts
4. Check: `Product taxes applied: Amount=X, Rate=Y` - Verify amounts

### If tax breakdown missing:
1. Check: `TaxDetails: X` - Should be > 0
2. Check: `ProductTaxDetails: X` - Should show product taxes if any
3. Check: `CASE 1` or `CASE 2` - Understand which path is taken

