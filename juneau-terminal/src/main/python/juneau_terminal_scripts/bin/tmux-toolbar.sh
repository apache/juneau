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
# A one-row toolbar for a watch-run.sh run window: [Hide output] [Cancel] [Close] and the run's status.  watch-run.sh starts it
# as the last follower pane; it takes the run from TMUX_PANE_RUN_ID (the leader's pane id) and TMUX_PANE_RUN_CANCEL, which
# tmux-pane.sh exports to every follower, and polls the leader's @tmux_state pane option for the status.
# Requires: bash, tmux 3.4+, tmux-pane.sh beside it.  Tested on macOS (bash 3.2).
#
# Usage: tmux-toolbar.sh [--title T]
#   Buttons (click, or press the key):  [Hide output]/[Show output] h   [Cancel] k   [Close] x
#   Hide moves the log pane to a hidden window (its tail keeps running); Show puts it back under the leader.  Cancel is only
#   live while the run is running.  Close cancels a running run, then closes the leader, the log pane and this pane.
#
#   --print-status RAW        print the status word for a leader @tmux_state value, and exit (used by the tests)
#   --print-line RAW HIDDEN TITLE   print the toolbar text (HIDDEN is 0 or 1), and exit
#   --print-hit COL           print which button owns 1-based column COL (hide, cancel, close or none), and exit
#   --print-click SEQ         print the column of a left press in SEQ (the bytes after ESC of an SGR mouse report), or none
#
# Edit this file atomically (write a temp file, then mv it over): a running bash reads the script as it goes.
set -uo pipefail

here=$(cd "$(dirname "$0")" && pwd)

# Fixed button columns (1-based, inclusive).  "Hide output" and "Show output" are the same width, so they never move.
HIDE_C1=1 HIDE_C2=13 CANCEL_C1=15 CANCEL_C2=22 CLOSE_C1=24 CLOSE_C2=30

# The status word for the leader's @tmux_state: running, `exit N`, `kept (exit N)`, `kept (cancelled, exit N)`; empty = pane gone.
status_word() {
	local n
	case $1 in
		'') echo closed ;;
		running) echo running ;;
		*cancelled*) echo cancelled ;;
		*exit\ *)
			n=${1##*exit }
			n=${n%%[!0-9]*}
			if [ "$n" = 0 ]; then echo ok; else echo "failed (exit $n)"; fi ;;
		*) echo "$1" ;;
	esac
}

# The toolbar text.  $1 raw state, $2 hidden (0/1), $3 title, $4 extra message (optional), $5 columns to fit (0 = no limit),
# $6 styled (1 dims [Cancel] once the run is over; the plain form is what the tests compare).
render_line() {
	local raw=$1 hidden=$2 title=$3 msg=${4:-} cols=${5:-0} styled=${6:-0} hide='[Hide output]' rest
	[ "$hidden" != 1 ] || hide='[Show output]'
	rest="  $title: $(status_word "$raw")"
	[ -z "$msg" ] || rest="$rest ($msg)"
	[ "$cols" -le "$CLOSE_C2" ] 2>/dev/null || rest=${rest:0:$((cols - CLOSE_C2))}
	if [ "$styled" = 1 ] && [ "$raw" != running ]; then
		printf '%s \033[2m[Cancel]\033[22m [Close]%s' "$hide" "$rest"
	else
		printf '%s [Cancel] [Close]%s' "$hide" "$rest"
	fi
}

# Which button owns column $1: hide, cancel, close or none.
hit_button() {
	local c=$1
	case $c in ''|*[!0-9]*) echo none; return ;; esac
	if [ "$c" -ge $HIDE_C1 ] && [ "$c" -le $HIDE_C2 ]; then echo hide
	elif [ "$c" -ge $CANCEL_C1 ] && [ "$c" -le $CANCEL_C2 ]; then echo cancel
	elif [ "$c" -ge $CLOSE_C1 ] && [ "$c" -le $CLOSE_C2 ]; then echo close
	else echo none
	fi
}

# $1 = the bytes after the ESC of an SGR mouse report (`[<b;x;yM` press, `m` release).  Prints the column of a left-button
# press and succeeds; anything else fails.
click_column() {
	local body final b x y
	case $1 in '[<'*[Mm]) ;; *) return 1 ;; esac
	body=${1#'[<'}
	final=${body#"${body%?}"}
	body=${body%?}
	[ "$final" = M ] || return 1
	IFS=';' read -r b x y <<< "$body"
	[ "$b" = 0 ] || return 1
	case $x in ''|*[!0-9]*) return 1 ;; esac
	echo "$x"
}

case ${1:-} in
	--print-status) status_word "${2:-}"; exit 0 ;;
	--print-line) render_line "${2:-}" "${3:-0}" "${4:-}"; echo; exit 0 ;;
	--print-hit) hit_button "${2:-}"; exit 0 ;;
	--print-click) click_column "${2:-}" || echo none; exit 0 ;;
esac

# --- the pane ---

