# Task Manager API Contract

This file describes every operation of `TaskApi`, what it returns, and every error it can return. Each operation is a Java method that returns an `ApiResponse` (a status code and a JSON-style body). The "HTTP" line shows the endpoint it would become if the service were put behind a web framework. If that happens, this file is the starting point for the OpenAPI file.

## Conventions

- **Status codes:** 200 OK, 201 Created, 400 invalid input, 404 unknown ID, 422 idempotency conflict, 500 unexpected error, 502 the language model (or parser) failed, 503 AI features not configured.
- **Body:** a JSON object. Text fields are strings, IDs and counts are numbers, `completed` is a boolean, and a missing value is `null`.
- **Input:** `TaskRequest` carries every field as a **String**, exactly as the client sent it. The service decides what is valid.
- **Optional fields:** for `dueDate`, `priority`, `recurrence` and `completed`, `null` means "not given". For `dueDate`, `priority` and `recurrence`, a blank string also means "not given".
- **Idempotency key:** `createTask`, `createTaskFromText` and `breakDownTask` take an optional `idempotencyKey`. `null` turns idempotency off. See [Idempotency](#idempotency).

## The Task object

```json
{
  "id": 1,
  "title": "Pay rent",
  "description": "Remind me to pay rent on the 1st of every month",
  "completed": false,
  "dueDate": "2026-12-01",
  "priority": "MEDIUM",
  "parentId": null,
  "recurrence": "MONTHLY",
  "createdAt": 1795723037938
}
```

| Field | Type | Rules |
|---|---|---|
| `id` | number | Assigned by the repository, starting at 1. Never reused. |
| `title` | string | Required. Not empty or blank, at most 200 characters. Stored trimmed. |
| `description` | string | Required. Not empty or blank, at most 2000 characters. Stored trimmed. |
| `completed` | boolean | Input must be exactly `"true"` or `"false"`. Missing means `false`. |
| `dueDate` | string or null | A real calendar date, `YYYY-MM-DD`. `2026-02-30` and `2026-02-29` are rejected, `2028-02-29` is accepted. |
| `priority` | string | `LOW`, `MEDIUM` or `HIGH`, exactly (upper case). Missing means `MEDIUM`. |
| `parentId` | number or null | The parent's ID for a subtask, otherwise `null`. Set by `createSubtask` and `breakDownTask`, never by the request. |
| `recurrence` | string or null | `DAILY`, `WEEKLY`, `MONTHLY` or `YEARLY`, exactly. Usually filled in by the AI parser. |
| `createdAt` | number | Creation time in milliseconds since 1970-01-01 UTC. |

Only one level of subtasks is allowed: a subtask cannot have subtasks of its own.

## The error response

Every error, whatever its status, has exactly this shape:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "title", "issue": "must not be empty" }
    ]
  }
}
```

- `code` and `message` are always strings.
- `details` is always a list. It is empty when there is nothing field-specific to say.
- A 400 lists **every** problem in the request, not just the first.

### Error codes

| Status | `code` | `message` | When |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | `Validation failed` | The input breaks a rule. `details` says which field and why. |
| 404 | `NOT_FOUND` | `Task {id} not found` | No task has that ID. `details` is empty. |
| 422 | `IDEMPOTENCY_CONFLICT` | `Idempotency key '{key}' was already used with a different request` | The key was used before with different contents or by a different operation. |
| 500 | `INTERNAL_ERROR` | `An unexpected error occurred` | A bug or an unexpected failure. The real cause is logged on the server, never sent to the client. |
| 502 | `BAD_GATEWAY` | Says what went wrong, for example `The task parser returned an invalid task` | The language model call failed or timed out, the reply was not in the expected format, or its values broke the validation rules. `details` lists the bad fields when there are any. **Nothing is saved.** |
| 503 | `SERVICE_UNAVAILABLE` | `AI features are unavailable: LLM_API_KEY is not set` | An AI operation was called in LLM mode without an API key, or the provider rejected the key. |

### Fields that can appear in `details`

| `field` | Issue | Operations |
|---|---|---|
| `title`, `description` | `must not be empty`, `must be at most N characters` | create, update, createSubtask; inside 502 for AI operations |
| `completed` | `must be exactly "true" or "false"` | create, update, createSubtask |
| `dueDate` | `must be a real date in YYYY-MM-DD format` | create, update, createSubtask; inside 502 for AI operations |
| `priority` | `must be LOW, MEDIUM, or HIGH` | create, update, createSubtask, listTasks |
| `recurrence` | `must be DAILY, WEEKLY, MONTHLY, or YEARLY` | create, update, createSubtask; inside 502 for AI operations |
| `request` | `must not be null` | create, update, createSubtask |
| `text` | `must not be empty` | createTaskFromText |
| `idempotencyKey` | `must not be blank when given` | create, createTaskFromText, breakDownTask |
| `parentId` | `task N is a subtask, so it cannot have subtasks ...` | createSubtask |
| `id` | `task N is a subtask, so it cannot be broken down ...` | breakDownTask |
| `level` | `must be LOW, MEDIUM, or HIGH` | listByPriority |
| `sortBy`, `order`, `page`, `pageSize` | see `listTasks` | listTasks |
| `title` (wrong JSON type from the model) | `must be a string or null` | inside 502 for createTaskFromText |
| `subtasks[i]` | `expected one line as title\|description but got: ...` | inside 502 for breakDownTask |
| `subtasks[i].title` etc. | the normal rule that failed for subtask number `i` (counted from 0) | inside 502 for breakDownTask |

## Idempotency

`createTask`, `createTaskFromText` and `breakDownTask` accept an `idempotencyKey` so a client can retry safely after a network failure.

| Situation | Result |
|---|---|
| Key is `null` | No idempotency: every call runs. |
| Key is blank (`""`, `"  "`) | 400, `idempotencyKey: must not be blank when given`. |
| First use of a key, operation succeeds | The response is stored with a fingerprint of the request. |
| First use of a key, operation fails | Nothing is stored. The client can fix the input and retry with the same key. |
| Same key, same operation, same request | The stored response is returned unchanged (same status and body). Nothing is created, and the parser or model is **not** called again. |
| Same key, different request or different operation | 422 `IDEMPOTENCY_CONFLICT`. |

The fingerprint is the operation name plus every request value: all `TaskRequest` fields for `createTask`, the text for `createTaskFromText`, the task ID for `breakDownTask`. Keys are kept for as long as the program runs. A stored response is returned even if the task was later changed or deleted, because it is the answer to the original request.

The idempotency check runs before anything else, so a repeated key never reaches validation, the repository or the model.

## Operations

### createTask(TaskRequest request, String idempotencyKey)

HTTP: `POST /tasks` with an `Idempotency-Key` header.

Creates a top-level task.

- **201** with the new Task.
- **400** `VALIDATION_ERROR`: any rule in [The Task object](#the-task-object), a null request, or a blank key.
- **422** `IDEMPOTENCY_CONFLICT`.

### getTask(int id)

HTTP: `GET /tasks/{id}`

- **200** with the Task.
- **404** `NOT_FOUND`.

### listTasks(TaskQuery query)

HTTP: `GET /tasks?completed=&priority=&search=&sortBy=&order=&page=&pageSize=`

All query fields are optional. `null` for the whole query means "no filters, first page".

| Field | Meaning | Default | Invalid value (400 detail) |
|---|---|---|---|
| `completed` | `true` = only completed, `false` = only open | both | (it is a Boolean, so it cannot be invalid) |
| `priority` | only this priority | any | `priority: must be LOW, MEDIUM, or HIGH` |
| `search` | case-insensitive text in title or description | none | (blank means no search) |
| `sortBy` | `createdAt` or `dueDate`, plus any sort added with `registerSortStrategy` | `createdAt` | `sortBy: must be one of createdAt, dueDate` |
| `order` | `asc` or `desc` (lower case) | `asc` | `order: must be "asc" or "desc"` |
| `page` | page number, 1 is the first page | 1 | `page: must be at least 1` |
| `pageSize` | items per page | 10 | `pageSize: must be between 1 and 100` |

Filters are combined with AND. Ties are broken by ID. With `sortBy=dueDate`, tasks without a due date come last in both directions.

- **200**:

  ```json
  { "items": [ Task, ... ], "page": 1, "pageSize": 10, "totalItems": 3, "totalPages": 1 }
  ```

  A page after the last page is not an error. It returns `"items": []` with the real `totalItems` and `totalPages`.
- **400** `VALIDATION_ERROR`, listing every bad field.

### listByPriority(String level)

HTTP: `GET /tasks/priority/{level}`

- **200** with `{ "items": [ Task, ... ], "totalItems": n }`, oldest first.
- **400** `VALIDATION_ERROR`, `level: must be LOW, MEDIUM, or HIGH` (also for `null`, `""` and lower case).

### updateTask(int id, TaskRequest request)

HTTP: `PUT /tasks/{id}`

Replaces every field with the request's values, using the same rules as `createTask`. `id`, `parentId` and `createdAt` never change. Fires `UPDATED`, then `COMPLETED` if `completed` went from false to true.

- **200** with the updated Task.
- **404** `NOT_FOUND`. This is checked first.
- **400** `VALIDATION_ERROR`. The task is left unchanged.

### deleteTask(int id)

HTTP: `DELETE /tasks/{id}`

Deletes the task and all of its subtasks.

- **200** with `{ "deletedId": 6, "deletedSubtaskIds": [7, 8, 9] }`.
- **404** `NOT_FOUND`.

### createTaskFromText(String text, String idempotencyKey)

HTTP: `POST /tasks/from-text` with body `{ "text": "..." }` and an `Idempotency-Key` header.

Sends the text and today's date to the configured `TaskParser` (the language model, or the offline rule-based parser), then validates the answer with the same rules as `createTask`. New tasks are not completed and have priority `MEDIUM`.

- **201** with the new Task.
- **400** `VALIDATION_ERROR`: `text` is null or blank (the parser is not called), or the key is blank.
- **422** `IDEMPOTENCY_CONFLICT`.
- **502** `BAD_GATEWAY`: the model call failed or timed out, the reply was empty or not a JSON object, a field had the wrong JSON type, or a value broke a rule (empty title, impossible date, unknown recurrence). Nothing is saved.
- **503** `SERVICE_UNAVAILABLE`: LLM mode without `LLM_API_KEY`, or the provider rejected the key.

### createSubtask(int parentId, TaskRequest request)

HTTP: `POST /tasks/{parentId}/subtasks`

- **201** with the new Task (`parentId` set).
- **404** `NOT_FOUND`: no task has `parentId`. This is checked first.
- **400** `VALIDATION_ERROR`: the parent is itself a subtask (`parentId` detail), or the request breaks a rule.

### listSubtasks(int parentId)

HTTP: `GET /tasks/{parentId}/subtasks`

- **200** with `{ "parentId": 6, "items": [ Task, ... ], "totalItems": n }`, in ID order.
- **404** `NOT_FOUND`.

### breakDownTask(int id, String idempotencyKey)

HTTP: `POST /tasks/{id}/breakdown` with an `Idempotency-Key` header.

Asks the configured `BreakdownStrategy` for subtasks. Every proposed subtask is validated **before any is saved**, so either all are saved or none are. Each subtask gets the parent's priority and due date.

- **201** with `{ "parentId": 6, "items": [ Task, ... ], "totalItems": n }`.
- **404** `NOT_FOUND`.
- **400** `VALIDATION_ERROR`: the task is itself a subtask (the strategy is not called), or the key is blank.
- **422** `IDEMPOTENCY_CONFLICT`.
- **502** `BAD_GATEWAY`: the model call failed, a line was not `title|description`, more than 10 subtasks were returned, no subtasks were returned, or any subtask broke a rule. Nothing is saved.
- **503** `SERVICE_UNAVAILABLE`: LLM mode without `LLM_API_KEY`.

## Events

These are not part of the HTTP surface, but other code can rely on them. After a successful change, `TaskService` calls every registered `TaskEventListener`:

| Event | When |
|---|---|
| `CREATED` | after `createTask`, `createTaskFromText`, `createSubtask`, and once per subtask saved by `breakDownTask` |
| `UPDATED` | after every successful `updateTask` |
| `COMPLETED` | after `UPDATED`, when `completed` changed from false to true |
| `DELETED` | once per deleted task (subtasks first, then the parent) |

Failed operations send no events. A listener that throws is logged and skipped, and the operation still succeeds.
