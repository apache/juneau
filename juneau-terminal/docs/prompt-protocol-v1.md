# Prompt protocol v1

How a script asks a host (a UI, a service, a test) for input without a terminal. `juneau_terminal_scripts.prompt` is the script
side; `juneau_terminal.py --pty --prompts stdio` relays the channel over a process's stdio, and
`TerminalProcess` in `juneau-terminal-region` is a host that does so from Java.

With no host, the same calls prompt on the terminal; nothing in this document applies then.

## Transport

- The host starts the script with two inherited descriptors and names them in the environment:
  - `JUNEAU_PROMPT_FD_OUT`: the script writes, the host reads.
  - `JUNEAU_PROMPT_FD_IN`: the host writes, the script reads.
- The script reads both variables once, removes them from its environment (so tool children never inherit them),
  checks each descriptor and marks it non-inheritable. A malformed variable is ignored with a warning, and the script
  prompts on the terminal.
- Framing is UTF-8 JSON objects, one per line, at most 1 MiB. A longer line is dropped.
- Every object has `"v": 1` and a `type`. An unknown `type` is ignored. An unknown `v` is a protocol error: the
  reader warns and treats it as EOF.
- A line that is not JSON is dropped with a warning that names its length, never its content.

## Script to host

```json
{"v":1,"type":"hello","pid":4242,"script":"release.py","attempt":"2026-10-20"}
{"v":1,"type":"prompt","id":"p7","kind":"text","key":"release.rc","title":"Release candidate",
 "message":"Release candidate number","default":"1","error":null,"step":"prechecks"}
{"v":1,"type":"prompt","id":"p8","kind":"secret","key":"credential.gpg.passphrase","message":"Passphrase","step":"unlock"}
{"v":1,"type":"prompt","id":"p9","kind":"confirm","message":"Type 9.2.1 to release","expect":"9.2.1","danger":true}
{"v":1,"type":"prompt","id":"p10","kind":"choice","message":"Outcome",
 "options":[{"value":"passed","label":"Passed"},{"value":"failed","label":"Failed"}],"default":null}
{"v":1,"type":"prompt","id":"p11","kind":"form","title":"New release","message":"New release",
 "fields":[{"name":"version","label":"Version","kind":"text","default":"9.2.1","required":true}]}
{"v":1,"type":"prompt","id":"p12","kind":"email","message":"Email: [VOTE] Release 9.2.1",
 "email":{"to":["dev@example.org"],"cc":[],"subject":"[VOTE] Release 9.2.1","body":"...","mailto":"mailto:..."}}
```

- `hello` is sent once, before the first prompt. Extra fields (such as `attempt`) come from `prompt.hello(**fields)`.
- `id` is `p1`, `p2`, ... per process. At most one prompt is outstanding.
- `key` is a stable name a host may use to offer a stored value. `title` is a short heading. `step` is the registry
  step that is running. A field with no value is omitted.
- `error` is set when the script re-asks after rejecting the previous answer. It names the field, never the content.
- Form field `kind` is `text`, `int`, `multiline` or `secret`.
- `mailto` is omitted when the URL would exceed 1800 characters.
- A prompt never carries a secret value; only answers do.

## Host to script

```json
{"v":1,"type":"answer","id":"p7","value":"1","source":"entered"}
{"v":1,"type":"answer","id":"p8","value":"<secret>","source":"stored"}
{"v":1,"type":"answer","id":"p12","value":"sent"}
{"v":1,"type":"answer","id":"p9","cancel":true}
```

| kind | `value` |
|---|---|
| `text` | string |
| `confirm` | boolean (the prompt carries `hint`, `(y/N)` or `(Y/n)`, absent with `expect`); with `expect`, the typed string, which the script compares |
| `secret` | string |
| `choice` | one option `value` |
| `form` | object of field name to string |
| `email` | `"sent"` or `"not-yet"` |

- `source` (`stored` or `entered`) is optional. The script appends it to the transcript line of a secret.
- `cancel: true` cancels the prompt. The call returns `None`.
- An answer for another `id` is dropped with a warning.
- EOF on the descriptor cancels this and every later prompt at once; the script does not wait.
- A value that fails validation (a missing required field, a non-integer in an `int` field, an option that was not
  offered) makes the script send the prompt again under a new `id` with `error` set.

## Relay: `juneau_terminal.py --pty --prompts stdio`

- Valid only with `--pty`, and not with `--input`.
- The runner makes two pipes and gives the job their ends as the two descriptors above.
- Each complete line the job writes goes to the runner's stdout, flushed per line. Each complete line on the runner's
  stdin goes to the job. Lines are relayed as received.
- A line that is not JSON, or is over 1 MiB, is dropped with a warning on stderr.
- In this mode stdout carries nothing but relayed prompt lines. Notes and warnings go to stderr.
- EOF on the runner's stdin means the host is gone. The runner sends SIGTERM to the job's process group, waits 10
  seconds, then sends SIGKILL, and ends with `done` status `cancelled` and exit 130.

## Secrets

A secret answer is never written to `terminal.log`, the events file, `runner.out`, the bus, a checkpoint, a command
line or a warning. The transcript line the script prints shows a secret as `********`.
