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
# Run commands in tmux side panes, so the person running them can watch live with a real TTY, and block until the main one finishes.
# Command-agnostic: it knows nothing about juneau-terminal.  For a juneau-terminal script (push.py, release.py, deploy*.py) with a
# log tail, use watch-run.sh, which wraps this.
# Requires: bash, tmux, ps, pgrep (lsof optional).  Tested on macOS.
#
# Usage: tmux-pane.sh [-d DIR] [-n LINES] [-t TITLE] [-k fail|always|never] [-K MIN] [--reap] [--keep-on-cancel]
#                     [--size N%] --pane STRING [[--size N%] --pane STRING ...]
#        tmux-pane.sh [opts] -- CMD [ARGS...]          (one leader pane running the command)
#        tmux-pane.sh cancel [PANE|TITLE|--all]
#        tmux-pane.sh list
#        tmux-pane.sh close [--kept|PANE]
#        tmux-pane.sh equalize PANE [--closing]
#
#   -d DIR    working directory for the commands (default: the current one)
#   -n LINES  how many trailing leader-pane lines to print when it ends (default 60)
#   -t TITLE  leader pane title (default: the command name, or `bash` with --pane)
#   -k KEEP   when to keep the panes open after the leader ends, until Enter is pressed in the leader:
#             fail (default) only when it exits non-zero, always, or never
#   -K MIN    kept panes close themselves after MIN minutes (default: they stay until Enter or `close`)
#   --reap    when the leader ends (or its pane vanishes), also TERM then KILL any process it left running
#   --keep-on-cancel
#             a run stopped by `cancel` is handled per -k like any other (default: the panes close, whatever -k says)
#   --pane STRING
#             repeatable; STRING runs via `bash -c` in its own pane.  The first --pane is the leader (the caller blocks
#             on it and exits with its code); the rest are followers, stacked below it in argument order.  Followers
#             are not tracked or waited on; they close with the leader.  -- CMD is shorthand for one leader pane.
#   --size N% applies to the next --pane (a follower only): its height as a percentage of the leader's column when it is
#             created.  The first follower defaults to 40%; later ones without --size split the follower area evenly.
#
# The panes open to the right without taking focus.  Every pane gets TMUX_PANE_RUN_ID (the leader's pane id) and
# TMUX_PANE_RUN_CANCEL (the shell command that cancels the run).  When the leader finishes this prints the pane id, the
# last LINES lines of the leader pane, the orphan report and `exit=N`, closes the panes unless -k says to keep them, and
# exits with the leader's code.  Exits 2 when not inside tmux or on a usage error (`-f` was removed).  Exits 129 (after
# printing `pane gone`) when the leader pane is killed or closed before the command ends.
#
# The leader runs in its own process group.  A registry under ${TMPDIR}/tmux-pane/ (one .reg file per pane, plus the
# .pids list of every descendant seen, sampled about every 2 s) lets `cancel` stop it from any shell: INT to the group,
# then TERM, then KILL, each after TMUX_PANE_GRACE seconds (default 10), then it reaps stragglers.  A cancelled run
# closes its panes (leader and followers) after printing its output, unless --keep-on-cancel.  Whenever a run ends,
# processes it started that are still alive (same PID and same start time, so a reused PID is never hit) are listed with
# their listening TCP ports as orphans.  Reaping (cancel, or --reap) signals those exact PIDs only, never a pattern.
# `list` shows this script's panes (id, title, state running|kept|exit N|follower, age); `close` closes finished ones,
# with their followers.  The window's columns (the pane the script was started from, plus each leader with its followers
# stacked below) are kept at equal widths: re-sized when a leader opens and when its panes close (best effort, a failed
# resize only warns).  `equalize PANE` does that by hand for PANE's window; `--closing` leaves PANE itself out.  `cancel`,
# `list`, `close` and `equalize` are reserved first words, so give a command that is named that after `--`.
#
# Edit this file atomically (write a temp file, then mv it over): a running bash reads the script as it goes.
set -uo pipefail

