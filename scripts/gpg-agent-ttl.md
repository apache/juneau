# gpg-agent-ttl.py

Sets `default-cache-ttl` and `max-cache-ttl` in `gpg-agent.conf` so gpg-agent keeps the release passphrase cached for a whole release. Without a conf file gpg-agent uses 600 s and 7200 s, which expired mid-release ("No pinentry"). Standard library only.

## When to use

- Once per machine before a release, or when the preflight "gpg-agent cache TTL" check fails.
- Never lowers a value you already set higher.

## Usage

```bash
python3 scripts/gpg-agent-ttl.py
python3 scripts/gpg-agent-ttl.py --dry-run
python3 scripts/gpg-agent-ttl.py --default-ttl 3600 --max-ttl 7200 --no-reload
python3 scripts/gpg-agent-ttl.py --homedir /path/to/gnupg
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--default-ttl` | seconds | 7200 | Target `default-cache-ttl`. |
| `--max-ttl` | seconds | 14400 | Target `max-cache-ttl`; must not be lower than `--default-ttl`. |
| `--homedir` | dir | `$GNUPGHOME`, else `~/.gnupg` | The gnupg home; also passed to `gpgconf`. |
| `--no-reload` | none | off | Skip `gpgconf --reload gpg-agent` after a change. |
| `--dry-run` | none | off | Print what would change; write and reload nothing. |

## What it does

1. Resolves the gnupg home and reads `gpg-agent.conf` (creating the home mode 0700 and the file mode 0600 when missing).
2. Raises a `default-cache-ttl` / `max-cache-ttl` line only when its value is below the target; appends a missing key; leaves every other line alone.
3. Writes atomically (temp file and rename), keeping the file mode; writes nothing when already OK.
4. After a change, runs `gpgconf --reload gpg-agent`; a failure only warns.
5. Reads the effective values back from `gpgconf --list-options gpg-agent`.

## Output and exit codes

One line per action (created, updated, added, already OK, reloaded), then an `effective:` line.

| Code | Meaning |
|---|---|
| 0 | Done, nothing to do, or `--dry-run`. |
| 2 | `--max-ttl` is lower than `--default-ttl`. |

## Gotchas

- If `gpgconf` is not on PATH, the reload and read-back are skipped with a warning.
- A reload failure is a warning; run `gpgconf --reload gpg-agent` by hand.
- Cached passphrases are not extended retroactively; re-enter the passphrase after the reload.

## See also

- `release.md` (the preflight check for the gpg-agent cache TTL)
