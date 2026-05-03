#!/usr/bin/env python3
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
"""
Slack notification helper for Apache Juneau.

Posts a text payload to the Slack Workflow Builder webhook configured in
``.cursor/slack.local.yml`` (key ``notify_webhook_url``). The workflow then
posts the message into your personal notifications channel (the channel
configured as the "Send a message" recipient in the workflow — by
convention the same channel you set as ``default_channel_id``).

Why this exists
===============

Messages sent through the ``user-slack`` MCP are authored as *you*, and
Slack deliberately suppresses pushes for messages the recipient sent
themselves. A Slack Workflow Builder webhook posts from the workflow app's
identity instead, which does push on iOS/Android — same code path as a
message from any other human or app you share a channel with.

Because the workflow posts to a real channel (not a DM), the message has a
``message_ts`` you can thread-reply to. That's what makes "ask me on Slack
and wait" round-trips work: the push wakes your phone, you tap to open
Slack, reply in the thread, and Cursor polls that same thread for the
reply — one message, one thread, one conversation.

This is the backing script for:

- ``/slack notify <message>`` in ``.cursor/commands/slack.md`` (fire-and-
  forget push).
- The "Notify me on Slack when done" / "Ask me on Slack and wait" trigger
  phrases in ``AGENTS.md``. The wait-loop path combines this script with a
  subsequent ``slack_read_channel`` lookup to resolve the posted message's
  ``message_ts``.

Matching the posted message
===========================

The Workflow Builder webhook response doesn't include a ``message_ts`` for
the message the workflow ends up posting, so callers that need the
timestamp (``/slack notify``, ``/slack ask``, the AGENTS.md wait loop)
have to locate the post in the channel after the fact. This script prints
the *pre-send* Unix timestamp on stdout as ``T0=<float>`` on success.
Callers should:

1. Sleep ~3–5 seconds to let Slack's workflow engine run.
2. ``slack_read_channel`` on ``default_channel_id`` (limit 5).
3. Pick the newest message authored by a bot (author id starts with ``B``)
   whose ``message_ts`` is ``>= T0 - 5`` (small jitter tolerance). That's
   the workflow-posted message.

No token is appended to the message body — the post goes out clean.

Usage
=====

::

    python3 scripts/slack-notify.py "Build finished."
    echo "Some message" | python3 scripts/slack-notify.py -

Exit codes
==========

:0: webhook accepted the payload (HTTP 200); ``T0=<float>`` printed on stdout.
:1: webhook URL not configured in ``slack.local.yml``.
:2: HTTP error posting to the webhook.
:3: missing message argument.

Setup
=====

See ``.cursor/slack.local.example.yml`` for the full Slack Workflow
Builder setup (trigger type **must** be "From a webhook"; the "Send a
message" step recipient **must** be your ``default_channel_id``
notifications channel, not yourself).
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CONFIG_PATH = REPO_ROOT / ".cursor" / "slack.local.yml"


def _load_webhook_url() -> str | None:
	"""Extract notify_webhook_url from slack.local.yml without requiring PyYAML.

	The config file is a flat, single-line key/value YAML; a tiny parser is
	enough and avoids adding a dependency for a one-line read.
	"""
	if not CONFIG_PATH.exists():
		return None
	for raw in CONFIG_PATH.read_text(encoding="utf-8").splitlines():
		line = raw.strip()
		if not line or line.startswith("#"):
			continue
		if ":" not in line:
			continue
		key, _, value = line.partition(":")
		if key.strip() != "notify_webhook_url":
			continue
		value = value.strip()
		if value.startswith('"') and value.endswith('"'):
			value = value[1:-1]
		elif value.startswith("'") and value.endswith("'"):
			value = value[1:-1]
		return value or None
	return None


def _read_message(args: argparse.Namespace) -> str | None:
	if not args.message:
		return None
	if len(args.message) == 1 and args.message[0] == "-":
		text = sys.stdin.read()
		return text.strip() or None
	return " ".join(args.message).strip() or None


def _parse_args(argv: list[str]) -> argparse.Namespace:
	p = argparse.ArgumentParser(
		prog="slack-notify.py",
		description="POST a message to the Slack Workflow Builder webhook in .cursor/slack.local.yml.",
	)
	p.add_argument(
		"message",
		nargs="*",
		help="Message text. Pass '-' alone to read from stdin.",
	)
	return p.parse_args(argv[1:])


def main(argv: list[str]) -> int:
	args = _parse_args(argv)
	message = _read_message(args)
	if not message:
		print("error: no message provided (pass as arg or '-' for stdin)", file=sys.stderr)
		return 3
	url = _load_webhook_url()
	if not url:
		print(
			"error: notify_webhook_url not set in .cursor/slack.local.yml\n"
			"       see .cursor/slack.local.example.yml for setup steps",
			file=sys.stderr,
		)
		return 1
	if not url.startswith("https://hooks.slack.com/triggers/"):
		print(
			f"error: notify_webhook_url does not look like a Workflow Builder webhook URL\n"
			f"       got: {url}\n"
			f"       expected: https://hooks.slack.com/triggers/...\n"
			f"       if yours starts with https://slack.com/shortcuts/... the workflow\n"
			f"       was created with the wrong trigger type; recreate with 'From a webhook'",
			file=sys.stderr,
		)
		return 1
	t0 = time.time()
	payload = json.dumps({"text": message}).encode("utf-8")
	req = urllib.request.Request(
		url,
		data=payload,
		headers={"Content-Type": "application/json"},
		method="POST",
	)
	try:
		with urllib.request.urlopen(req, timeout=15) as resp:
			body = resp.read().decode("utf-8", errors="replace").strip()
			if resp.status != 200:
				print(f"error: webhook returned HTTP {resp.status}: {body}", file=sys.stderr)
				return 2
			print(f"T0={t0:.3f}")
			return 0
	except urllib.error.HTTPError as e:
		detail = e.read().decode("utf-8", errors="replace").strip()
		print(f"error: webhook HTTPError {e.code}: {detail}", file=sys.stderr)
		return 2
	except urllib.error.URLError as e:
		print(f"error: webhook URLError: {e.reason}", file=sys.stderr)
		return 2


if __name__ == "__main__":
	sys.exit(main(sys.argv))
