#!/bin/bash

# Juneau Topic Link Checker
# This script runs the Python script to check for correct topic links

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Change to the juneau root directory
cd "$SCRIPT_DIR"

# Check if Python 3 is available
if ! command -v python3 &> /dev/null; then
    echo "ERROR: python3 is not installed or not in PATH"
    exit 1
fi

# Check if the Python script exists
PYTHON_SCRIPT="$SCRIPT_DIR/juneau-check-topic-links.py"
if [ ! -f "$PYTHON_SCRIPT" ]; then
    echo "ERROR: Python script not found: $PYTHON_SCRIPT"
    exit 1
fi

# Check if juneau-docs directory exists
if [ ! -d "$SCRIPT_DIR/juneau-docs" ]; then
    echo "ERROR: juneau-docs directory not found: $SCRIPT_DIR/juneau-docs"
    echo "Make sure you're running this from the juneau root directory"
    exit 1
fi

echo "Running Juneau Topic Link Checker..."
echo "Script directory: $SCRIPT_DIR"
echo "Python script: $PYTHON_SCRIPT"
echo

# Generate output filename with timestamp
OUTPUT_FILE="topic-link-check-$(date +%Y%m%d-%H%M%S).txt"

echo "Output will be saved to: $OUTPUT_FILE"
echo

# Run the Python script and tee output to file
python3 "$PYTHON_SCRIPT" 2>&1 | tee "$OUTPUT_FILE"

# Exit with the same code as the Python script
exit ${PIPESTATUS[0]}
