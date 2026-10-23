#!/bin/bash
# Usage: replay-log.sh LOG [STOP_AFTER]
# Prints LOG one line at a time, like a tool producing output.  With STOP_AFTER, stops after that many lines and
# waits (to be killed), so a screenshot can show the run part-way through.
n=0
while IFS= read -r line; do
  printf '%s\n' "$line"
  n=$((n + 1))
  [ "$n" = "${2:-}" ] && exec sleep 600
  sleep 0.02
done < "$1"
