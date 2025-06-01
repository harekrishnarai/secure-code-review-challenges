# Challenge 28: Information Disclosure via Wildcard Route

## Vulnerability
The application uses a wildcard route `/profile*` that matches any path starting with `/profile`. This can lead to unintended information disclosure when combined with static file serving or other middleware.

## Location
`main.js` line 51: `app.get("/profile*", (req, res) => {`

## Impact
- Information disclosure of sensitive API keys
- Bypass of intended access controls
- Potential exposure of internal application structure

## Exploitation
Access any URL starting with `/profile` (e.g., `/profile/../../etc/passwd`, `/profileadmin`, `/profile.json`) to potentially access the profile endpoint and view sensitive information including API keys.

## Remediation
1. Use exact route matching instead of wildcards
2. Implement proper path validation and sanitization
3. Use middleware to validate route parameters
4. Apply principle of least privilege for route access
