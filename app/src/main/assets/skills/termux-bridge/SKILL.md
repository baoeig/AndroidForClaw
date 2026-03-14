---
name: termux-bridge
description: Execute Python, Node.js, and Shell code locally via Termux SSH
metadata:
  {
    "openclaw": {
      "always": false,
      "emoji": "🐧"
    }
  }
---

# Termux Bridge - Local Code Execution via SSH

Execute Python, Node.js, and Shell scripts locally on the Android device using Termux.
Communication uses SSH to Termux's built-in sshd on localhost:8022.

## Requirements

**User must have installed**:
1. Termux (from F-Droid or GitHub)
2. openssh package in Termux (`pkg install openssh`)
3. sshd running in Termux (`sshd`)

**Setup**: See [Quick Setup](#quick-setup)

## Available Tool

### exec

Run commands through the unified exec entry. When Termux is available, exec is routed to Termux via SSH; otherwise it falls back to internal Android exec.

**Parameters**:
- `command` (required): Shell command to execute
- `working_dir` (optional): Working directory (default: Termux home)
- `timeout` (optional): Timeout in seconds (default: 60)
- `backend` (optional): Force routing - "auto" (default) | "termux" | "internal"
- `action` (optional): "exec" (default) or "setup" (check status)
- `runtime` (optional, backward-compat): "python" | "nodejs" | "shell"
- `code` (optional, backward-compat): Code string (used with runtime)

**Returns**:
- `stdout`: Standard output
- `stderr`: Standard error (if any)
- `exitCode`: Exit code (0 = success)
- `backend`: "termux" or "internal"
- `transport`: "ssh" (when using Termux)

## Usage Examples

### Check Environment

```javascript
exec({ command: "node --version && python3 --version" })
```

### Python

```javascript
exec({ command: "python3 -c 'import sys; print(sys.version)'" })
```

### Node.js

```javascript
exec({ command: "node -e 'console.log(process.version)'" })
```

### Shell

```javascript
exec({ command: "ls -lh /sdcard/Download | head -10" })
```

### With Working Directory

```javascript
exec({ command: "ls -la", working_dir: "/sdcard/Download" })
```

### Install Packages

```javascript
exec({ command: "pip3 install requests beautifulsoup4" })
exec({ command: "npm install -g axios" })
```

### Check Setup Status

```javascript
exec({ action: "setup" })
```

## Quick Setup

### Step 1: Install Termux

- **F-Droid** (recommended): https://f-droid.org/packages/com.termux/
- **GitHub**: https://github.com/termux/termux-app/releases

⚠️ Do NOT use Google Play version (outdated)

### Step 2: Setup SSH in Termux

Open Termux and run:

```bash
pkg update
pkg install openssh
passwd          # Set a password (remember it!)
sshd            # Start SSH server on port 8022
whoami          # Note your username (e.g. u0_a123)
```

### Step 3: Configure Credentials

Create config file so the app can connect:

```bash
echo '{"user":"YOUR_USERNAME","password":"YOUR_PASSWORD"}' > /sdcard/.androidforclaw/termux_ssh.json
```

Replace `YOUR_USERNAME` with the output of `whoami`, and `YOUR_PASSWORD` with what you set in `passwd`.

### Step 4: Auto-start sshd (Optional)

To start sshd automatically when Termux opens:

```bash
echo 'sshd' >> ~/.bashrc
```

### Step 5: Install Common Tools (Optional)

```bash
pkg install python nodejs
pip3 install requests beautifulsoup4 pandas
```

### Step 6: Test

In AndroidForClaw chat, send:
> Check if node and python are available

The agent will run `exec({ command: "node -v && python3 -V" })` via SSH.

## Troubleshooting

### "Termux not installed"
Install from F-Droid or GitHub (links above).

### "SSH server not reachable"
1. Open Termux
2. Run `sshd`
3. Verify: `ssh -p 8022 localhost` should prompt for password

### "SSH authentication failed"
1. In Termux: `passwd` (reset password)
2. Update config: `echo '{"user":"YOUR_USER","password":"NEW_PASS"}' > /sdcard/.androidforclaw/termux_ssh.json`

### "Module not found"
Install missing packages:
```bash
pip3 install <package>    # Python
npm install -g <package>  # Node.js
```

### Timeout
Increase timeout for long tasks:
```javascript
exec({ command: "...", timeout: 300 })
```

## Architecture

```
AndroidForClaw App
    └── ExecFacadeTool (unified "exec")
        ├── TermuxBridgeTool (SSH to localhost:8022)
        │   └── Termux sshd → bash → command
        └── ExecTool (internal Android shell fallback)
```

- **Auto-routing**: If Termux+SSH is available, exec goes to Termux. Otherwise falls back to internal shell.
- **SSH transport**: Uses SSHJ library, connects to Termux's OpenSSH server on localhost:8022.
- **Credentials**: Stored in `/sdcard/.androidforclaw/termux_ssh.json`.
- **No polling**: Direct SSH command execution with synchronous stdout/stderr/exitCode return.

## Notes

- **Latency**: ~50ms for SSH connection + command execution time
- **Security**: SSH on localhost only, not exposed to network
- **Sandboxed**: Termux runs in its own Android sandbox
- **No root required**: Standard Termux capabilities
