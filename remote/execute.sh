#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
else
  echo "Error: .env file not found in $SCRIPT_DIR"
  exit 1
fi

if [ -z "$REMOTE_USER" ] || [ -z "$REMOTE_PASSWORD" ] || [ -z "$REMOTE_ADDRESS" ]; then
  echo "Missing required environment variables in .env file."
  exit 1
fi

# Validate arguments
if [ "$#" -lt 1 ]; then
  echo "Usage: ./upload.sh [<file_pattern>] <remote_path>"
  exit 1
fi

if [ "$#" -eq 1 ]; then
  FILE_PATTERN="$SCRIPT_DIR/build/libs/*.jar"
  REMOTE_PATH="$1"
else
  FILE_PATTERN="$SCRIPT_DIR/$1"
  REMOTE_PATH="$2"
fi

# Find matching files
shopt -s nullglob
files=( $FILE_PATTERN )
shopt -u nullglob

if [ ${#files[@]} -eq 0 ]; then
  echo "Error: No file found matching pattern '$FILE_PATTERN'."
  exit 1
elif [ ${#files[@]} -gt 1 ]; then
  echo "Error: More than one file found matching pattern '$FILE_PATTERN'."
  exit 1
fi

localFile="${files[0]}"
fileName="$(basename "$localFile")"

# If remotePath is a directory (ends with / or \), append formatted file name manually
if [[ "$REMOTE_PATH" =~ [\\/]$ ]]; then
  baseName="${fileName%%-*}.jar"
  REMOTE_PATH="$REMOTE_PATH$baseName"
fi

# Use sshpass and scp for file transfer
sshpass -p "$REMOTE_PASSWORD" \
  scp -o StrictHostKeyChecking=no "$localFile" "$REMOTE_USER@$REMOTE_ADDRESS:$REMOTE_PATH"

if [ $? -eq 0 ]; then
  echo "Upload finished"
else
  echo "File upload failed"
  exit 1
fi
