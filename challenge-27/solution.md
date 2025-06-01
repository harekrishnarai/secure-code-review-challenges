# Challenge 27: Integer Overflow Leading to Logic Bypass

## Vulnerability
The application performs integer arithmetic without checking for overflow conditions. When calculating totals, large values can cause integer overflow, leading to unexpected results.

## Location
`main.c` line 17: `int total = quantity * price_per_item;`

## Impact
- Price calculation bypass through integer overflow
- Financial loss due to incorrect pricing
- Logic errors in business calculations

## Exploitation
Enter a very large quantity (e.g., 2147483647) that when multiplied by the price causes integer overflow, potentially resulting in a negative or very small total.

Example:
- Quantity: 2147483647
- Price per item: 1
- Result: Integer overflow causes unexpected total calculation

## Remediation
1. Check for potential overflow before performing arithmetic operations
2. Use larger integer types (long long) for calculations
3. Implement bounds checking for user inputs
4. Use safe arithmetic functions that detect overflow
