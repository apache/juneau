# Start Docusaurus Server Script

## Overview

`start-docusaurus.py` starts the Docusaurus development server with automatic cleanup and process management.

## What It Does

The script performs these operations before starting the server:

1. **Kills Existing Processes** - Automatically terminates any process running on port 3000
2. **Clears Caches** - Removes `.docusaurus`, `build`, and `node_modules/.cache` directories
3. **Starts Fresh Server** - Launches the Docusaurus development server
4. **Cross-Platform** - Works on macOS, Linux, and Windows

## Usage

### Start the Server

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/start-docusaurus.py
```

The server will start at `http://localhost:3000/`

Press `Ctrl+C` to stop the server.

## Output Example

```
🔄 Starting Docusaurus server...
📁 Working directory: /Users/james.bognar/git/juneau/juneau-docs
🔍 Checking for existing processes on port 3000...
⚡ Killing existing process on port 3000 (PID: 12345)
🧹 Clearing cache...
   Removed: .docusaurus
   Removed: build
   Removed: node_modules/.cache
🚀 Starting Docusaurus server...

[INFO] Starting the development server...
[SUCCESS] Docusaurus website is running at: http://localhost:3000/
```

## When to Use

Use this script when:
- Starting development on documentation
- The server is acting strangely (cache issues)
- Port 3000 is already in use
- You need a clean start after major changes

## Features

### Automatic Process Cleanup

The script automatically finds and kills any process using port 3000, so you don't have to manually search for and kill stale processes.

**macOS/Linux**: Uses `lsof -ti:3000`  
**Windows**: Uses `netstat -ano` to find the process

### Cache Clearing

Clears these directories:
- `.docusaurus` - Docusaurus build cache
- `build` - Production build output
- `node_modules/.cache` - npm cache

This ensures a clean start and resolves most cache-related issues.

### Keyboard Interrupt Handling

The script gracefully handles `Ctrl+C` interrupts and shows a proper shutdown message.

## Troubleshooting

### Port 3000 Still in Use

If the script can't kill the existing process automatically:

**macOS/Linux:**
```bash
lsof -ti:3000 | xargs kill -9
```

**Windows:**
```bash
netstat -ano | findstr :3000
taskkill /PID <pid> /F
```

### Permission Denied Errors

If you see permission errors when deleting cache directories:

```bash
# Fix permissions (macOS/Linux)
sudo chown -R $USER:$USER juneau-docs/.docusaurus juneau-docs/build

# Or manually delete
rm -rf juneau-docs/.docusaurus juneau-docs/build juneau-docs/node_modules/.cache
```

### npm Command Not Found

Make sure Node.js and npm are installed:

```bash
node --version
npm --version
```

If not installed, visit https://nodejs.org/

### package.json Not Found

The script expects to run from the Juneau root directory. Make sure you're in:
```
/Users/james.bognar/git/juneau
```

## Requirements

- **Python**: 3.6 or higher
- **Node.js**: 14.x or higher
- **npm**: 6.x or higher
- **Dependencies**: Run `npm install` in `/juneau-docs` directory first

## Replacing the Old Script

This Python script replaces `/juneau-docs/start-server.sh` with:
- ✅ Better process management
- ✅ Cross-platform compatibility
- ✅ Automatic cache clearing
- ✅ Clearer error messages
- ✅ More robust process detection

## Development Workflow

**Typical usage during development:**

```bash
# Start the server
python3 scripts/start-docusaurus.py

# Edit documentation files in juneau-docs/docs/
# Browser auto-reloads as you save changes

# When done, press Ctrl+C to stop
```

## Notes

- The script automatically detects the `/juneau-docs` directory
- Cache clearing is automatic - no flags needed
- The server supports hot-reloading (changes appear immediately in browser)
- Port 3000 is the default Docusaurus port