self=$(cd "$(dirname "$0")" && pwd)/$(basename "$0")
regdir=${TMPDIR:-/tmp}/tmux-pane
grace=${TMUX_PANE_GRACE:-10}
tab=$'\t'

usage() { awk '/^# \*+$/ && length($0) > 40 {n++; next} n >= 2 && /^# Edit this file/ {exit} n >= 2' "$0" >&2; exit 2; }

# --- process helpers (PIDs are only ever acted on together with their recorded start time) ---

lstart() { ps -o lstart= -p "$1" 2>/dev/null | sed -e 's/^ *//' -e 's/ *$//'; }

# True when the process exists and is not a zombie.
alive() { local s; s=$(ps -o stat= -p "$1" 2>/dev/null | tr -d ' '); [ -n "$s" ] && [ "${s#Z}" = "$s" ]; }

descend() { local c; echo "$1"; for c in $(pgrep -P "$1" 2>/dev/null); do descend "$c"; done; }

# Appends every not-yet-recorded descendant of $1 (and $1 itself) to the pids file $2: pid, start time, command line.
track() {
	local p ls
	for p in $(descend "$1"); do
		ls=$(lstart "$p")
		[ -n "$ls" ] || continue
		awk -F'\t' -v p="$p" -v l="$ls" '$1==p && $2==l {f=1} END {exit !f}' "$2" 2>/dev/null && continue
		printf '%s\t%s\t%s\n' "$p" "$ls" "$(ps -o command= -p "$p" 2>/dev/null | cut -c1-200)" >> "$2"
	done
}

