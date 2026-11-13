# Contributing

**English** · [Русский](CONTRIBUTING.ru.md)

Bug reports, ideas and pull requests are welcome. This file describes how commits are written in this repository: simple, transparent rules that keep the history easy to read.

## Commits

The repository follows [Conventional Commits](https://www.conventionalcommits.org/). Commits made before this convention was introduced keep their original messages.

**Fundamental rules:**

1. Commit messages are written in English.
2. The type must be used.
3. Use the imperative mood, not the past tense: `add`, not `added`.
4. The subject starts with a lowercase letter and has no punctuation mark at the end.
5. The first line must not exceed 100 characters.

**Structure:**

```
<type>(<scope>): <subject>

<body: optional, what and why>
```

The scope is optional and can be left out when a change touches several areas. A change that breaks compatibility (worlds, datapacks, config, network) gets `!` after the type or scope: `feat(config)!: rename the removable list option`.

| Type | Meaning | Example |
| ---- | ------- | ------- |
| **feat** | A new feature | `feat(iface): add the disabled side mode` |
| **fix** | A bug fix | `fix(rooms): keep untouched template chests from blocking removal` |
| **refactor** | Code changes that don't change behaviour | `refactor(iface): move storage kinds onto the kind registry` |
| **perf** | Performance | `perf(iface): cache neighbour storages per side` |
| **docs** | Documentation, README, comments | `docs: describe datapack room exports` |
| **style** | Formatting and typos in code, no logic changes | `style(client): fix indentation in the room screen` |
| **test** | Tests | `test(rooms): cover doorway strip mirroring` |
| **build** | Gradle, dependencies, versions | `build: bump the version to 1.3.0` |
| **ci** | GitHub Actions | `ci: run game tests on demand` |
| **chore** | Everything that does not fit the previous ones | `chore: update the example datapack` |

| Scope | Area |
| ----- | ---- |
| `rooms` | Growing, managing and removing rooms |
| `iface` | TARDIS Interface and transfer kinds |
| `compat` | Integrations with other mods |
| `config` | Config files and options |
| `client` | Screens, rendering, client-only code |
| `net` | Network packets |
