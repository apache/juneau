# TODO-26: Personal Slack bot for Cursor agent alerts and Q&A

Source: created 2026-04-19.

## Goal

Provide a **reusable Slack bot + Python CLI** so a Cursor agent (or shell automation) can:

1. **Notify** a user in a **dedicated personal Slack channel** (e.g. `#james-bognar-claude-alerts`).
2. **Ask a question** and either **stop** (fire-and-forget notification) or **wait** for an answer by **polling** Slack (no public inbound webhook required for the developer machine).

**Design constraints**

- **Per-message channel** — each invocation passes a channel (ID strongly preferred; see below) so **one bot** (one `SLACK_BOT_TOKEN`) can post to **one or more channels** in the same Slack workspace.
- **Per-user defaults** — optional config maps **local username** or **`CURSOR_SLACK_DEFAULT_CHANNEL`** to a default channel when `--channel` is omitted.
- **Secrets never in git** — bot token **only** via environment variable **`SLACK_BOT_TOKEN`** (no file-based token, no keychain in v1). Optional JSON may hold **channel defaults only**, never the token.
- **Single Slack workspace** — one installed app, one bot token; no Enterprise Grid / multi-workspace token switching in v1.
- **Reusability** — new users adopt the feature by **copying starter files** (script, optional requirements, Cursor command doc) from a **committed template location** in this repo into their own **local `.cursor/`** (or `~/.cursor/`). See [Starter kit](#starter-kit-for-other-workspaces) below.
- **Thread-only Q&A** — `ask` starts a **thread**; `wait-reply` only considers **replies in that thread** (`conversations.replies`). Answers posted only in the channel (not in the thread) are **ignored** by design.
- **Rich messages (v1)** — **`notify`** and **`ask`** send **[Block Kit](https://api.slack.com/block-kit)** payloads (`blocks` + optional `text` fallback for notifications and accessibility). No “plain text only” phase.

**Non-goals (v1)**

- Hosting a public HTTP endpoint for Slack **Events API** (optional later for push instead of poll).
- Replacing Cursor’s own notification surfaces; this is an **opt-in** side channel.
- Multi-workspace / multi-org Slack bots.

---

## Slack app setup (one-time per workspace)

**Salesforce employees (internal Slack org, approvals required):** use the full walkthrough — scopes, approval materials, install order — in the **[slack-agent README](https://git.soma.salesforce.com/central-routing/slack-agent)** (“Salesforce internal Slack org”). The Juneau starter **`docs/cursor-slack-agent-starter/README.md`** links there and stays copy-paste focused.

**Short checklist (any workspace):**

1. Create a **Slack app** at [api.slack.com/apps](https://api.slack.com/apps) (from scratch is fine).
2. **Bot token scopes** (minimum starting set):
   - `chat:write` — post messages (and threads).
   - `channels:read` + `groups:read` (if private channels) — resolve channel **name → ID** if the CLI supports `--channel-name`.
   - `users:read` (optional) — enrich logs; not required for v1.
3. **Install app to workspace** (after any required org approvals); copy **Bot User OAuth Token** (`xoxb-...`). Store as **`SLACK_BOT_TOKEN`** (never commit).
4. **Invite the bot** to each personal channel (`/invite @YourBot` in `#james-bognar-claude-alerts`). Bots cannot post to channels they are not a member of.

**Channel ID vs name**

- Slack APIs prefer **channel IDs** (`C…` for public, `G…` for private multi-party). Names can be renamed; IDs are stable.
- v1 CLI should accept **`--channel C0123456789`** as the primary interface.
- Optional: **`--channel-name james-bognar-claude-alerts`** calls `conversations.list` (paginated) to resolve ID — slower and permission-sensitive; document limitations.

---

## Architecture

```
┌─────────────┐     subprocess      ┌──────────────────────────────┐
│ Cursor agent│ ──────────────────►│ .cursor/scripts/slack-agent.py │
└─────────────┘   SLACK_BOT_TOKEN   │  (local-only; see below)       │
       ▲                            └──────────────┬───────────────┘
       │ poll / exit code                           │ HTTPS
       │                                            ▼
       │                            ┌──────────────────────────────┐
       └────────────────────────────│ Slack Web API              │
                                    └──────────────────────────────┘
```

**Why polling (v1)**  
The agent runs on a laptop without a stable public URL. **Incoming webhooks** can only *post*; they cannot read answers. **Socket Mode** works but adds a long-lived process and complexity. **Polling `conversations.replies`** on the **`ask` message’s thread** after `chat.postMessage` returns `thread_ts` is simple and good enough for interactive Q&A. **Thread-only:** only messages in that thread count as answers.

**Optional v2**  
Slack **Events API** + ngrok or a small cloud relay to push replies into a local queue — only if polling latency or API rate limits become painful.

---

## Python CLI design

### Where the script lives (runtime vs templates)

**Runtime (personal, not Apache source):**

- **Preferred (workspace-local):** **`<repo>/.cursor/scripts/slack-agent.py`** alongside other personal Cursor material.
- **In this repository:** **`.cursor/` is listed in `.gitignore`**, so the **live** script and token-adjacent config stay **on your machine** and are **not committed** as Juneau source unless someone force-adds them.

**Templates (shared, safe to commit):**

- A **starter kit** directory (see [Starter kit](#starter-kit-for-other-workspaces)) holds **copy-pasteable** `slack-agent.py`, `requirements-slack.txt`, and **README** (approvals, CLI, **Using with Cursor**). New users (or new repos) **copy** the script into **their** `.cursor/scripts/` and optionally add a local **`.cursor/commands/slack-agent.md`** that points at or quotes the README. That is how we achieve **reusability across workspaces** without putting anyone’s bot token in git.

**Alternative (all projects on one machine):** **`~/.cursor/scripts/slack-agent.py`** — single copy; point commands at that path.

Committed **Juneau** `scripts/` should **not** be the primary home for the **personal** bot; use **`.cursor/`** / **`~/.cursor/`** at runtime, and a **committed starter** only where we document templates.

**Dependencies:** Prefer **`slack_sdk`** (`WebClient`). Install with **`pip install --user slack_sdk`** or keep a **local** `.cursor/scripts/requirements-slack.txt` beside the script (personal use only; not a committed Juneau artifact).

### Subcommands (proposed)

| Subcommand | Behavior |
|------------|----------|
| **`notify`** | `chat.postMessage` with **Block Kit**: **`blocks`** from **`--blocks-json`**, **`--blocks-file`**, or stdin (JSON array); optional top-level **`--text`** fallback for push/accessibility. If only `--text` is passed, CLI may wrap it in a minimal `section` + `mrkdwn` block for consistency. Exit `0` on success. |
| **`ask`** | Post the question as **`chat.postMessage`** with **blocks** (same input options as `notify`). The bot’s message becomes the **thread parent**; print **`thread_ts`** and **`channel`** to stdout (single-line JSON or `KEY=value`) for `wait-reply`. |
| **`wait-reply`** | Args: `--channel`, `--thread-ts`, `--timeout-seconds`, `--poll-interval-seconds`. Polls **`conversations.replies`** on that **thread only** until a **non-bot** message appears in the thread, or timeout. **Ignore** messages posted in the channel but not in the thread. **Stdout:** prefer extracting human-readable text (`message.text` and/or concatenation of `section` text fields from `blocks`); optional **`--reply-json`** to emit the full Slack message object for agents that want structure. Exit `1` on timeout, `0` on answer. Default **`--poll-interval-seconds` = 60**; override for testing. |
| **`resolve-channel`** | Optional: `--name foo` → print channel ID (helper for setup). |

**Block Kit helpers (recommended in the script)**

- Small Python helpers to build common blocks: **header**, **section** (`mrkdwn` / `plain_text`), **context**, **divider**, **actions** (optional later for buttons).
- Enforce Slack limits (e.g. **50 blocks**, section text length); truncate or split long agent logs with a final `context` line like `*truncated*`.

**Environment / config**

| Variable / file | Purpose |
|-----------------|--------|
| `SLACK_BOT_TOKEN` | Bot token (**required**; **only** supported storage — set in shell profile, Cursor env, or CI secrets). |
| `CURSOR_SLACK_DEFAULT_CHANNEL` | Default channel ID when `--channel` omitted. |
| Optional: `~/.config/juneau-cursor/slack-agent.json` or **`.cursor/slack-agent.local.json`** | Per-machine `{ "default_channel": "C…", "users": { "jbognar": "C…" } }` — **channel mapping only**, never tokens. Under Juneau, `.cursor/` is gitignored so this file stays local. |

**Identifying “the user’s reply”**

- **`conversations.replies`** on the parent `thread_ts` returns **only that thread** — aligns with thread-only supply.
- Skip messages where `user` is the bot (`bot_id` / app user) or subtype `bot_message` if needed.
- First **human** message in the thread after the parent = answer. User answers may themselves use **Block Kit**; `wait-reply` should still flatten to text for stdout unless `--reply-json` is set.

**Rate limits**

- Default poll interval **60 seconds** (1 minute) between `conversations.replies` checks — easy on Slack rate limits and fine for “reply when you have a moment” workflows. Shorter interval remains available via **`--poll-interval-seconds`** for interactive testing. Max timeout user-configurable (e.g. 3600 s). Optional exponential backoff later.

---

## Cursor integration

### 1. Cursor command (documentation + convention)

Add **`.cursor/commands/slack-agent.md`** (or `slack-alert.md`) under **`.cursor/commands/`** that instructs the agent:

- When blocked or needing confirmation, run **`.cursor/scripts/slack-agent.py`** (or the user’s **`~/.cursor/scripts/...`** path) with the documented flags.
- Prefer **channel ID** from user settings or env.
- For **blocking questions**, run `ask`, then `wait-reply`, then continue reasoning from stdout **or** stop and tell the user to paste the answer if timeout.

Because **`.cursor/` is gitignored** in the Juneau repo, the **live** command file is **local to your checkout**. **Reusability:** author **`.cursor/commands/slack-agent.md`** locally (or paste from the **[slack-agent README](https://git.soma.salesforce.com/central-routing/slack-agent/blob/main/README.md)** section *Using with Cursor*) when onboarding a new machine or repo.

### 2. Agent instructions (optional)

Optional: a one-line pointer in **your local `AGENTS.md`** (also gitignored in this repo) under **“Cursor / Slack alerts”**: **`SLACK_BOT_TOKEN`** (or gateway env vars), path to **`.cursor/scripts/slack-agent.py`**, link to **`todo/TODO-26-cursor-slack-agent-alerts.md`** (that plan file *is* part of Juneau git for documentation of the idea).

---

## Security and governance

- **Token storage (preferred):** run the **local Slack engine** from **[slack-agent](https://git.soma.salesforce.com/central-routing/slack-agent)** (`slack-agent.py engine setup` then `engine start`). **`~/.slack-agent/config.json`** holds **loopback port + localhost `auth_token` only** — Slack **`xoxb-`** is prompted (or read from **`SLACK_BOT_TOKEN`** for non-interactive starts) **into memory** at each **`engine start`**, not written to that file. Agents use **`SLACK_AGENT_URL`** + **`SLACK_AGENT_TOKEN`** only. **Direct mode:** **`slack-agent.py direct …`** with **`SLACK_BOT_TOKEN`** via env or gitignored **`.cursor/.env`** — see the same README (“Secrets model”). Never log the token; redact in script error messages.
- **Apache Juneau repo:** Implementation and Cursor command live under **gitignored `.cursor/`** (or **`~/.cursor/`**); this **TODO plan** in **`todo/`** can document the approach for contributors without shipping the bot in **`scripts/`**.
- **Channel isolation:** Personal channels reduce risk of posting to `#all-company`; still treat message body as potentially sensitive — prefer **Block Kit** `context` / truncated `section` for stack traces and command output.

---

## Starter kit for other workspaces

**Purpose:** Let any developer reuse the same Slack app pattern by copying files — no fork of Juneau required beyond this repo’s docs.

**Suggested committed location (pick one when implementing):**

- e.g. **`docs/cursor-slack-agent-starter/`** or **`contrib/cursor-slack-agent/`** in the Juneau repo — contains **no tokens**, only templates.

**Minimum files in the starter bundle**

| File | Action by new user |
|------|---------------------|
| `slack-agent.py` | Copy → **`<their-repo>/.cursor/scripts/slack-agent.py`** (or `~/.cursor/scripts/`). |
| `requirements-slack.txt` | Copy → same directory; run `pip install -r …` in a venv or `pip install --user -r …`. |
| `test-slack-agent.py` | Optional: copy beside `slack-agent.py`; run **`python3 test-slack-agent.py -v`** (no network). |
| `README.md` | Copy-paste quick start; **Salesforce approvals and secrets** → **[slack-agent README](https://git.soma.salesforce.com/central-routing/slack-agent)**. Optional **`.cursor/commands/slack-agent.md`**: see *Using with Cursor* in that README. |

**Operational docs (Salesforce Slack approvals, secrets, gateway):** maintained in the **[slack-agent README](https://git.soma.salesforce.com/central-routing/slack-agent)** (single source of truth). Juneau **`docs/cursor-slack-agent-starter/README.md`** summarizes copy-paste and links there.

**README must stress:** one **Slack workspace**, one **bot token**, many **channels**; **thread-only** answers for `wait-reply`; default **60 s** poll interval; **gateway-first** secrets for agents where possible.

---

## Implementation phases

### Phase 0 — Starter kit + local setup

- [x] **Salesforce Slack + secrets docs** — live in **[slack-agent README](https://git.soma.salesforce.com/central-routing/slack-agent)**.
- [x] **`docs/cursor-slack-agent-starter/`** starter implementation (mirrors **[slack-agent](https://git.soma.salesforce.com/central-routing/slack-agent)**): `slack-agent.py`, `requirements-slack.txt`, `README.md`, `test-slack-agent.py` (**README** links the canonical repo for approvals and secrets and includes Cursor usage).
- [ ] Document **`pip install --user slack_sdk`** (or use starter `requirements-slack.txt` after copy to `.cursor/scripts/`).
- [ ] Confirm **`.cursor/`** remains gitignored for Juneau (already in `.gitignore`).

### Phase 1 — `notify` (Block Kit)

- [ ] Implement `notify` with `WebClient.chat_postMessage` using **`blocks`** + fallback `text`.
- [ ] Helpers for common blocks + truncation to Slack limits.
- [ ] Unit-test with **mocked** HTTP or slack_sdk stubs; optional manual integration checklist in starter README.

### Phase 2 — `ask` + `wait-reply` (thread-only)

- [ ] Implement `ask` with Block Kit; capture and print **`thread_ts`** / **`channel`** for the parent message.
- [ ] Implement `wait-reply` polling **`conversations.replies`** for that **`thread_ts` only**; ignore non-thread channel messages.
- [ ] Flatten user’s Block Kit reply to text for stdout; support **`--reply-json`**.
- [ ] Document **timeout / exit code** contract for agents.

### Phase 3 — Defaults and multi-user (same workspace)

- [ ] Read optional JSON for default channel and per-OS-user overrides (**channel only**, no token).
- [ ] Document: **one Slack workspace, one bot**; different humans use **different channels** (or different default channel env) with the **same** bot install.

### Phase 4 — Cursor command template + plan links

- [ ] README (or starter README) copy-paste examples: Block Kit JSON samples, `ask` / `wait-reply` flow.
- [ ] Optional local **AGENTS.md** pointer to **`todo/TODO-26-cursor-slack-agent-alerts.md`**.

### Phase 5 — (Optional) Events / Socket Mode

- [ ] Revisit only if polling proves insufficient.

---

## Resolved decisions

- **Single Slack workspace** — One bot token; posts to any channel the bot is invited to. No multi-workspace v1.
- **Reusability** — Committed **starter kit**; users **copy** into their **`.cursor/`** (or `~/.cursor/`).
- **Thread-only** — `wait-reply` uses thread replies only; in-channel replies are not answers.
- **Rich messages** — **Block Kit from v1** for `notify` and `ask`; helpers + truncation; user replies may be blocks with text extraction for stdout.

---

## Acceptance criteria

- From a clean shell with **`SLACK_BOT_TOKEN`** (env only) and channel ID set, **`notify`** posts a **Block Kit** message to the channel within seconds.
- **`ask`** + **`wait-reply`** succeed when the user replies **in the thread** (including a block-formatted reply) within the timeout; **in-channel** replies outside the thread do **not** satisfy `wait-reply`.
- Starter kit can be **copied** to a fresh repo’s `.cursor/` and works after env + Slack app setup (documented in starter README).
- No secrets in committed starter files; CI does not require Slack.
- Cursor command template documents invocation without improvising token handling.
