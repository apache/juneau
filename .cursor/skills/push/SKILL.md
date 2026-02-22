---
name: push
description: Push Changes
disable-model-invocation: true
---
# Push Changes

Run the Juneau push script to build, test, commit, and push changes. You will determine the commit message yourself based on the staged/changed files.

## Steps

1. **Inspect changes**: Run `git status` and `git diff --staged` (or `git diff` if nothing staged) to see what has changed.
2. **Determine commit message**: Based on the changes, craft a clear, descriptive commit message. Follow conventional commit style when appropriate (e.g., `fix:`, `feat:`, `refactor:`, `docs:`). Keep it concise but specific.
3. **Run push script**: Execute from the project root (master/):

   ```
   python3 scripts/push.py "YOUR_COMMIT_MESSAGE"
   ```

   Use `--skip-tests` only for documentation-only changes. Do not use `--dry-run` unless the user asks.

4. **Report result**: Summarize what was committed and pushed, or report any failures.

Do not ask the user for the commit message—determine it from the changes yourself.
