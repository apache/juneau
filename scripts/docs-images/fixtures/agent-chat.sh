#!/bin/bash
# Prints a short, fixed AI-agent conversation and waits (to be killed).
e=$'\e'
printf '%s\n' "${e}[1mAgent${e}[0m"
printf '\n'
printf '%s\n' "${e}[36m>${e}[0m Run the build and tell me if the tests pass."
printf '\n'
printf '%s\n' "${e}[2mI'll start it in a side pane so you can watch it.${e}[0m"
printf '\n'
printf '%s\n' "${e}[32m*${e}[0m Bash(tmux split-window -h -d ...)"
printf '%s\n' "${e}[32m*${e}[0m Bash(tmux wait-for build-done)"
printf '%s\n' "  ${e}[2mwaiting for the run to finish...${e}[0m"
exec sleep 600
