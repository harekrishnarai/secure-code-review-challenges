# Challenge 26: Prototype Pollution via Object Merge

## Vulnerability
The application uses a vulnerable merge function that doesn't protect against prototype pollution. An attacker can modify Object.prototype by sending specially crafted JSON data.

## Location
`main.js` lines 16-20:
```javascript
function merge(target, source) {
    for (let key in source) {
        target[key] = source[key];
    }
}
```

## Impact
- Prototype pollution leading to application-wide property injection
- Privilege escalation by polluting `isAdmin` property
- Potential RCE in some Node.js environments
- Application logic bypass

## Exploitation
POST to `/update-profile` with:
```json
{
  "__proto__": {
    "isAdmin": true
  }
}
```

This pollutes Object.prototype with `isAdmin: true`, affecting all objects.

## Remediation
1. Use `Object.create(null)` for objects that will be merged
2. Implement safe merge functions that check for dangerous keys
3. Use libraries like `lodash.merge` with proper configuration
4. Validate and sanitize input keys before merging
