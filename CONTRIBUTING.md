# Contributing to juneau-docs

## Sidebar label convention

Every sidebar `label:` must equal the page `title:` frontmatter value, without a number. `sidebars.ts` generates the numbers from position (the `numbered()` helper), so never type one into a label.

Example: `title: "YAML Basics"` in file `02.34.01.YamlBasics.md` → sidebar `label: 'YAML Basics'`, displayed with its generated position number in front of it. The file prefix and the displayed number are independent.

Exception: the four Getting Started pages carry their number in the page title (`title: "1. Install Juneau"`), so their label is the title minus that prefix.

A divider heading inside a section (for example `Core Concepts`) is wrapped in `group({...})`: it has no number and its children continue the parent's count.

If a title is too long for comfortable sidebar display, shorten the **page title** — don't curate a separate sidebar label. This keeps the two in sync automatically.
