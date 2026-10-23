# slack-notify.py

Posts a text message to a Slack Workflow Builder webhook so it arrives as a real push notification, and prints the send time so a caller can find the post.

## When to use

- A long task is done, or needs a decision, and you want a push to your phone.
- A caller needs to post a message and then read replies in the same thread.

## Usage

```bash
python3 scripts/slack-notify.py "Build finished."
echo "Some message" | python3 scripts/slack-notify.py -
```

Words after the script name are joined with spaces. A lone `-` reads the message from stdin.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `message` | text (zero or more words; `-` alone for stdin) | none, required in practice | Message text. Empty after trimming exits 3. |

## What it does

1. Builds the message from the arguments, or from stdin when the only argument is `-` (`_read_message`).
2. Reads `notify_webhook_url` from `.cursor/slack.local.yml` in the repository root with a small built-in parser, so PyYAML is not needed (`_load_webhook_url`).
3. Requires the URL to start with `https://hooks.slack.com/triggers/`. A `https://slack.com/shortcuts/...` URL means the workflow was created with the wrong trigger type.
4. Records the time, then POSTs `{"text": "<message>"}` as JSON with a 15 second timeout.
5. On HTTP 200, prints `T0=<unix seconds>`.

## Output and exit codes

Stdout on success is one line, `T0=<float>`. Errors go to stderr.

| Code | Meaning |
|---|---|
| 0 | The webhook accepted the payload. |
| 1 | `notify_webhook_url` is missing from the config, or does not look like a Workflow Builder webhook URL. |
| 2 | The webhook returned an HTTP error, a non-200 status, or the connection failed. |
| 3 | No message was given. |

## Gotchas

- The webhook response carries no message timestamp. To thread-reply or poll for an answer, a caller waits a few seconds, reads the newest messages in the notifications channel, and picks the bot-authored one whose timestamp is at least `T0 - 5`.
- The workflow posts as the workflow app, not as you, which is why a push is delivered; messages you author yourself do not push to you.
- In the workflow, the trigger must be "From a webhook" and the "Send a message" step must target the notifications channel, not you. See `.cursor/slack.local.example.yml` if your checkout has it.
- `.cursor/slack.local.yml` is a local, per-person file and is not committed.

## See also

None.
