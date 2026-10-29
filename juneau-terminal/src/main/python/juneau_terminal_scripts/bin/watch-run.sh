#!/usr/bin/env bash
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
# Run a juneau-terminal based script (push.py, release.py, deploy*.py) in a watchable tmux pane with its full log tailed in
# a pane below it, and block until it finishes.  A wrapper over tmux-pane.sh that owns the juneau-terminal contract: it
# chooses the log and result paths itself (so nothing has to guess them), exports them, and prints the result summary.
# Requires: bash, tmux, tmux-pane.sh beside it, perl (for the follower filter), python3 (optional, for the result summary).  Tested on macOS.
#
# Usage: watch-run.sh [--no-tail] [--no-close-kept] [-d DIR] [-n LINES] [-t TITLE] [-k fail|always|never] [-K MIN] [--reap]
#                     [--keep-on-cancel] [--read-only] -- CMD [ARGS...]
#
#   --no-tail  do not open the lower pane running `tail -F` on the full log
#   --no-close-kept  leave kept panes from earlier runs open (by default they are closed before the new pane opens)
#   --print-tail-filter  print the filter the follower pane pipes `tail -F` through, and exit (used by the tests)
#   other options pass straight through to tmux-pane.sh (see its header); -t defaults to the script's name
#
# Before the pane opens it prints `tail -f <full log>` and `board: <board log>` (relay the first to the person watching).  The
# logs live in ${JUNEAU_TERMINAL_LOG_DIR:-~/.cache/juneau-terminal} as <stem>-<YYYYMMDD-HHMMSS>-<pid>.log (and .board.log), where
# <stem> is the script's basename; the command gets JUNEAU_TERMINAL_FULL_LOG, JUNEAU_TERMINAL_BOARD_LOG, JUNEAU_TERMINAL_RESULT (a
# file in a temp dir this removes on exit) and JUNEAU_TERMINAL_CANCEL (the command that cancels the run).  When the command
# ends, tmux-pane.sh prints the pane tail and `exit=N`; this then prints the result summary (when the command wrote
# one) and `exit=N` again, so read the last `exit=` line.  Exits with the command's code; 2 when not inside tmux.
#
# Edit this file atomically (write a temp file, then mv it over): a running bash reads the script as it goes.
set -uo pipefail

# The follower shows the log through a filter, so the file itself stays byte-for-byte unchanged: U+2028, U+2029 and the
# other format characters (\p{Cf}) become a space, because tmux and the outer terminal disagree on their width and the
# pane's line ends then overflow into the pane to the left.  C0 controls other than tab, newline and ESC (so ANSI colour
# survives), and DEL, are dropped.  Invalid UTF-8 becomes U+FFFD instead of killing the filter.
tail_filter() {
	printf '%s' "perl -MEncode -e '\$|=1; while (<STDIN>) { \$_ = decode(\"UTF-8\", \$_); s/[\\x{2028}\\x{2029}\\p{Cf}]/ /g; s/[\\x00-\\x08\\x0b-\\x1a\\x1c-\\x1f\\x7f]//g; print encode(\"UTF-8\", \$_) }'"
}

here=$(cd "$(dirname "$0")" && pwd)

usage() { awk '/^# \*+$/ && length($0) > 40 {n++; next} n >= 2 && /^# Edit this file/ {exit} n >= 2' "$0" >&2; exit 2; }

no_tail=0 close_kept=1 title=
passthru=()
while [ $# -gt 0 ]; do
	case $1 in
		--) shift; break ;;
		--no-tail) no_tail=1 ;;
		--no-close-kept) close_kept=0 ;;
		--print-tail-filter) tail_filter; echo; exit 0 ;;
		--reap|--keep-on-cancel|--read-only) passthru+=("$1") ;;
		-[dntkK]) [ $# -ge 2 ] || usage; [ "$1" != -t ] || title=$2; passthru+=("$1" "$2"); shift ;;
		-[dntkK]?*) [ "${1:0:2}" != -t ] || title=${1:2}; passthru+=("$1") ;;
		*) usage ;;
	esac
	shift
