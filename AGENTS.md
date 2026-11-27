# AGENTS.md

Documentation lives in `docs/`, in English. `docs/README.md` is the index.

Keep `docs/` true to the code at every commit. Each change updates the documentation it affects in the same commit,
not in a later one. A change is done when every statement in `docs/` and `README.md` about the code it touched still
holds. The usual cases:

- A worker added or removed, or its topic, input or output variables changed → the topic table in
  `docs/architecture.md`, and the integration file when the worker calls another service.
- An integration added or removed, or its calls or authentication changed → `docs/integrations/<integration>.md`.
- An endpoint added or removed, or the engine service it uses changed → `docs/architecture.md`.
- A module added, removed or renamed → `docs/architecture.md`.
- A rule that holds across the service added or changed, such as how a worker fails a task → `docs/architecture.md`.
- A choice that is not obvious from the code and could reasonably be undone → a new record in `docs/adr/`.
- A configuration property added, renamed or removed → `README.md`.
- Work found but left for later → an item in `docs/todo/`; work that finishes an item → that item deleted.

A change a client or an operator notices (an endpoint, a response, a status, a worker topic or its variables,
configuration, a dependency bump) adds one line to `CHANGELOG.md` under today's date (`## YYYY-MM-DD`), newest date
first. A refactoring adds one line naming what was reworked, not each change in it. Tests and documentation add none.
Create the heading if today has none. One short sentence in English, in the imperative ("Add …", "Fix …"), saying
what changed rather than how, with the Jira key in parentheses when there is one.

Read `docs/AGENTS.md` before editing anything under `docs/`.

## Code conventions

- Method names start with a verb that says what the method does (`getProcessInstance`, `selectTransferable`).
  Predicates read as a question (`isInControlPeriod`); converters start with `to` (`toDeploymentResponse`).
- Write conditionals as `if` statements or an Optional chain, not the ternary operator.
- Let names and structure explain the code. Add a comment only for a genuinely complex case the code cannot make
  clear on its own, such as an engine or dept44 quirk, and keep it short.
- References run one way: `docs/` points at the code, and nothing outside `docs/` points into it. Code, comments,
  validation and error messages, `application.yml`, `@TopicWorker` descriptions and `CHANGELOG.md` state what they
  need in their own words, without naming an ADR or a docs file.

