#!/bin/bash
# Usage: tail-log.sh LOG LINES ROWS
# Prints what `tail -F run.log` shows once LOG has LINES lines: the last ROWS of them.  Then waits (to be killed).
# A capture takes one pane at a time, so this stands in for the follower pane beside a live run.
head -n "$2" "$1" | tail -n "$3"
exec sleep 600
