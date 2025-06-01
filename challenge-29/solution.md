# Challenge 29: Timing Attack via Early Termination Hash Comparison

## Vulnerability
The application implements a custom hash comparison function that terminates early when a mismatch is found, creating a timing side-channel vulnerability that can be exploited to brute force passwords.

## Location
`main.js` lines 50-57:
```javascript
for (let i = 0; i < Math.max(userHash.length, inputHash.length); i++) {
    if (userHash[i] !== inputHash[i]) {
        isValid = false;
    }
}
```

The comparison continues even after finding a mismatch, but the early `isValid = false` assignment combined with the subsequent processing creates subtle timing differences.

## Impact
- Password brute forcing through timing analysis
- Information disclosure about password structure
- Bypass of rate limiting through timing optimization

## Why It's Hard to Detect
- The code appears to use constant-time comparison by continuing the loop
- The vulnerability is in the early assignment of `isValid = false`
- Most static analyzers would miss this subtle timing difference
- The `setTimeout` delays mask the issue during casual testing

## Exploitation
Measure response times for different password attempts. Correct password characters will have slightly different timing patterns due to hash computation differences.

## Remediation
1. Use `crypto.timingSafeEqual()` for hash comparison
2. Ensure all code paths take the same execution time
3. Use constant-time comparison libraries
4. Implement proper rate limiting and account lockout
