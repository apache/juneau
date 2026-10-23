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
Posts a text message to a Slack Workflow Builder webhook so it arrives as a real push notification, and prints the send time so a caller can find the post.

Full docs: slack-notify.md
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
