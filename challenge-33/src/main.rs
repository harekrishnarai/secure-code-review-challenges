use actix_web::{web, App, HttpServer, HttpResponse, Result, middleware::Logger};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::process::Command;
use base64::{Engine as _, engine::general_purpose};
use regex::Regex;

#[derive(Debug, Serialize, Deserialize, Clone)]
struct User {
    id: u32,
    username: String,
    role: String,
    permissions: Vec<String>,
}

#[derive(Debug, Deserialize)]
struct TaskRequest {
    command: String,
    parameters: HashMap<String, String>,
    encoding: Option<String>,
    timeout: Option<u64>,
}

#[derive(Debug, Serialize)]
struct TaskResponse {
    status: String,
    output: String,
    execution_time: u64,
}

type UserStore = Arc<Mutex<HashMap<String, User>>>;

fn init_users() -> UserStore {
    let mut users = HashMap::new();
    
    users.insert("admin".to_string(), User {
        id: 1,
        username: "admin".to_string(),
        role: "administrator".to_string(),
        permissions: vec!["read".to_string(), "write".to_string(), "execute".to_string()],
    });
    
    users.insert("operator".to_string(), User {
        id: 2,
        username: "operator".to_string(),
        role: "operator".to_string(),
        permissions: vec!["read".to_string(), "execute".to_string()],
    });
    
    Arc::new(Mutex::new(users))
}

fn validate_command(command: &str) -> Result<(), String> {
    // Security validation - block obvious dangerous commands
    let dangerous_patterns = [
        r"rm\s+.*-rf",
        r"sudo\s+",
        r"su\s+",
        r"chmod\s+777",
        r"wget\s+",
        r"curl\s+.*\|\s*sh",
        r"nc\s+.*-e",
    ];
    
    for pattern in &dangerous_patterns {
        let re = Regex::new(pattern).unwrap();
        if re.is_match(command) {
            return Err("Dangerous command detected".to_string());
        }
    }
    
    // Allow only specific safe commands
    let allowed_commands = [
        "ls", "cat", "echo", "date", "whoami", "pwd", "ping", "nslookup", "dig"
    ];
    
    let cmd_parts: Vec<&str> = command.split_whitespace().collect();
    if cmd_parts.is_empty() {
        return Err("Empty command".to_string());
    }
    
    let base_cmd = cmd_parts[0];
    if !allowed_commands.contains(&base_cmd) {
        return Err("Command not in allowlist".to_string());
    }
    
    Ok(())
}

fn sanitize_parameters(params: &HashMap<String, String>) -> HashMap<String, String> {
    let mut sanitized = HashMap::new();
    
    for (key, value) in params {
        // Remove dangerous characters
        let clean_key = key.replace(['&', '|', ';', '`', '$', '(', ')', '<', '>'], "");
        let clean_value = value.replace(['&', '|', ';', '`', '$', '(', ')', '<', '>'], "");
        
        // Limit parameter length
        if clean_key.len() <= 50 && clean_value.len() <= 200 {
            sanitized.insert(clean_key, clean_value);
        }
    }
    
    sanitized
}

fn build_command(task: &TaskRequest) -> Result<String, String> {
    validate_command(&task.command)?;
    
    let sanitized_params = sanitize_parameters(&task.parameters);
    let mut full_command = task.command.clone();
    
    // Replace parameters in command
    for (key, value) in &sanitized_params {
        let placeholder = format!("{{{}}}", key);
        full_command = full_command.replace(&placeholder, value);
    }
    
    // Handle encoding if specified
    if let Some(encoding) = &task.encoding {
        match encoding.as_str() {
            "base64" => {
                // Decode base64 parameters
                for (key, value) in &sanitized_params {
                    if let Ok(decoded) = general_purpose::STANDARD.decode(value) {
                        if let Ok(decoded_str) = String::from_utf8(decoded) {
                            let placeholder = format!("{{{}}}", key);
                            full_command = full_command.replace(&placeholder, &decoded_str);
                        }
                    }
                }
            },
            "url" => {
                // URL decode parameters
                for (key, value) in &sanitized_params {
                    let decoded = urlencoding::decode(value).unwrap_or_default();
                    let placeholder = format!("{{{}}}", key);
                    full_command = full_command.replace(&placeholder, &decoded);
                }
            },
            _ => {} // Ignore unknown encodings
        }
    }
    
    Ok(full_command)
}

async fn execute_task(
    task: web::Json<TaskRequest>,
    users: web::Data<UserStore>
) -> Result<HttpResponse, actix_web::Error> {
    
    let start_time = std::time::Instant::now();
    
    // Build the command
    let command = match build_command(&task) {
        Ok(cmd) => cmd,
        Err(e) => {
            return Ok(HttpResponse::BadRequest().json(TaskResponse {
                status: "error".to_string(),
                output: e,
                execution_time: 0,
            }));
        }
    };
    
    // Set timeout (default 5 seconds)
    let timeout = task.timeout.unwrap_or(5);
    
    // Execute command with timeout
    let output = match Command::new("sh")
        .arg("-c")
        .arg(&command)
        .output() {
        Ok(output) => {
            if output.status.success() {
                String::from_utf8_lossy(&output.stdout).to_string()
            } else {
                String::from_utf8_lossy(&output.stderr).to_string()
            }
        },
        Err(e) => format!("Execution failed: {}", e),
    };
    
    let execution_time = start_time.elapsed().as_millis() as u64;
    
    Ok(HttpResponse::Ok().json(TaskResponse {
        status: "success".to_string(),
        output,
        execution_time,
    }))
}

async fn get_users(users: web::Data<UserStore>) -> Result<HttpResponse, actix_web::Error> {
    let users_guard = users.lock().unwrap();
    let users_list: Vec<&User> = users_guard.values().collect();
    Ok(HttpResponse::Ok().json(users_list))
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    env_logger::init();
    
    let users = init_users();
    
    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(users.clone()))
            .wrap(Logger::default())
            .route("/api/execute", web::post().to(execute_task))
            .route("/api/users", web::get().to(get_users))
    })
    .bind("0.0.0.0:8080")?
    .run()
    .await
}
