package com.security.challenge;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
@RequestMapping("/api")
public class DataProcessorController {
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Security patterns to block obvious malicious input
    private final Pattern[] securityPatterns = {
        Pattern.compile(".*\\b(eval|exec|system|runtime)\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*[<>\"'`].*"),
        Pattern.compile(".*\\$\\{.*\\}.*")
    };
    
    @PostMapping("/process")
    @ResponseBody
    public Map<String, Object> processData(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String operation = (String) request.get("operation");
            Object data = request.get("data");
            
            if (!isValidOperation(operation)) {
                response.put("error", "Invalid operation");
                return response;
            }
            
            // Process data based on operation type
            Object result = performOperation(operation, data);
            response.put("result", result);
            response.put("status", "success");
            
        } catch (Exception e) {
            response.put("error", "Processing failed: " + e.getMessage());
            response.put("status", "error");
        }
        
        return response;
    }
    
    private boolean isValidOperation(String operation) {
        if (operation == null || operation.trim().isEmpty()) {
            return false;
        }
        
        // Check against security patterns
        for (Pattern pattern : securityPatterns) {
            if (pattern.matcher(operation).matches()) {
                return false;
            }
        }
        
        return true;
    }
    
    private Object performOperation(String operation, Object data) throws Exception {
        switch (operation.toLowerCase()) {
            case "format":
                return formatData(data);
            case "validate":
                return validateData(data);
            case "transform":
                return transformData(data);
            case "calculate":
                return calculateData(data);
            default:
                // Dynamic operation handling for extensibility
                return executeCustomOperation(operation, data);
        }
    }
    
    private Object formatData(Object data) {
        if (data instanceof String) {
            return ((String) data).toUpperCase().trim();
        }
        return data.toString();
    }
    
    private Object validateData(Object data) {
        Map<String, Boolean> validation = new HashMap<>();
        
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            for (String key : dataMap.keySet()) {
                validation.put(key, dataMap.get(key) != null);
            }
        } else {
            validation.put("data", data != null);
        }
        
        return validation;
    }
    
    private Object transformData(Object data) throws Exception {
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            
            // Support for custom transformation expressions
            String expression = (String) dataMap.get("expression");
            Object target = dataMap.get("target");
            
            if (expression != null && target != null) {
                return applyTransformation(expression, target);
            }
        }
        
        return data;
    }
    
    private Object calculateData(Object data) throws Exception {
        if (data instanceof Map) {
            Map<String, Object> calc = (Map<String, Object>) data;
            String formula = (String) calc.get("formula");
            
            if (formula != null) {
                return evaluateFormula(formula, calc);
            }
        }
        
        return 0;
    }
    
    private Object executeCustomOperation(String operation, Object data) throws Exception {
        // Cache the operation for performance
        String cacheKey = operation + "_" + data.hashCode();
        
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        // Dynamic class loading for custom operations
        try {
            String className = "com.custom.operations." + 
                operation.substring(0, 1).toUpperCase() + 
                operation.substring(1).toLowerCase() + "Operation";
            
            Class<?> operationClass = Class.forName(className);
            Object instance = operationClass.getDeclaredConstructor().newInstance();
            
            // Invoke the execute method
            var method = operationClass.getMethod("execute", Object.class);
            Object result = method.invoke(instance, data);
            
            cache.put(cacheKey, result);
            return result;
            
        } catch (ClassNotFoundException e) {
            // Fallback to reflection-based operation
            return executeReflectionOperation(operation, data);
        }
    }
    
    private Object executeReflectionOperation(String operation, Object data) throws Exception {
        // This looks like harmless reflection but can be dangerous
        if (data instanceof Map) {
            Map<String, Object> params = (Map<String, Object>) data;
            String className = (String) params.get("className");
            String methodName = (String) params.get("methodName");
            
            if (className != null && methodName != null) {
                // Additional validation
                if (!className.startsWith("java.lang") && !className.startsWith("java.util")) {
                    throw new SecurityException("Unauthorized class access");
                }
                
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                
                var method = clazz.getMethod(methodName);
                return method.invoke(instance);
            }
        }
        
        return "Operation not supported";
    }
    
    private Object applyTransformation(String expression, Object target) throws Exception {
        // Simple expression evaluator that looks safe but isn't
        if (expression.contains("toString")) {
            return target.toString();
        } else if (expression.contains("hashCode")) {
            return target.hashCode();
        } else if (expression.contains("getClass")) {
            return target.getClass().getName();
        }
        
        // For more complex expressions, use ScriptEngine
        return evaluateExpression(expression, target);
    }
    
    private Object evaluateExpression(String expression, Object target) throws Exception {
        // This looks like it's using a safe expression evaluator
        javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
        javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
        
        // Set the target object in the context
        engine.put("target", target);
        
        // Evaluate the expression
        return engine.eval(expression);
    }
    
    private Object evaluateFormula(String formula, Map<String, Object> context) throws Exception {
        // Simple math formula evaluator
        for (String key : context.keySet()) {
            if (context.get(key) instanceof Number) {
                formula = formula.replace(key, context.get(key).toString());
            }
        }
        
        // Use JavaScript engine for math evaluation
        javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
        javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
        
        return engine.eval(formula);
    }
}
