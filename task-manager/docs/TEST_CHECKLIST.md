# Test checklist

This is the checklist `TestRunner` is built from. The first draft of the checklist and of `TestRunner` was generated with an AI assistant. Items marked **(review)** were missing from that draft and were added after comparing it with the brief. See "Testing" in the [README](../README.md) for what the review found.

Run it with:

```bash
java -cp out com.airtribe.taskmanager.test.TestRunner
```

## CRUD

- [x] Create a valid task: 201, ID 1, priority defaults to MEDIUM
- [x] Get an existing task: 200
- [x] Update a task: 200, fields changed
- [x] Delete a task: 200, then 404 on get
- [x] List tasks: 200 with every task
- [x] **(review)** Update keeps `id`, `parentId` and `createdAt`
- [x] **(review)** A deleted ID is never reused

## Validation and error shape

- [x] Empty title: 400 VALIDATION_ERROR
- [x] **(review)** Many problems in one request: every one is listed in `details`
- [x] **(review)** `completed` must be exactly "true" or "false" ("TRUE", "False", "1", "", " true" rejected; missing means false)
- [x] **(review)** Impossible dates rejected: 2026-02-30, 2026-02-29, 1900-02-29, 2026-13-01, 2026-00-10, 2026-04-31, 0000-01-01
- [x] **(review)** Badly formatted dates rejected: 2026-1-5, 2026/01/05, "tomorrow"
- [x] **(review)** Real dates accepted: 2028-02-29, 2000-02-29, 2026-12-31, 2026-04-30
- [x] **(review)** Priority must be LOW, MEDIUM or HIGH exactly ("high" rejected)
- [x] **(review)** Blank title and a title over 200 characters rejected
- [x] **(review)** Null request: 400, not a crash
- [x] **(review)** Invalid update: 400 and the task is unchanged
- [x] **(review)** Error shape checked in the JSON text for 400, 404, 422, 500, 502 and 503: string `code`, string `message`, list `details`
- [x] **(review)** Unexpected exception: 500 INTERNAL_ERROR, internal message not sent to the client

## Unknown IDs

- [x] getTask with an unknown ID: 404
- [x] **(review)** updateTask, deleteTask, listSubtasks, createSubtask, breakDownTask with an unknown ID: 404; also IDs 0 and -1

## Idempotency

- [x] Same key twice: one task
- [x] **(review)** Same key twice: the second response is identical to the first
- [x] **(review)** Same key with a different request: 422, nothing created
- [x] **(review)** A failed request is not stored, so the fixed request can reuse the key
- [x] **(review)** Blank key: 400; null key: no idempotency
- [x] **(review)** One key used by two different operations: 422
- [x] **(review)** Repeated `createTaskFromText` with a key: the model is called once
- [x] **(review)** Repeated `breakDownTask` with a key: subtasks created once, model called once
- [x] **(review)** A failed AI call is not stored; a retry with the same key can succeed
- [x] **(review)** 8 threads with the same key at the same time: one task

## AI task creation (fake and faulty models)

- [x] Fake model reply: 201 with the model's title and recurrence
- [x] Empty title from the model: 502, **(review)** and nothing saved
- [x] **(review)** Impossible date, wrong JSON type, malformed text, empty reply, exception: 502 and nothing saved
- [x] **(review)** LLM mode without a key: 503 and nothing saved
- [x] **(review)** JSON wrapped in a code fence is still read
- [x] **(review)** The prompt contains today's date, the weekday and the user's text
- [x] **(review)** Empty text: 400 and the model is not called
- [x] **(review)** A parser that crashes: 502, not 500

## Rule-based parser (offline)

- [x] **(review)** "Remind me to pay rent on the 1st of every month": Pay rent, 2026-12-01, MONTHLY
- [x] **(review)** "Call mom tomorrow": 2026-11-28
- [x] **(review)** "Submit timesheet every friday": WEEKLY, 2026-12-04 (this case found a bug)
- [x] **(review)** The 31st skips months that are too short
- [x] **(review)** Text that leaves no title: 502 and nothing saved

## Subtasks and breakdown

- [x] Fake model breakdown: 201 with 3 subtasks
- [x] **(review)** A subtask cannot get subtasks: 400
- [x] **(review)** A subtask cannot be broken down: 400 and the model is not called
- [x] **(review)** One badly formatted line: 502, no subtask saved
- [x] **(review)** One subtask with an empty title: 502, no subtask saved
- [x] **(review)** Model failure during breakdown: 502; no key: 503
- [x] **(review)** Subtasks copy the parent's priority and due date
- [x] **(review)** Rule-based breakdown splits on commas and "and"; nothing to split: 502
- [x] **(review)** Deleting a parent deletes its subtasks and nothing else
- [x] **(review)** listSubtasks returns only that parent's subtasks

## Filtering, search, sorting and pagination

- [x] listByPriority returns only that priority
- [x] **(review)** A page beyond the last page: 200, empty items, correct totals
- [x] **(review)** page 0, pageSize 0 and pageSize 101: 400
- [x] **(review)** Bad sortBy, order and priority are all reported together
- [x] **(review)** Filter, search, sort and paging together in one call, checked against exact IDs
- [x] **(review)** Tasks without a due date come last, ascending and descending
- [x] **(review)** Sort by createdAt, newest first
- [x] **(review)** listByPriority rejects "high", "URGENT", "" and null
- [x] **(review)** A new sort and a new filter plug in without changing existing classes

## Events

- [x] **(review)** CREATED, UPDATED, COMPLETED (only on the change to completed) and DELETED counts
- [x] **(review)** Failed operations send no events
- [x] **(review)** A removed listener gets no more events
- [x] **(review)** A listener that throws does not break the operation or the other listeners

## Design checks

- [x] **(review)** The repository and idempotency store are singletons
- [x] **(review)** `Task.Builder` rejects a blank title, a null description and an impossible date
- [x] **(review)** SimpleJson round trip with quotes, newlines, tabs, backslashes and accents
- [x] **(review)** ParserFactory picks LLM or rule-based from the key and the mode

## Real HTTP client (local server, no internet)

- [x] **(review)** Sends the key, version, model and an escaped prompt; returns only the text blocks
- [x] **(review)** HTTP 500: 502; HTTP 401: 503; refusal: 502; not JSON: 502; timeout: 502
- [x] **(review)** No key: 503 without any network call
