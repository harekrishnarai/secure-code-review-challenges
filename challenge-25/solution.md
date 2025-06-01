# Challenge 25: NoSQL Injection via Dynamic Query Construction

## Vulnerability
The application constructs MongoDB queries dynamically by iterating over user-provided JSON input without validation. This allows for NoSQL injection attacks.

## Location
`main.go` lines 39-42:
```go
filter := bson.M{}
for key, value := range creds {
    filter[key] = value
}
```

## Impact
- Authentication bypass by injecting MongoDB operators
- Data exfiltration through query manipulation
- Information disclosure about database structure

## Exploitation
POST to `/login` with:
```json
{
  "username": {"$ne": null},
  "password": {"$ne": null}
}
```

This bypasses authentication by using the `$ne` (not equal) operator.

## Remediation
1. Use parameterized queries with explicit field mapping
2. Validate and whitelist allowed query operators
3. Implement strict input validation for user credentials
4. Use MongoDB's query sanitization libraries
