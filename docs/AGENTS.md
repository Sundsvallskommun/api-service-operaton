# Documentation rules

These rules apply to every change under `docs/` and keep its structure the same from one change to the next.

## Layout

```
docs/
  README.md          index, one line per document
  architecture.md    modules, cross-cutting rules, endpoints, worker topics, deploying models
  integrations/      one file per external system a worker calls
  adr/               decision records, created with the first record
  todo/              known work not done yet, one file per kind
```

- An integration file is named after the integration's key under `integration:` in
  `operaton-app/src/main/resources/application.yml` (`care-management.md`, `messaging.md`), and follows the sections
  of the existing files.
- A new subdirectory starts once it has at least two files; until then the file sits directly in `docs/`.
- File names are kebab-case. Decision records are named `adr/NNNN-short-title.md`, numbered from `0001`.
- Every file added, renamed or removed is reflected in `docs/README.md` in the same change.
- A todo item says what is wrong, where, and when it is done. The change that does it deletes the item, and the file
  once it is empty.

## Where each kind of knowledge lives

Each fact has one home. The documentation links to the others instead of restating them.

|                            Knowledge                            |                                       Home                                        |
|-----------------------------------------------------------------|-----------------------------------------------------------------------------------|
| The map: what talks to what, knowledge spanning several classes | `docs/`                                                                           |
| A complex case in a single class the code cannot make clear     | A short comment in that class, see Code conventions in the root `AGENTS.md`       |
| What a configuration property means                             | Its entry under Configuration in the root `README.md`                             |
| What a worker does and which variables it reads and writes      | Its `@TopicWorker` annotation, served live at `GET /{municipalityId}/topics`      |
| Work known but not done yet                                     | `docs/todo/`                                                                      |
| Request and response fields                                     | The OpenAPI annotations on the resources, served at `/api-docs`                   |
| The BPMN and DMN models themselves                              | Outside this repository. TODO: name the repository once there is one              |
| Ownership, environments, contacts, operations                   | Confluence, linked from `docs/README.md`                                          |

## Writing

- Plain Markdown in English. Describe flows as numbered lists or tables.
- Reference code by repository-relative path, e.g. `operaton-process/src/main/java/.../ProcessService.java`.
- Mark anything you could not verify from the code as `TODO:` followed by what is missing.

## Decision records

Write one when a choice is not obvious from the code and someone could reasonably undo it. Sections: **Status**
(proposed, accepted, superseded by NNNN), **Context**, **Decision**, **Consequences**. Half a page is enough.
