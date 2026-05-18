# Consolidate `AGENTS.md` / `CLAUDE.md` / Cursor skills across repos

## Goal

Collapse the scattered agent-instruction locations (today: 13 distinct paths across Juneau, IRS, and home dir) into a single tool-neutral `agents/` directory at each repo root and a parallel `~/agents/` in the home directory. Modernize `AGENTS.md`, de-duplicate Slack guidance and Java conventions, and lift personal/cross-repo content into `~/` so both Cursor and Claude Code pick it up via symlinks.

## Scope

Two repos:

- [`/Users/james.bognar/git/apache/juneau`](/Users/james.bognar/git/apache/juneau)
- `/Users/james.bognar/git/central-routing/irs`

Plus the developer's home directory:

- `~/.claude`, `~/.cursor`

Out of scope:

- `~/.cursor/skills-cursor/` (Cursor auto-managed built-ins)
- `~/.claude/plugins/` (Claude CLI managed)
- Per-repo `.cursor/extensions/`, `.cursor/projects/`, `.cursor/plans/`, `.cursor/ai-tracking/`
- Rewriting the content of [`code-conventions/SKILL.md`](../juneau-core/.cursor/skills/code-conventions/SKILL.md) (41KB; just relocate)
- Per-developer secret-bearing config (`.cursor/slack.local.yml`, `.claude/settings.local.json`)

## Inventory snapshot (today)

| Location | Contents | Action |
|---|---|---|
| `apache/juneau/AGENTS.md` (365 lines) | Mix of Juneau workflow + generic Java/Eclipse/Slack/git-revert | Trim to ~80 lines (Juneau-specific only) |
| `apache/juneau/.cursor/commands/` | 6 cmds: `coverage`, `docs`, `modernize`, `push`, `slack`, `todo` | Move to `agents/commands/`, replace dir with symlink |
| `apache/juneau/.cursor/skills/code-conventions/SKILL.md` (41KB) | Canonical Java conventions | Move to `agents/skills/`, replace dir with symlink |
| `apache/juneau/.cursor/slack.local{,.example}.yml` | Per-developer Slack config | Stays in `.cursor/`; gitignored |
| `apache/juneau/.claude/settings.local.json` | Permissions stub | Stays in `.claude/` |
| `irs/AGENTS.md` (742 lines) | Project overview + heavy Java style + Slack + push.py details | Trim to ~150 lines |
| `irs/CLAUDE.md` (121 lines) | Claude Code-specific overview, mostly duplicates AGENTS.md | Replace with stub / symlink to `AGENTS.md` |
| `irs/AGENTS_VARIABLES.md` (27 lines) | Canonical literals (`JAVA_COPYRIGHT_YEAR = 2026`) | Move to `agents/variables.md` |
| `irs/AGENTS_DESIGNS.md` (1453 lines) | RFC/design authoring rules | Move to `agents/skills/rfc-and-design-authoring/SKILL.md` (add frontmatter) |
| `irs/CHANGE-CASE-CONFIG-UPDATE.md` (6 lines) | Tombstone pointing at Docusaurus | Delete |
| `irs/.cursor/commands/` | 8 cmds (adds `blacktab`, `git` over Juneau) | Move to `agents/commands/`, symlink |
| `irs/.cursor/skills/` | 4 skills: `gus-irs-story-bucket-bug`, `irs-database-schema`, `irs-juneau-rest`, `irs-sonarqube` | Move to `agents/skills/`, symlink |
| `~/.claude/{skills,agents,rules}/` | Empty dirs | Replace `skills` with symlink to `~/agents/skills`; remove the others |
| `~/.cursor/skills-cursor/` | Cursor auto-managed | Leave alone |

## Target layout

```
<repo>/
├── AGENTS.md                    # slim, project-specific, root entry point
├── CLAUDE.md                    # 1-line pointer or symlink to AGENTS.md (IRS only)
├── agents/                      # visible, tool-neutral, CANONICAL
│   ├── variables.md             # IRS only (was AGENTS_VARIABLES.md)
│   ├── skills/
│   │   ├── code-conventions/    # Juneau only
│   │   ├── irs-java-conventions/    # IRS only (extracted from AGENTS.md)
│   │   ├── irs-database-schema/     # IRS only
│   │   ├── irs-juneau-rest/         # IRS only
│   │   ├── irs-sonarqube/           # IRS only
│   │   ├── gus-irs-story-bucket-bug/    # IRS only
│   │   ├── rfc-and-design-authoring/    # IRS only (was AGENTS_DESIGNS.md)
│   │   └── _shared -> ~/agents/skills   # SYMLINK; cross-repo + personal skills
│   └── commands/                # Cursor slash-command prompts
├── .cursor/
│   ├── skills        -> ../agents/skills        # SYMLINK
│   ├── commands      -> ../agents/commands      # SYMLINK
│   ├── slack.local.yml          # gitignored, per-developer
│   └── slack.local.example.yml  # checked in
└── .claude/
    ├── skills        -> ../agents/skills        # SYMLINK
    └── settings.local.json
```

