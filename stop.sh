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
# Stop the locally-running Apache Juneau Release Manager (started by start.sh).
#
#   ./stop.sh
#
# Finds whatever process is listening on the app's configured port
# (server.port in src/main/resources/application.properties, default 8790)
# and terminates it gracefully (SIGTERM), escalating to SIGKILL only if it
# doesn't exit within ~10s.
#
set -euo pipefail

cd "$(dirname "$0")"

PROPS="src/main/resources/application.properties"
PORT="8790"
if [[ -f "$PROPS" ]]; then
  parsed="$(awk -F= '/^[[:space:]]*server\.port[[:space:]]*=/{gsub(/[^0-9]/,"",$2); print $2; exit}' "$PROPS")"
  [[ -n "$parsed" ]] && PORT="$parsed"
fi

pids="$(lsof -ti "tcp:${PORT}" -sTCP:LISTEN 2>/dev/null || true)"
if [[ -z "$pids" ]]; then
  echo "Release Manager is not running (nothing listening on port ${PORT})."
  exit 0
fi

echo "Stopping Release Manager (port ${PORT}, pid(s): ${pids})..."
kill ${pids} 2>/dev/null || true

for _ in $(seq 1 20); do
  sleep 0.5
  still="$(lsof -ti "tcp:${PORT}" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -z "$still" ]]; then
    echo "Stopped."
    exit 0
  fi
done

echo "Did not exit gracefully; sending SIGKILL..."
kill -9 $(lsof -ti "tcp:${PORT}" -sTCP:LISTEN 2>/dev/null || true) 2>/dev/null || true
echo "Stopped."