done
[ $# -gt 0 ] || usage
[ -n "${TMUX:-}" ] || { echo "watch-run.sh: not inside tmux" >&2; exit 2; }

# The file-name form of a name, as juneau-terminal's safe_name makes it: runs of anything but word characters and dots
# become one `-`, leading and trailing `-` and `.` are trimmed, and an empty result is `run`.
safe_name() {
	local s
	s=$(printf '%s' "$1" | sed -E -e 's/[^[:alnum:]_.]+/-/g' -e 's/^[-.]+//' -e 's/[-.]+$//')
	echo "${s:-run}"
}

# The stem is the first script-like word of the command: not an option, no whitespace or `=`, and either containing a `/`
# or ending in a script extension (python3 scripts/push.py -> push).  Else the first word's basename.
script_stem() {
	local a b pick=
	for a in "$@"; do
		case $a in -*|*[[:space:]]*|*=*|'') continue ;; esac
		case $a in */*|*.py|*.sh|*.bash|*.js|*.mjs|*.rb|*.pl|*.ts) pick=$a; break ;; esac
	done
	[ -n "$pick" ] || pick=$1
	b=${pick##*/}
	case $b in .*) ;; *.*) b=${b%.*} ;; esac
	safe_name "$b"
}

stem=$(script_stem "$@")
[ -n "$title" ] || passthru+=(-t "$stem")

logdir=${JUNEAU_TERMINAL_LOG_DIR:-$HOME/.cache/juneau-terminal}
mkdir -p "$logdir" || exit 1
logdir=$(cd "$logdir" && pwd)
base=$logdir/$stem-$(date +%Y%m%d-%H%M%S)-$$
full=$base.log board=$base.board.log
rd=$(mktemp -d "${TMPDIR:-/tmp}/watch-run.XXXXXX") || exit 1
result=$rd/result.json
trap '[ -z "$rd" ] || rm -rf "$rd"' EXIT
touch "$full" "$board"

echo "tail -f $(printf '%q' "$full")"
echo "board: $(printf '%q' "$board")"

# Two levels of `bash -c` (the pane's wrapper, then this string), so every value is quoted with %q.  JUNEAU_TERMINAL_CANCEL
# is left as a reference, expanded inside the pane where tmux-pane.sh has already exported TMUX_PANE_RUN_CANCEL.
cmdq=$(printf '%q ' "$@")
leader="JUNEAU_TERMINAL_FULL_LOG=$(printf '%q' "$full") JUNEAU_TERMINAL_BOARD_LOG=$(printf '%q' "$board") JUNEAU_TERMINAL_RESULT=$(printf '%q' "$result") JUNEAU_TERMINAL_CANCEL=\"\$TMUX_PANE_RUN_CANCEL\" ${cmdq% }"
args=(${passthru[@]+"${passthru[@]}"} --pane "$leader")
[ $no_tail = 1 ] || args+=(--pane-title "$(basename "$full")" --pane "tail -F $(printf '%q' "$full") | $(tail_filter)")

# Clear finished (kept) panes from earlier runs and their followers first; tmux-pane.sh refuses running ones.  A failed
# cleanup only warns, so it never stops the launch.  --no-close-kept skips it.
[ $close_kept = 0 ] || "$here/tmux-pane.sh" close --kept >/dev/null 2>&1 || echo "watch-run.sh: warning: could not close leftover panes" >&2

# Not exec'd, so the summary can follow; INT and TERM are forwarded so the pane runner cleans up.
"$here/tmux-pane.sh" "${args[@]}" &
child=$!
trap 'kill -TERM "$child" 2>/dev/null' INT TERM
rc=0
while kill -0 "$child" 2>/dev/null; do
	wait "$child"
	rc=$?
done
trap - INT TERM

# The summary supplements the pane tail; a missing or unparseable result file is silent.
if [ -s "$result" ]; then
	python3 - "$result" 2>/dev/null <<'PY'
import json, sys
try:
    r = json.load(open(sys.argv[1]))
    out = [f"result: status={r.get('status')} exit={r.get('exit')} ms={r.get('ms')}"]
    for row in r.get("rows", []):
        t = row.get("tests")
        tests = f" tests={t['total']} fail={t['fail']} err={t['err']} skip={t['skip']}" if t else ""
        out.append(f"  {row.get('status', '?'):5} {row.get('label') or row.get('id')} ({row.get('ms')}ms){tests} {row.get('summary') or ''}".rstrip())
    for n in r.get("notes", []):
        out.append(f"  {n.get('level')}: {n.get('text')}")
    for k, v in r.get("logs", {}).items():
        out.append(f"  log.{k}: {v}")
except Exception:
    sys.exit(1)
print("\n".join(out))
PY
fi
echo "exit=$rc"
exit "$rc"
