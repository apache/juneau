# push-timings.py

Compares the latest test run's timings against the repository's recorded history and reports modules that got slower or faster.

## When to use

- You want to see whether the last run of `test.py` was noticeably slower than usual, per module and test bucket.
- `push.py` loads this file as a module for its Timing row; you can also run it by hand.

## Usage

```bash
python3 scripts/push-timings.py
python3 scripts/push-timings.py --window 10 --report /tmp/timings.txt
python3 scripts/push-timings.py --history path/to/test.jsonl
python3 scripts/push-timings.py --repo ~/git/apache/juneau
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--repo` | directory | this script's repository | Repository whose run history to read. Mutually exclusive with `--history`. |
| `--history` | file | none | A `test.py` history records file (JSON lines) to read instead of the repository's store. Mutually exclusive with `--repo`. |
| `--window` | N (integer) | 20 | Number of earlier runs used for the rolling median of each module and bucket. |
| `--report` | file | `test-timings.txt` beside the history store | Where to write the full table. |

## What it does

1. Reads the `tests` records that `test.py` writes to the run-history store shared by every branch and worktree of the repository, or the file given with `--history` (`load_history`, `rows_from_records`).
2. Keeps only records with the same command shape as the newest one, so a run with different options never skews the baseline.
3. Turns each record into rows: one per module and bucket from its Surefire seconds, plus a reactor row from the wall-clock time.
4. Stops with a one-line message when history is off, there is no history, the latest run failed, or fewer than two runs of this shape exist.
5. Compares each module and bucket of the latest run with the median of up to `--window` earlier passing runs (`analyse`). It is slower only when it exceeds the median by more than the relative threshold and by more than the minimum number of seconds, and faster by the same two margins (`classify`).
6. Writes the full table to the report file and prints a summary line plus the slower entries.

## Output and exit codes

One summary line with the counts and the report path, then one `⚠` line per slower entry. The report file lists every entry with a mark.

| Code | Meaning |
|---|---|
| 0 | Always, including when there is nothing to compare. |

## Gotchas

- `JUNEAU_PUSH_TIMING_THRESHOLD` sets the relative margin (default `0.20`) and `JUNEAU_PUSH_TIMING_MIN_SECONDS` the absolute one (default `5`). Unparseable values fall back to the defaults.
- `JUNEAU_RUN_HISTORY=0` turns the history store off; the script then reports that and exits 0.
- The report file is replaced on every run.
- The history comes from `test.py` runs, so a repository with no recorded runs reports "no timing history yet".

## See also

- `test.py`, which writes the history
- `push.py`, whose Timing row uses this script
- `surefire-rollup.py`
