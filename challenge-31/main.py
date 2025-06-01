import os
import subprocess
import tempfile
import json
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import yaml

app = Flask(__name__)

# Configuration
UPLOAD_FOLDER = '/tmp/uploads'
ALLOWED_EXTENSIONS = {'yml', 'yaml', 'json'}
DOCKER_REGISTRY = 'internal-registry.company.com'

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def validate_container_config(config):
    """Validate container configuration for security"""
    required_fields = ['image', 'name']
    
    for field in required_fields:
        if field not in config:
            return False, f"Missing required field: {field}"
    
    # Check if image is from trusted registry
    if not config['image'].startswith(DOCKER_REGISTRY):
        return False, "Image must be from trusted registry"
    
    # Basic validation for container name
    if not config['name'].replace('-', '').replace('_', '').isalnum():
        return False, "Invalid container name format"
    
    return True, "Valid configuration"

def sanitize_environment_vars(env_vars):
    """Sanitize environment variables"""
    if not env_vars:
        return {}
    
    sanitized = {}
    dangerous_patterns = ['$(', '`', '${']
    
    for key, value in env_vars.items():
        # Check for dangerous patterns
        str_value = str(value)
        if any(pattern in str_value for pattern in dangerous_patterns):
            continue  # Skip dangerous variables
        
        sanitized[key] = str_value
    
    return sanitized

@app.route('/deploy', methods=['POST'])
def deploy_container():
    try:
        if 'config_file' not in request.files:
            return jsonify({'error': 'No config file provided'}), 400
        
        file = request.files['config_file']
        if file.filename == '' or not allowed_file(file.filename):
            return jsonify({'error': 'Invalid file type'}), 400
        
        filename = secure_filename(file.filename)
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        file.save(filepath)
        
        # Parse configuration file
        with open(filepath, 'r') as f:
            if filename.endswith('.json'):
                config = json.load(f)
            else:  # YAML
                config = yaml.safe_load(f)
        
        # Validate configuration
        is_valid, message = validate_container_config(config)
        if not is_valid:
            return jsonify({'error': message}), 400
        
        # Extract deployment parameters
        image = config['image']
        container_name = config['name']
        env_vars = sanitize_environment_vars(config.get('environment', {}))
        volumes = config.get('volumes', [])
        ports = config.get('ports', [])
        
        # Build Docker command
        docker_cmd = ['docker', 'run', '-d', '--name', container_name]
        
        # Add environment variables
        for key, value in env_vars.items():
            docker_cmd.extend(['-e', f"{key}={value}"])
        
        # Add volume mounts
        for volume in volumes:
            if isinstance(volume, str):
                docker_cmd.extend(['-v', volume])
            elif isinstance(volume, dict) and 'host' in volume and 'container' in volume:
                docker_cmd.extend(['-v', f"{volume['host']}:{volume['container']}"])
        
        # Add port mappings
        for port in ports:
            if isinstance(port, str):
                docker_cmd.extend(['-p', port])
            elif isinstance(port, dict) and 'host' in port and 'container' in port:
                docker_cmd.extend(['-p', f"{port['host']}:{port['container']}"])
        
        # Add image
        docker_cmd.append(image)
        
        # Add any additional arguments
        if 'args' in config:
            docker_cmd.extend(config['args'])
        
        # Execute Docker command
        result = subprocess.run(
            docker_cmd,
            capture_output=True,
            text=True,
            timeout=30
        )
        
        if result.returncode == 0:
            return jsonify({
                'status': 'success',
                'container_id': result.stdout.strip(),
                'message': 'Container deployed successfully'
            })
        else:
            return jsonify({
                'status': 'error',
                'error': result.stderr
            }), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Deployment timeout'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        # Clean up uploaded file
        if 'filepath' in locals() and os.path.exists(filepath):
            os.remove(filepath)

@app.route('/status/<container_name>')
def container_status(container_name):
    try:
        # Get container status
        result = subprocess.run(
            ['docker', 'inspect', container_name],
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            status_data = json.loads(result.stdout)
            return jsonify({
                'status': 'running' if status_data[0]['State']['Running'] else 'stopped',
                'details': status_data[0]['State']
            })
        else:
            return jsonify({'error': 'Container not found'}), 404
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