title=run
while [ $# -gt 0 ]; do
	case $1 in
		--title) [ $# -ge 2 ] || { echo "tmux-toolbar.sh: --title needs a value" >&2; exit 2; }; title=$2; shift ;;
		*) echo "tmux-toolbar.sh: unknown argument $1" >&2; exit 2 ;;
	esac
	shift
done
run=${TMUX_PANE_RUN_ID:-} cancel_cmd=${TMUX_PANE_RUN_CANCEL:-} self=${TMUX_PANE:-}
[ -n "${TMUX:-}" ] && [ -n "$run" ] && [ -n "$cancel_cmd" ] && [ -n "$self" ] ||
	{ echo "tmux-toolbar.sh: must run as a watch-run.sh pane (TMUX, TMUX_PANE, TMUX_PANE_RUN_ID, TMUX_PANE_RUN_CANCEL)" >&2; exit 2; }

leader_state() { tmux show-options -pqv -t "$run" @tmux_state 2>/dev/null; }

# The log follower: a follower of this run that is not this pane (it may sit in the hidden window).
find_log() {
	tmux list-panes -a -F '#{pane_id}|#{@tmux_parent}|#{@tmux_state}' 2>/dev/null |
		awk -F'|' -v r="$run" -v s="$self" '$2 == r && $3 == "follower" && $1 != s {print $1; exit}'
}

# 1 when the log pane lives in another window than this pane.
is_hidden() {
	local log=$1 a b
	[ -n "$log" ] || { echo 0; return; }
	a=$(tmux display-message -p -t "$log" '#{window_id}' 2>/dev/null)
	b=$(tmux display-message -p -t "$self" '#{window_id}' 2>/dev/null)
	if [ -n "$a" ] && [ "$a" != "$b" ]; then echo 1; else echo 0; fi
}

msg=
toggle_output() {
	local log; log=$(find_log)
	[ -n "$log" ] || { msg="no log pane"; return; }
	if [ "$(is_hidden "$log")" = 0 ]; then
		tmux break-pane -d -s "$log" -n "$title-log" 2>/dev/null || msg="could not hide the log"
	else
		tmux join-pane -d -v -l 40% -s "$log" -t "$run" 2>/dev/null || msg="could not show the log"
	fi
}

do_cancel() {
	[ "$(leader_state)" = running ] || return 0
	bash -c "$cancel_cmd" >/dev/null 2>&1 &
	msg="cancelling"
}

# Cancels a running run first, then has tmux-pane.sh close the leader and every follower (this pane included) from the tmux
# server, so the close survives this pane dying.  When the leader is already gone, close what is left by hand.
do_close() {
	local log i
	if [ "$(leader_state)" = running ]; then
		bash -c "$cancel_cmd" >/dev/null 2>&1
		# tmux-pane.sh close refuses while the leader still says running, and the runner clears that a moment after the cancel returns
		for ((i = 0; i < 100; i++)); do [ "$(leader_state)" = running ] || break; sleep 0.1; done
	fi
	if [ -n "$(leader_state)" ]; then
		tmux run-shell -b "$(printf '%q' "$here/tmux-pane.sh") close $(printf '%q' "$run") >/dev/null 2>&1"
	else
		log=$(find_log)
		[ -z "$log" ] || tmux kill-pane -t "$log" 2>/dev/null
		tmux kill-pane -t "$self" 2>/dev/null
	fi
	exit 0
}

act() {
	case $1 in
		hide) toggle_output ;;
		cancel) do_cancel ;;
		close) do_close ;;
	esac
}

stty_saved=$(stty -g 2>/dev/null)
restore() {
	printf '\033[?1006l\033[?1000l\033[?25h'
	[ -z "$stty_saved" ] || stty "$stty_saved" 2>/dev/null
}
trap restore EXIT
trap 'exit 130' HUP INT TERM
stty -echo -icanon min 1 time 0 2>/dev/null
printf '\033[?25l\033[?1000h\033[?1006h'
tmux resize-pane -y 1 -t "$self" 2>/dev/null

last_raw=
redraw() {
	local raw log cols h
	# A window resize hands the extra rows to the last pane, which is this one; give them back to the panes above.
	h=$(tmux display-message -p -t "$self" '#{pane_height}' 2>/dev/null)
	[ -z "$h" ] || [ "$h" = 1 ] || tmux resize-pane -y 1 -t "$self" 2>/dev/null
	raw=$(leader_state)
	[ "$raw" = "$last_raw" ] || { msg=; last_raw=$raw; }
	log=$(find_log)
	cols=$(tput cols 2>/dev/null || echo 0)
	printf '\r\033[2K%s' "$(render_line "$raw" "$(is_hidden "$log")" "$title" "$msg" "$cols" 1)"
}

redraw
while :; do
	if IFS= read -rsn1 -t 1 ch; then
		case $ch in
			h|H) toggle_output ;;
			k|K) do_cancel ;;
			x|X) do_close ;;
			$'\033')
				seq=
				while IFS= read -rsn1 -t 1 c; do
					seq=$seq$c
					case $c in M|m) break ;; esac
					[ ${#seq} -lt 24 ] || break
				done
				if col=$(click_column "$seq"); then act "$(hit_button "$col")"; fi ;;
		esac
	fi
	redraw
done
