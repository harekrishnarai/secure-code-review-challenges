# Challenge 31: Container Escape via Volume Mount Injection

## Vulnerability
The application validates that Docker images come from a trusted registry and sanitizes environment variables, but fails to properly validate volume mount paths. An attacker can escape the container and access the host filesystem by injecting malicious volume mounts.

## Location
`main.py` lines 79-84:
```python
for volume in volumes:
    if isinstance(volume, str):
        docker_cmd.extend(['-v', volume])
    elif isinstance(volume, dict) and 'host' in volume and 'container' in volume:
        docker_cmd.extend(['-v', f"{volume['host']}:{volume['container']}"])
```

## Impact
- Container escape and host filesystem access
- Privilege escalation on the host system
- Data exfiltration from host directories
- Potential compromise of other containers and services

## Why It's Hard to Detect
- The validation focuses on image registry and environment variables
- Volume validation appears to check for proper structure
- The vulnerability is in the lack of path validation, not the parsing logic
- Most static analyzers would miss this logical security flaw

## Exploitation
Upload a YAML configuration file:
```yaml
image: "internal-registry.company.com/ubuntu:latest"
name: "test-container"
volumes:
  - host: "/etc"
    container: "/host-etc"
  - host: "/var/run/docker.sock"
    container: "/var/run/docker.sock"
  - "/:/host-root"
```

This mounts sensitive host directories including:
- `/etc` (system configuration)
- `/var/run/docker.sock` (Docker daemon socket for full container control)
- `/` (entire host filesystem)

## Remediation
1. Implement strict whitelist for allowed volume mount paths
2. Validate that host paths are within allowed directories
3. Prevent mounting of sensitive system directories
4. Use read-only mounts where possible
5. Implement proper access controls and path sanitization
