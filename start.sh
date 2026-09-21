#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Start the Apache Juneau Release Manager locally.
#
#   ./start.sh
#
# Builds and runs the Spring Boot app via the spring-boot-maven-plugin.
# Once started it serves at:
#
#   http://127.0.0.1:8790/
#
# The Juneau REST resources are mounted under /rest/*, e.g.:
#
#   http://127.0.0.1:8790/rest/setup
#   http://127.0.0.1:8790/rest/releases
#
# Bind address/port come from src/main/resources/application.properties.
#
# Frees that port if something is already listening (SIGTERM, then SIGKILL), then
# reloads an existing Chrome/Safari tab on that host:port (or opens a new one)
# once the server accepts connections.
#
set -euo pipefail

cd "$(dirname "$0")"

# Personal JDK layout: ~/jdk/default -> current JDK 17. Honor an existing
# JAVA_HOME if the caller already set one.
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk/default}"
export PATH="$JAVA_HOME/bin:$PATH"

PROPS="src/main/resources/application.properties"
PORT="8790"
if [[ -f "$PROPS" ]]; then
  parsed="$(awk -F= '/^[[:space:]]*server\.port[[:space:]]*=/{gsub(/[^0-9]/,"",$2); print $2; exit}' "$PROPS")"
  [[ -n "$parsed" ]] && PORT="$parsed"
fi
URL="http://127.0.0.1:${PORT}/rest/setup"

# Skip when lsof is missing so Linux CI that sources this file is a no-op.
kill_listen_port() {
  local port="$1"
  command -v lsof >/dev/null 2>&1 || return 0
  local pids
  pids="$(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null || true)"
  [[ -z "$pids" ]] && return 0
  echo "Port ${port} in use (pid(s): ${pids}); stopping..."
  # shellcheck disable=SC2086
  kill ${pids} 2>/dev/null || true
  for _ in $(seq 1 20); do
    sleep 0.5
    [[ -z "$(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null || true)" ]] && return 0
  done
  echo "Did not exit gracefully; sending SIGKILL..."
  # shellcheck disable=SC2046,SC2086
  kill -9 $(lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null || true) 2>/dev/null || true
}

# Reload a Chrome/Safari tab whose URL is already on http://127.0.0.1:$PORT
# (any path). Falls back to `open` if none is found.
reuse_or_open() {
  local url="$1"
  local prefix="http://127.0.0.1:${PORT}"
  local result=""
  if command -v osascript >/dev/null 2>&1; then
    result="$(osascript - "$prefix" 2>/dev/null <<'APPLESCRIPT' || true
on run argv
  set prefix to item 1 of argv
  if application "Google Chrome" is running then
    try
      tell application "Google Chrome"
        repeat with wi from 1 to (count of windows)
          repeat with ti from 1 to (count of tabs of window wi)
            try
              set tabUrl to URL of tab ti of window wi
              if my urlOnPrefix(tabUrl, prefix) then
                set active tab index of window wi to ti
                set index of window wi to 1
                tell tab ti of window wi to reload
                activate
                return "reloaded"
              end if
            end try
          end repeat
        end repeat
      end tell
    end try
  end if
  if application "Safari" is running then
    try
      tell application "Safari"
        repeat with wi from 1 to (count of windows)
          repeat with ti from 1 to (count of tabs of window wi)
            try
              set tabUrl to URL of tab ti of window wi
              if my urlOnPrefix(tabUrl, prefix) then
                set current tab of window wi to tab ti of window wi
                set index of window wi to 1
                try
                  do JavaScript "location.reload()" in tab ti of window wi
                on error
                  set URL of tab ti of window wi to tabUrl
                end try
                activate
                return "reloaded"
              end if
            end try
          end repeat
        end repeat
      end tell
    end try
  end if
  return "none"
end run

on urlOnPrefix(tabUrl, prefix)
  try
    if tabUrl is missing value then return false
    set u to tabUrl as string
    considering case
      if u is prefix then return true
      if u starts with (prefix & "/") then return true
      if u starts with (prefix & "?") then return true
      if u starts with (prefix & "#") then return true
    end considering
  end try
  return false
end urlOnPrefix
APPLESCRIPT
)"
  fi
  [[ "$result" == "reloaded" ]] && return 0
  open "$url"
}

# macOS only. Polls the UI URL so we do not open a tab against a still-booting server.
open_when_ready() {
  local url="$1"
  if [[ "$(uname -s)" != "Darwin" ]] || ! command -v open >/dev/null 2>&1; then
    return 0
  fi
  (
    for _ in $(seq 1 180); do
      if command -v curl >/dev/null 2>&1 && curl -sf -o /dev/null --connect-timeout 1 --max-time 2 "$url"; then
        reuse_or_open "$url"
        exit 0
      fi
      sleep 1
    done
  ) >/dev/null 2>&1 &
  disown $! || true
}

kill_listen_port "$PORT"
echo "Starting Release Manager on ${URL}"
open_when_ready "$URL"

exec mvn spring-boot:run
