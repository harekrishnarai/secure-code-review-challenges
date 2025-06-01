package main

import (
    "context"
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"
    "reflect"
    "strconv"
    "strings"
    "time"
)

type Config struct {
    DatabaseURL    string `json:"database_url"`
    APIKey         string `json:"api_key"`
    DebugMode      bool   `json:"debug_mode"`
    MaxConnections int    `json:"max_connections"`
    AllowedIPs     []string `json:"allowed_ips"`
}

type User struct {
    ID       int    `json:"id"`
    Username string `json:"username"`
    Role     string `json:"role"`
    Active   bool   `json:"active"`
}

var globalConfig Config
var users []User

func init() {
    globalConfig = Config{
        DatabaseURL:    "postgres://user:pass@localhost/db",
        APIKey:         "secret-api-key-12345",
        DebugMode:      false,
        MaxConnections: 100,
        AllowedIPs:     []string{"127.0.0.1", "10.0.0.0/8"},
    }
    
    users = []User{
        {ID: 1, Username: "admin", Role: "administrator", Active: true},
        {ID: 2, Username: "user1", Role: "user", Active: true},
        {ID: 3, Username: "user2", Role: "user", Active: false},
    }
}

// Helper function to update config values using reflection
func updateConfigField(fieldName string, value interface{}) error {
    configValue := reflect.ValueOf(&globalConfig).Elem()
    fieldValue := configValue.FieldByName(fieldName)
    
    if !fieldValue.IsValid() {
        return fmt.Errorf("field %s not found", fieldName)
    }
    
    if !fieldValue.CanSet() {
        return fmt.Errorf("field %s cannot be set", fieldName)
    }
    
    // Handle different types
    switch fieldValue.Kind() {
    case reflect.String:
        if str, ok := value.(string); ok {
            fieldValue.SetString(str)
        }
    case reflect.Bool:
        if str, ok := value.(string); ok {
            if boolVal, err := strconv.ParseBool(str); err == nil {
                fieldValue.SetBool(boolVal)
            }
        }
    case reflect.Int:
        if str, ok := value.(string); ok {
            if intVal, err := strconv.Atoi(str); err == nil {
                fieldValue.SetInt(int64(intVal))
            }
        }
    case reflect.Slice:
        if str, ok := value.(string); ok {
            values := strings.Split(str, ",")
            fieldValue.Set(reflect.ValueOf(values))
        }
    }
    
    return nil
}

func configHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method == "GET" {
        // Return current configuration (sanitized)
        sanitizedConfig := map[string]interface{}{
            "debug_mode":      globalConfig.DebugMode,
            "max_connections": globalConfig.MaxConnections,
            "allowed_ips":     globalConfig.AllowedIPs,
        }
        
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(sanitizedConfig)
        return
    }
    
    if r.Method == "POST" {
        var updates map[string]interface{}
        if err := json.NewDecoder(r.Body).Decode(&updates); err != nil {
            http.Error(w, "Invalid JSON", http.StatusBadRequest)
            return
        }
        
        // Process configuration updates
        for key, value := range updates {
            // Convert key to proper field name format
            fieldName := strings.Title(strings.ReplaceAll(key, "_", ""))
            if fieldName == "ApiKey" {
                fieldName = "APIKey"
            }
            
            if err := updateConfigField(fieldName, value); err != nil {
                log.Printf("Error updating config field %s: %v", fieldName, err)
                continue
            }
            
            log.Printf("Updated config field %s to %v", fieldName, value)
        }
        
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("Configuration updated"))
        return
    }
    
    http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
}

func debugHandler(w http.ResponseWriter, r *http.Request) {
    if !globalConfig.DebugMode {
        http.Error(w, "Debug mode is disabled", http.StatusForbidden)
        return
    }
    
    debugInfo := map[string]interface{}{
        "config":      globalConfig,
        "users":       users,
        "environment": os.Environ(),
        "timestamp":   time.Now(),
    }
    
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(debugInfo)
}

func usersHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(users)
}

func main() {
    http.HandleFunc("/config", configHandler)
    http.HandleFunc("/debug", debugHandler)
    http.HandleFunc("/users", usersHandler)
    
    log.Println("Server starting on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}