# Lists the recorded processes that are still alive with the same start time; with $2=1, TERMs then KILLs those PIDs.
report() {
	local file=$1 reap=$2 pid ls cmd ports n=0 e i left
	local -a live=()
	[ -s "$file" ] || return 0
	while IFS=$'\t' read -r pid ls cmd; do
		alive "$pid" && [ "$(lstart "$pid")" = "$ls" ] || continue
		ports=$(lsof -nP -a -p "$pid" -iTCP -sTCP:LISTEN -Fn 2>/dev/null | sed -n 's/^n//p' | paste -sd, -)
		[ $n = 0 ] && echo "orphans (started by the command, still running):"
		n=$((n + 1))
		echo "  pid $pid  $cmd${ports:+  [listening: $ports]}"
		live+=("$pid$tab$ls")
	done < "$file"
	[ "$reap" = 1 ] && [ $n -gt 0 ] || return 0
	for sig in TERM KILL; do
		for e in "${live[@]}"; do
			pid=${e%%$tab*}; ls=${e#*$tab}
			alive "$pid" && [ "$(lstart "$pid")" = "$ls" ] && kill -s "$sig" "$pid" 2>/dev/null
		done
		[ $sig = KILL ] && sleep 0.3
		for ((i = 0; i < grace * 4; i++)); do
			left=0
			for e in "${live[@]}"; do
				pid=${e%%$tab*}; ls=${e#*$tab}
				alive "$pid" && [ "$(lstart "$pid")" = "$ls" ] && left=$((left + 1))
			done
			[ $left = 0 ] && break
			[ $sig = KILL ] && break
			sleep 0.25
		done
		[ $left = 0 ] && break
	done
	if [ $left = 0 ]; then echo "  reaped $n process(es)"; else echo "  $left process(es) survived KILL"; fi
}

# --- registry helpers ---

# This script's registry directory, plus any other one named by a tagged pane (a shell with another TMPDIR can still find it).
regdirs() {
	echo "$regdir"
	tmux list-panes -a -F '#{@tmux_reg}' 2>/dev/null | sort -u | grep -v '^$' | grep -vxF "$regdir"
}

reg_files() {
	local d f
	while IFS= read -r d; do
		for f in "$d"/*.reg; do [ -e "$f" ] && echo "$f"; done
	done < <(regdirs)
}

group_gone() { ! kill -0 -- "-$1" 2>/dev/null; }

# Runs in a subshell.  0 = handled, 3 = $1 is not the target, 4 = nothing to cancel.
cancel_one() {
	local f=$1 target=$2 id cur sig i pidsfile copy
	. "$f"
	if [ "$target" != --all ]; then
		[ "$pane" = "$target" ] || [ "$pane" = "%${target#%}" ] || [ "$title" = "$target" ] || return 3
	fi
	id=${pane#%}
	pidsfile=$rd/$id.pids
	echo "cancel: $pane ($title)"
	if [ -z "${pgid:-}" ]; then echo "  not started yet; nothing to signal"; return 4; fi
	cur=$(lstart "$cpid")
	if [ -n "$cur" ] && [ "$cur" != "$cpidstart" ]; then echo "  pid $cpid was reused; not touching it"; return 4; fi
	# The caller can't test the .cancel flag once this finishes (it is removed), so it learns of the cancel from this option.
	tmux set-option -p -t "$pane" @tmux_cancelled 1 2>/dev/null
	touch "$rd/$id.cancel"
	copy=$(mktemp "${TMPDIR:-/tmp}/tmux-pane-cancel.XXXXXX")
	cp "$pidsfile" "$copy" 2>/dev/null
	if group_gone "$pgid"; then
		echo "  process group $pgid already gone"
	else
		for sig in INT TERM KILL; do
			kill -s "$sig" -- "-$pgid" 2>/dev/null
			echo "  sent $sig to process group $pgid"
			[ $sig = KILL ] && sleep 0.3 && break
			for ((i = 0; i < grace * 4; i++)); do group_gone "$pgid" && break; sleep 0.25; done
			group_gone "$pgid" && break
		done
		group_gone "$pgid" && echo "  process group stopped" || echo "  process group $pgid survived KILL"
	fi
	[ ! -f "$pidsfile" ] || cp "$pidsfile" "$copy"
	report "$copy" 1
	rm -f "$copy" "$rd/$id.cancel"
	return 0
}

cmd_cancel() {
	local target=${1:-} files f n r matched=0
	files=$(reg_files)
	[ -n "$files" ] || { echo "cancel: no tmux-pane.sh runs are registered"; return 1; }
	if [ -z "$target" ]; then
		n=$(echo "$files" | wc -l | tr -d ' ')
		if [ "$n" != 1 ]; then
			echo "cancel: $n runs are registered; name one (pane id or title) or use --all:" >&2
			while IFS= read -r f; do ( . "$f"; echo "  $pane  $title" >&2 ); done <<< "$files"
			return 2
		fi
		target=--all
	fi
	while IFS= read -r f; do
		( cancel_one "$f" "$target" )
		r=$?
		[ $r = 3 ] || matched=$((matched + 1))
	done <<< "$files"
	[ $matched -gt 0 ] || { echo "cancel: no registered run matches '$target'" >&2; return 1; }
}

# --- list / close (panes are found by the @tmux_pane=1 pane option) ---

pane_rows() {
	tmux list-panes -a -F "#{@tmux_pane}|#{pane_id}|#{@tmux_state}|#{@tmux_parent}|#{@tmux_start}|#{@tmux_title}" 2>/dev/null |
		awk -F'|' '$1 == "1"'
}

cmd_list() {
	local now n one p st par start ttl age d
	now=$(date +%s)
	n=0
	while IFS='|' read -r one p st par start ttl; do
		n=$((n + 1))
		d=$((now - ${start:-$now}))
		if [ $d -ge 3600 ]; then age="$((d / 3600))h$((d % 3600 / 60))m"; elif [ $d -ge 60 ]; then age="$((d / 60))m"; else age="${d}s"; fi
		[ "$st" = follower ] && ttl="follower of $par"
		printf '%-6s %-8s %-6s %s\n' "$p" "$st" "$age" "$ttl"
	done < <(pane_rows)
	[ $n -gt 0 ] || echo "no tmux-pane.sh panes"
}

# Gives the window's columns equal widths: the agent pane ($2) plus every leader (a follower shares its leader's column).
# $3 is a pane to leave out (one about to close).  Each column is resized through one pane with `resize-pane -x`, left to
# right; the last takes what is left.  Only the width of the columns involved is shared out, so panes not opened by this
# script keep their space.  Best effort: a failure warns on stderr and never changes the exit code.
equalize() {
	local win=$1 agent=$2 excl=${3:-} cols n=0 total=0 w i l p pw rest
	local -a ids=()
	cols=$(tmux list-panes -t "$win" -F "#{pane_left}|#{pane_id}|#{pane_width}|#{@tmux_pane}|#{@tmux_state}" 2>/dev/null) ||
		{ echo "tmux-pane.sh: warning: could not list panes of $win to equalize widths" >&2; return 0; }
	cols=$(echo "$cols" | awk -F'|' -v a="$agent" -v x="$excl" '$2 != x && ($2 == a || ($4 == "1" && $5 != "follower"))' | sort -t'|' -k1,1n)
	[ -n "$cols" ] || return 0
	while IFS='|' read -r l p pw rest; do
		ids+=("$p"); n=$((n + 1)); total=$((total + pw))
	done <<< "$cols"
	[ $n -ge 2 ] || return 0
	w=$((total / n))
	for ((i = 0; i < n - 1; i++)); do
		tmux resize-pane -x "$w" -t "${ids[i]}" 2>/dev/null || echo "tmux-pane.sh: warning: could not resize ${ids[i]} to $w columns" >&2
	done
	return 0
}

cmd_equalize() {
	local target=${1:-} info
	[ -n "$target" ] || usage
	info=$(tmux display-message -p -t "$target" '#{window_id}|#{@tmux_agent}' 2>/dev/null) ||
		{ echo "equalize: $target is not a tmux pane" >&2; return 1; }
	[ "${info%%|*}" != "" ] || { echo "equalize: $target is not a tmux pane" >&2; return 1; }
	if [ "${2:-}" = --closing ]; then equalize "${info%%|*}" "${info#*|}" "$target"; else equalize "${info%%|*}" "${info#*|}"; fi
}

cmd_close() {
	local target=${1:---kept} one p st par start ttl closed=0 q w
	local rows; rows=$(pane_rows)
	local -a wins=()
	if [ "$target" != --kept ]; then
		target="%${target#%}"
		st=$(echo "$rows" | awk -F'|' -v p="$target" '$2 == p && $3 != "follower" {print $3}')
		[ -n "$st" ] || { echo "close: $target is not a tmux-pane.sh pane" >&2; return 1; }
		[ "$st" != running ] || { echo "close: $target is still running; use: tmux-pane.sh cancel $target" >&2; return 1; }
	fi
	while IFS='|' read -r one p st par start ttl; do
		[ "$one" = 1 ] || continue
		if [ "$st" = follower ]; then
			# A follower goes with its leader; one whose leader is gone or finished goes too.
			q=$(echo "$rows" | awk -F'|' -v p="$par" '$2 == p {print $3}')
			if [ "$target" = --kept ]; then [ "$q" != running ] || continue; else [ "$par" = "$target" ] || continue; fi
		else
			[ "$st" != running ] || continue
			[ "$target" = --kept ] || [ "$p" = "$target" ] || continue
		fi
		[ "$st" = follower ] || wins+=("$(tmux display-message -p -t "$p" '#{window_id}|#{@tmux_agent}' 2>/dev/null)")
		tmux kill-pane -t "$p" 2>/dev/null && { closed=$((closed + 1)); echo "closed $p"; }
	done <<< "$rows"
	while IFS= read -r w; do
		[ -n "$w" ] && equalize "${w%%|*}" "${w#*|}"
	done < <(printf '%s\n' ${wins[@]+"${wins[@]}"} | sort -u)
	[ $closed -gt 0 ] || echo "nothing to close"
}

case ${1:-} in
	cancel) shift; cmd_cancel "$@"; exit $? ;;
	list) cmd_list; exit $? ;;
	close) shift; cmd_close "$@"; exit $? ;;
	equalize) shift; cmd_equalize "$@"; exit $? ;;
esac

# --- arguments (getopts has no long options, so the words are parsed by hand) ---

dir=$PWD lines=60 title= keep=fail keepmin= reap=0 keeponcancel=0 pending= cmdmode=0
panes=() sizes=()

set_opt() {
	case $1 in
		-d) dir=$2 ;;
		-n) lines=$2 ;;
		-t) title=$2 ;;
		-k) keep=$2 ;;
		-K) keepmin=$2 ;;
	esac
}

while [ $# -gt 0 ]; do
	case $1 in
		--) shift; cmdmode=1; break ;;
		--reap) reap=1 ;;
		--keep-on-cancel) keeponcancel=1 ;;
		--pane) [ $# -ge 2 ] || usage; panes+=("$2"); sizes+=("$pending"); pending=; shift ;;
		--size)
			[ $# -ge 2 ] || usage
			n=${2%\%}
			case $n in ''|*[!0-9]*) usage ;; esac
			[ "$n" != "$2" ] && [ "$n" -ge 1 ] && [ "$n" -le 99 ] || usage
			pending=$n
			shift ;;
		-f) echo "tmux-pane.sh: -f was removed; use watch-run.sh to tail the log" >&2; exit 2 ;;
		-[dntkK]) [ $# -ge 2 ] || usage; set_opt "$1" "$2"; shift ;;
		-[dntkK]?*) set_opt "${1:0:2}" "${1:2}" ;;
		-*) usage ;;
		*) cmdmode=1; break ;;
	esac
	shift
done
if [ $cmdmode = 1 ]; then
	[ ${#panes[@]} -eq 0 ] && [ $# -gt 0 ] || usage
else
	[ ${#panes[@]} -gt 0 ] && [ $# -eq 0 ] || usage
fi
[ -z "$pending" ] || usage
[ -z "${sizes[0]:-}" ] || usage
case $keep in fail|always|never) ;; *) usage ;; esac
case $keepmin in ''|*[!0-9]*) [ -z "$keepmin" ] || usage ;; esac
[ -n "${TMUX:-}" ] || { echo "tmux-pane.sh: not inside tmux" >&2; exit 2; }

if [ $cmdmode = 1 ]; then
	cmdline=$*
	cmd=$(printf '%q ' "$@")
	[ -n "$title" ] || title=$(basename "$1")
else
	cmdline=${panes[0]}
	cmd="bash -c $(printf '%q' "${panes[0]}")"
	[ -n "$title" ] || title=bash
fi

state=$(mktemp -d "${TMPDIR:-/tmp}/tmux-pane.XXXXXX")
chan="tmux-pane-$$-$RANDOM"
# The panes' shells get the tmux server's environment, not ours, so pass the caller's env through explicitly (one file,
# shared by the leader and every follower).
env -0 > "$state/env"

# The leader always waits for Enter after signalling; the caller captures its output first, then kills the panes if they
# shouldn't be kept, so nothing is lost to a race between the command ending and the capture.  The command runs as a
# foreground job under `set -m`, so it is its own process group (and keeps Ctrl-C), and the sh wrapper records its PID
# before exec'ing it, so the PID is the group id.  `mypane` is read first because the env import overwrites TMUX_PANE.
# A cancel sets @tmux_cancelled on this pane first, so the message below can say why the pane is there.
readcmd='read -r'
[ -z "$keepmin" ] || readcmd="read -r -t $((keepmin * 60))"
script=$(cat <<SCRIPT
mypane=\$TMUX_PANE
cd $(printf '%q' "$dir") || exit 1
while IFS= read -r -d '' kv; do export "\$kv" 2>/dev/null; done < $(printf '%q' "$state/env")
export TMUX_PANE=\$mypane TMUX_PANE_RUN_ID=\$mypane
export TMUX_PANE_RUN_CANCEL="$(printf '%q' "$self") cancel \$mypane"
printf '\\033]2;%s\\033\\\\' $(printf '%q' "$title")
set -m
sh -c 'echo \$\$ > "\$1"; shift; exec "\$@"' sh $(printf '%q' "$state/cpid") $cmd
rc=\$?
echo \$rc > $(printf '%q' "$state/exit")
tmux set-option -p -t "\$mypane" @tmux_state "exit \$rc" 2>/dev/null
cancelled=\$(tmux show-options -pqv -t "\$mypane" @tmux_cancelled 2>/dev/null)
tmux wait-for -S $chan
if [ -n "\$cancelled" ]; then
	printf '\\n[cancelled, exit %s] stopped by tmux-pane.sh cancel; press Enter to close this pane\\n' "\$rc"
else
	printf '\\n[exit %s] press Enter to close this pane\\n' "\$rc"
fi
$readcmd
for t in \$(tmux list-panes -a -F '#{pane_id} #{@tmux_parent}' | awk -v p="\$mypane" '\$2 == p {print \$1}'); do tmux kill-pane -t \$t; done
$(printf '%q' "$self") equalize \$mypane --closing
SCRIPT
)

agent=${TMUX_PANE:-}
pane=$(tmux split-window -h -d -l 50% -P -F '#{pane_id}' "bash -c $(printf '%q' "$script")") || exit 2
win=$(tmux display-message -p -t "$pane" '#{window_id}' 2>/dev/null)
echo "pane $pane: $title"
echo "cancel: $self cancel $pane"

id=${pane#%}
mkdir -p "$regdir"
reg=$regdir/$id.reg pids=$regdir/$id.pids cancelflag=$regdir/$id.cancel
started=$(date +%s)
tmux set-option -p -t "$pane" @tmux_pane 1 \; set-option -p -t "$pane" @tmux_title "$title" \; \
	set-option -p -t "$pane" @tmux_state running \; set-option -p -t "$pane" @tmux_start "$started" \; \
	set-option -p -t "$pane" @tmux_reg "$regdir" \; set-option -p -t "$pane" @tmux_agent "$agent" 2>/dev/null
[ -z "$win" ] || equalize "$win" "$agent"
pgid= cpid= cpidstart=
write_reg() {
	{
		printf 'pane=%q\ntitle=%q\ncallerpid=%q\ncmdline=%q\nstarted=%q\nrd=%q\n' "$pane" "$title" "$$" "$cmdline" "$started" "$regdir"
		[ -z "$pgid" ] || printf 'pgid=%q\ncpid=%q\ncpidstart=%q\n' "$pgid" "$cpid" "$cpidstart"
	} > "$reg.tmp" && mv "$reg.tmp" "$reg"
}
write_reg
: > "$pids"
trap 'rm -f "$reg" "$reg.tmp" "$pids" "$cancelflag"' EXIT

# --- followers: stacked below the leader in argument order, not tracked or waited on ---

followers=()
allunsized=1

pane_h() { local h; h=$(tmux display-message -p -t "$1" '#{pane_height}' 2>/dev/null); echo "${h:-0}"; }

add_follower() {
	local str=$1 size=$2 target=$pane f k lh colh region want th fp fscript i each
	local -a live=()
	lh=$(pane_h "$pane")
	colh=$lh
	# Followers that have already ended (a quick command) are not part of the column any more.
	for f in ${followers[@]+"${followers[@]}"}; do
		tmux display-message -p -t "$f" '#{pane_id}' >/dev/null 2>&1 || continue
		live+=("$f")
		colh=$((colh + $(pane_h "$f") + 1))
		target=$f
	done
	k=${#live[@]}
	region=$((colh - lh - 1))
	if [ -n "$size" ]; then want=$((colh * size / 100)); allunsized=0
	elif [ $k = 0 ]; then want=$((colh * 40 / 100))
	else want=$(((region - k) / (k + 1)))
	fi
	th=$(pane_h "$target")
	[ $want -le $((th - 2)) ] || want=$((th - 2))
	[ $want -ge 1 ] || { echo "tmux-pane.sh: no room for another pane; skipped: $str" >&2; return; }
	fscript=$(cat <<FSCRIPT
mypane=\$TMUX_PANE
cd $(printf '%q' "$dir") || exit 1
while IFS= read -r -d '' kv; do export "\$kv" 2>/dev/null; done < $(printf '%q' "$state/env")
export TMUX_PANE=\$mypane TMUX_PANE_RUN_ID=$(printf '%q' "$pane")
export TMUX_PANE_RUN_CANCEL=$(printf '%q' "$(printf '%q' "$self") cancel $pane")
exec bash -c $(printf '%q' "$str")
FSCRIPT
)
	fp=$(tmux split-window -v -d -l "$want" -t "$target" -P -F '#{pane_id}' "bash -c $(printf '%q' "$fscript")" 2>/dev/null) || return
	followers+=("$fp")
	tmux set-option -p -t "$fp" @tmux_pane 1 \; set-option -p -t "$fp" @tmux_state follower \; \
		set-option -p -t "$fp" @tmux_parent "$pane" \; set-option -p -t "$fp" @tmux_start "$(date +%s)" \; \
		set-option -p -t "$fp" @tmux_reg "$regdir" 2>/dev/null
	# With no explicit sizes the follower area is shared evenly, however many there are.
	if [ $allunsized = 1 ] && [ $k -ge 1 ]; then
		each=$(((region - k) / (k + 1)))
		for ((i = 0; i < k; i++)); do tmux resize-pane -y "$each" -t "${live[i]}" 2>/dev/null; done
	fi
}

for ((i = 1; i < ${#panes[@]}; i++)); do add_follower "${panes[i]}" "${sizes[i]}"; done

# Closes the leader and every follower (a missing pane is fine).
kill_all() {
	local p
	for p in "$pane" ${followers[@]+"${followers[@]}"}; do tmux kill-pane -t "$p" 2>/dev/null; done
	[ -z "$win" ] || equalize "$win" "$agent"
}

# Wait for the pane's signal, but notice the pane disappearing (it would never signal) and sample the process tree.
tmux wait-for "$chan" &
waiter=$!
gone=0 tick=0 sawcancel=0
while kill -0 "$waiter" 2>/dev/null; do
	sleep 0.2
	tick=$((tick + 1))
	[ ! -e "$cancelflag" ] || sawcancel=1
	if [ -z "$pgid" ] && [ -s "$state/cpid" ]; then
		cpid=$(cat "$state/cpid")
		pgid=$cpid cpidstart=$(lstart "$cpid")
		write_reg
		track "$cpid" "$pids"
	fi
	if [ $((tick % 10)) = 0 ]; then
		[ -z "$cpid" ] || track "$cpid" "$pids"
		if ! tmux list-panes -a -F '#{pane_id}' 2>/dev/null | grep -qxF "$pane"; then gone=1; break; fi
	fi
done
[ ! -e "$cancelflag" ] || sawcancel=1
# A cancel in progress does the reaping and the reporting; give it time to finish so the two don't race.
for ((i = 0; i < (grace * 2 + 5) * 5 && i >= 0; i++)); do [ -e "$cancelflag" ] || break; sleep 0.2; done
if [ $gone = 1 ]; then
	kill "$waiter" 2>/dev/null
	wait "$waiter" 2>/dev/null
	echo "pane gone"
	report "$pids" "$reap"
	kill_all
	rm -rf "$state"
	exit 129
fi
wait "$waiter" 2>/dev/null
rc=$(cat "$state/exit" 2>/dev/null || echo 1)
# The cancel sets the pane option before it signals anything; the flag seen while polling is only a fallback.
cancelled=$(tmux show-options -pqv -t "$pane" @tmux_cancelled 2>/dev/null)
[ -n "$cancelled" ] || { [ $sawcancel = 1 ] && cancelled=1; }
tmux capture-pane -p -J -t "$pane" -S "-$lines" | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}'
report "$pids" "$reap"
echo "exit=$rc"
if [ -n "$cancelled" ] && [ $keeponcancel = 0 ]; then
	kill_all
	echo "pane $pane closed (cancelled)"
elif [ "$keep" = never ] || { [ "$keep" = fail ] && [ "$rc" = 0 ]; }; then
	kill_all
	echo "pane $pane closed"
else
	if [ -n "$cancelled" ]; then
		tmux set-option -p -t "$pane" @tmux_state "kept (cancelled, exit $rc)" 2>/dev/null
	else
		tmux set-option -p -t "$pane" @tmux_state "kept (exit $rc)" 2>/dev/null
	fi
	echo "pane $pane kept open (press Enter in it to close, or: $self close $pane)"
fi
rm -rf "$state"
exit "$rc"