And in home:

```
~/
├── AGENTS.md         -> agents/AGENTS.md        # SYMLINK
├── agents/
│   ├── AGENTS.md                # personal preamble (shorthand, env, etc.)
│   └── skills/
│       ├── slack-notify/        # unified Slack notify/ask/wait-loop spec
│       └── create-rule-or-skill/    # "save a rule" semantics
└── .claude/
    └── skills        -> ~/agents/skills         # SYMLINK
```

## Phases

### Phase 1 — Build out `~/agents/` (personal extraction)

1. Create `~/agents/AGENTS.md` with:
   - Shorthand: `c` = continue, `s` = status, `TODO-n` = work on TODO.
   - Task interpretation: "make a plan" → suggestion only; "suppress warnings" → SonarLint format.
   - Environment: Java JDK path (`~/jdk/openjdk_17.0.14.0.101_17.57.18_aarch64/bin/java`), Eclipse Build-Automatically warning + resource-analysis prefs (lifted from Juneau `AGENTS.md` §4, §4.1).
   - Command execution: `timeout` wrapping for `head`/`tail` (Juneau §5.3).
   - Git revert protocol: prefer `scripts/revert-{staged,unstaged}.py`.
2. Create `~/agents/skills/slack-notify/SKILL.md` — single source of truth for the Slack notify/ask/wait-loop flow, currently duplicated in Juneau `AGENTS.md` 306-366, IRS `AGENTS.md` 52-123, and both `.cursor/commands/slack.md` files (identical 24KB each).
3. Create `~/agents/skills/create-rule-or-skill/SKILL.md` — "save a rule / store this rule" routing (AGENTS.md vs skill vs conventions skill).
4. Create `~/agents/slack/slack.local.example.yml` (single canonical copy; currently duplicated identically across both repos).
5. Symlink wiring:
   - `~/.claude/skills` → `~/agents/skills`
   - `~/AGENTS.md` → `~/agents/AGENTS.md`
6. Clean up empty dirs: `rm -rf ~/.claude/agents ~/.claude/rules` (both empty today).

### Phase 2 — Per-repo `agents/` layout (Juneau and IRS)

For each repo:

1. `mkdir -p agents/{skills,commands}`.
2. Move existing `.cursor/skills/*` → `agents/skills/`. Move `.cursor/commands/*.md` → `agents/commands/`.
3. Add cross-repo symlink: `ln -s ~/agents/skills agents/skills/_shared` (gitignored; the `~` differs per developer).
4. Replace tool dirs with symlinks:
   - `rm -rf .cursor/skills && ln -s ../agents/skills .cursor/skills`
   - `rm -rf .cursor/commands && ln -s ../agents/commands .cursor/commands`
   - `mkdir -p .claude && ln -s ../agents/skills .claude/skills`
