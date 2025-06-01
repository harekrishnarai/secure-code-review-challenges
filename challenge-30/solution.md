# Challenge 30: Configuration Injection via Reflection

## Vulnerability
The application uses Go's reflection to dynamically update configuration fields based on user input. While it appears to sanitize the output, an attacker can enable debug mode and expose sensitive configuration including API keys and environment variables.

## Location
`main.go` lines 46-74: The `updateConfigField` function uses reflection to set arbitrary fields
`main.go` lines 82-87: Field name transformation allows indirect access to sensitive fields

## Impact
- Information disclosure of sensitive configuration data
- Exposure of API keys, database credentials, and environment variables
- Potential privilege escalation through configuration manipulation

## Why It's Hard to Detect
- The reflection usage appears legitimate for configuration management
- Field name sanitization looks secure with `strings.Title()`
- The debug endpoint seems properly protected with a flag check
- Static analyzers would miss the logical flow that enables the vulnerability

## Exploitation
1. POST to `/config` with:
```json
{
  "debug_mode": "true"
}
```

2. Access `/debug` to view sensitive information including:
   - Database credentials
   - API keys
   - Environment variables
   - Full configuration

## Remediation
1. Avoid using reflection for user-controlled configuration updates
2. Implement a whitelist of allowed configuration fields
3. Never expose sensitive fields through debug endpoints
4. Use proper configuration management with validation
5. Implement proper access controls for configuration endpoints
