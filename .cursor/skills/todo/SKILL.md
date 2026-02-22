---
name: todo
description: Add to TODO
disable-model-invocation: true
---
# Add to TODO

Add an entry to `TODO.md` in the project root.

## Steps

1. **Get the TODO text**: Use the text the user provides after `/todo` as the new TODO entry. If no text is given, ask what they would like to add.
2. **Read TODO.md**: Check the current format and existing entries.
3. **Add the entry**: Append a new bullet to the list, matching the existing format:
   ```
   - [description of the TODO]
   ```
4. **Confirm**: Tell the user the entry was added.

Keep the description clear and actionable. Do not add TODO-# identifiers to the list—those are only used when referencing items for "work on TODO-X" commands.