5. Update `.gitignore` per repo: add `agents/skills/_shared`.
6. Verify `.cursor/` and `.claude/` are not in `.gitignore` (they aren't today).

### Phase 3 — Modernize Juneau `AGENTS.md`

Target: 365 → ~80 lines.

- Drop entirely:
  - Slack section (lines 306-366) → `@slack-notify` skill.
  - Eclipse Build-Automatically + Java runtime (lines 44-82) → `~/agents/AGENTS.md`.
  - Eclipse resource-analysis (lines 84-105) → `~/agents/AGENTS.md`.
  - Timeout-wrapping best practices (lines 189-216) → `~/agents/AGENTS.md`.
  - Git revert section (lines 279-302) → `~/agents/AGENTS.md`.
- Keep & condense:
  - Pointer to `agents/skills/code-conventions/SKILL.md` as canonical Java rules.
  - Juneau shorthand command catalog (one-liner each: `push`, `test`, `start docs`, etc.).
  - TODO format and lifecycle (the `[TODO-n]` policy is Juneau-specific).
  - Release notes location (trim to one paragraph + the canonical file path).
  - Helper script names (`test.py`, `coverage.py`, `push.py`, `revert-*.py`) — one line each, no inline usage docs (let users `./scripts/test.py --help`).

### Phase 4 — Modernize IRS root docs

Target IRS `AGENTS.md`: 742 → ~150 lines.

- Drop / move:
  - Slack section (lines 52-123) → `@slack-notify` skill.
  - "Suggestion Only" mode + "save a rule" (~12 lines) → `~/agents/AGENTS.md`.
  - Java style sections (indentation, imports, exception handling, `var`, instanceof, final, static constants, `@SuppressWarnings`) lines 143-399 → new `agents/skills/irs-java-conventions/SKILL.md` (mirrors the Juneau `code-conventions` pattern).
  - `push.py` deep-dive (lines 563-616) → keep one paragraph; depth lives in `agents/commands/git.md`.
- Keep & condense:
  - Project overview, module structure.
  - Cursor `/` command catalog (one-line per command, pointing at the prompts in `agents/commands/`).
  - DB / Flyway summary (the deep content is in `agents/skills/irs-database-schema/SKILL.md`).
  - REST API summary (deep content in `agents/skills/irs-juneau-rest/SKILL.md`).
  - Testing summary, deployment notes, resource links.

Other root-file changes:

- `CLAUDE.md` → 5-line stub: "Canonical instructions are in `AGENTS.md`. This file exists so Claude Code's auto-discovery still works." (Or `ln -s AGENTS.md CLAUDE.md` if Claude Code accepts symlinks.)
- `AGENTS_VARIABLES.md` → `agents/variables.md`; update the one reference in `AGENTS.md`.
- `AGENTS_DESIGNS.md` → `agents/skills/rfc-and-design-authoring/SKILL.md` with YAML frontmatter:
  ```yaml
  ---
  name: rfc-and-design-authoring
  description: IRS RFC and design doc workflow (/docs create rfc, /docs create design, /docs review).
  ---
  ```
- `CHANGE-CASE-CONFIG-UPDATE.md` → delete (6-line tombstone pointing at Docusaurus; redirect has no value at repo root).

### Phase 5 — Audit for drift / dead references (TODO-37 charter)

After moves:

- Verify all `.cursor/skills/foo/SKILL.md` references still resolve through the symlink (`ls -L`).
- Replace per-repo 24KB `.cursor/commands/slack.md` files with a 3-line stub pointing at `@slack-notify`.
- Sweep both `AGENTS.md` files: rewrite `.cursor/skills/*` paths → `agents/skills/*` so the canonical path is what users see (symlink keeps old path working as a fallback).
- Verify the `code-conventions/SKILL.md` reference in Juneau `AGENTS.md` (current lines 7-11) still resolves.

### Phase 6 — Verification

- `ls -L agents/skills/` in each repo lists all skills (own + shared via `_shared`).
- `cat .cursor/skills/code-conventions/SKILL.md` returns content via symlink in Juneau.
- `cat .claude/skills/slack-notify/SKILL.md` returns content via two-hop symlink (`.claude/skills` → `agents/skills` → `_shared` → `~/agents/skills`) in either repo.
- Open Cursor in each repo, type `@` in chat, confirm the Skills picker shows own + shared skills.
- Start a Claude Code session from `~`, confirm `~/AGENTS.md` is picked up.
- Run `python3 scripts/slack-notify.py "test"` from each repo, confirm push still works.
- `git status` shows only intended changes (no churn in `.cursor/skills-cursor/` or `~/.claude/plugins/cache/`).
- Remove the `[TODO-37]` line from `todo/TODO.md` (per AGENTS.md TODO completion rule — don't mark as fixed, just delete).

## Open questions / decisions to revisit during execution

- **CLAUDE.md as symlink vs stub**: test whether Claude Code 1.0+ follows symlinks for `CLAUDE.md` discovery. If yes, prefer symlink; if no, use the 5-line stub.
- **`_shared` naming**: `_shared` was picked because Cursor's skill picker sorts alphabetically and the underscore prefix groups it. Alternatives: `shared`, `home`, `personal`. Decide during Phase 2.
- **IRS `agents/commands/blacktab.md` and `git.md`**: these are IRS-specific (no Juneau equivalent). Stay in the IRS repo. The `slack.md` stub and any future cross-repo command would move to `~/agents/commands/` if Cursor ever supports user-level commands (today it doesn't, so stays per-repo).
- **`irs-java-conventions` vs reusing `code-conventions`**: IRS has its own tabs/copyright/`@SuppressWarnings` rules that differ from Juneau (`CONST_camelCase` is IRS-specific, copyright year is IRS-specific). Keep separate skills, but cross-link between them for shared concepts.
