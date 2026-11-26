package com.airtribe.taskmanager.test;

import com.airtribe.taskmanager.api.ApiResponse;
import com.airtribe.taskmanager.api.IdempotencyStore;
import com.airtribe.taskmanager.api.TaskApi;
import com.airtribe.taskmanager.event.AuditTrailListener;
import com.airtribe.taskmanager.event.TaskEventListener;
import com.airtribe.taskmanager.event.TaskEventType;
import com.airtribe.taskmanager.exception.ApiException;
import com.airtribe.taskmanager.exception.BadGatewayException;
import com.airtribe.taskmanager.exception.ServiceUnavailableException;
import com.airtribe.taskmanager.llm.AnthropicLlmClient;
import com.airtribe.taskmanager.llm.LlmClient;
import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;
import com.airtribe.taskmanager.parser.BreakdownStrategy;
import com.airtribe.taskmanager.parser.ParsedTask;
import com.airtribe.taskmanager.parser.ParserFactory;
import com.airtribe.taskmanager.parser.RuleBasedTaskParser;
import com.airtribe.taskmanager.parser.TaskParser;
import com.airtribe.taskmanager.query.SortStrategy;
import com.airtribe.taskmanager.query.TaskQuery;
import com.airtribe.taskmanager.repository.InMemoryTaskRepository;
import com.airtribe.taskmanager.repository.TaskRepository;
import com.airtribe.taskmanager.service.TaskService;
import com.airtribe.taskmanager.util.DateHelper;
import com.airtribe.taskmanager.util.SimpleJson;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Calls TaskApi and prints PASS or FAIL for each case. No test library, no API key, no network:
 * the language model is replaced by {@link FakeLlmClient} and {@link FaultyLlmClient}, plugged in
 * through {@link ParserFactory}. "Today" is fixed at 2026-11-27 (a Friday) so dates are predictable.
 *
 * <p>The first group is the AI-generated first draft. Every group after it was added after
 * reviewing that draft (see "Testing" in the README). Exit code 0 when every case passes, 1 otherwise.
 */
public final class TestRunner {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-27T10:00:00Z"), ZoneOffset.UTC);
    private static final String PAY_RENT_JSON =
            "{\"title\": \"Pay rent\", \"description\": \"Pay the monthly rent\", \"dueDate\": \"2026-12-01\", \"recurrence\": \"MONTHLY\"}";
    private static final String OFFSITE_BREAKDOWN =
            "Book venue|Find and book a place\nSend invites|Invite the whole team\nOrder food|Order catering";

    private final InMemoryTaskRepository repository = InMemoryTaskRepository.getInstance();
    private final IdempotencyStore idempotency = IdempotencyStore.getInstance();
    private final List<String> failures = new ArrayList<>();
    private int passed;

    /** The service behind the most recent api() / llmApi() call, for listener tests. */
    private TaskService service;

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.runAll();
        System.exit(runner.failures.isEmpty() ? 0 : 1);
    }

    private void runAll() {
        firstDraft();
        validation();
        errorShape();
        unknownIds();
        idempotencyCases();
        aiParsing();
        ruleBasedParsing();
        subtasksAndBreakdown();
        listing();
        events();
        designChecks();
        httpClient();

        System.out.println();
        System.out.println(passed + " passed, " + failures.size() + " failed");
        for (String failure : failures) {
            System.out.println("  FAILED: " + failure);
        }
    }

    // ================================================================== first draft (AI-generated)

    private void firstDraft() {
        group("First draft (AI-generated)");

        test("createTask with valid input returns 201 and the task", () -> {
            ApiResponse r = api().createTask(request("Buy milk"), null);
            expectStatus(r, 201);
            check(body(r).get("id").equals(1), "first task should get id 1");
            check("MEDIUM".equals(body(r).get("priority")), "priority should default to MEDIUM");
        });

        test("createTask with empty title returns 400", () ->
                expectError(api().createTask(new TaskRequest("", "desc", "false", null, null), null), 400, "VALIDATION_ERROR"));

        test("getTask returns 200 for an existing task", () -> {
            TaskApi api = api();
            api.createTask(request("Buy milk"), null);
            expectStatus(api.getTask(1), 200);
        });

        test("getTask returns 404 for an unknown id", () -> expectError(api().getTask(42), 404, "NOT_FOUND"));

        test("updateTask changes the fields and returns 200", () -> {
            TaskApi api = api();
            api.createTask(request("Buy milk"), null);
            ApiResponse r = api.updateTask(1, new TaskRequest("Buy oat milk", "desc", "true", null, "HIGH"));
            expectStatus(r, 200);
            check("Buy oat milk".equals(body(r).get("title")), "title should be updated");
            check(Boolean.TRUE.equals(body(r).get("completed")), "completed should be true");
        });

        test("deleteTask removes the task", () -> {
            TaskApi api = api();
            api.createTask(request("Buy milk"), null);
            expectStatus(api.deleteTask(1), 200);
            expectError(api.getTask(1), 404, "NOT_FOUND");
        });

        test("listTasks returns every task", () -> {
            TaskApi api = api();
            api.createTask(request("A"), null);
            api.createTask(request("B"), null);
            ApiResponse r = api.listTasks(TaskQuery.all());
            expectStatus(r, 200);
            check(items(r).size() == 2, "expected 2 items");
        });

        test("createTaskFromText with the fake model returns 201", () -> {
            ApiResponse r = llmApi(new FakeLlmClient(PAY_RENT_JSON))
                    .createTaskFromText("Remind me to pay rent on the 1st of every month", null);
            expectStatus(r, 201);
            check("Pay rent".equals(body(r).get("title")), "title from the model");
            check("MONTHLY".equals(body(r).get("recurrence")), "recurrence from the model");
        });

        // Review: the draft only checked the status. It now also checks that nothing was saved.
        test("createTaskFromText with an empty title from the model returns 502 and saves nothing", () -> {
            ApiResponse r = llmApi(new FaultyLlmClient(FaultyLlmClient.Fault.EMPTY_TITLE)).createTaskFromText("Do something", null);
            expectError(r, 502, "BAD_GATEWAY");
            expectDetail(r, "title");
            expectStoreSize(0);
        });

        test("breakDownTask with the fake model creates the subtasks", () -> {
            TaskApi api = llmApi(new FakeLlmClient(OFFSITE_BREAKDOWN));
            api.createTask(request("Plan offsite"), null);
            ApiResponse r = api.breakDownTask(1, null);
            expectStatus(r, 201);
            check(items(r).size() == 3, "expected 3 subtasks");
        });

        // Review: the draft did not check that the retry got the same response back.
        test("createTask twice with the same key creates one task and returns the same response", () -> {
            TaskApi api = api();
            ApiResponse first = api.createTask(request("Buy milk"), "key-1");
            ApiResponse second = api.createTask(request("Buy milk"), "key-1");
            expectStoreSize(1);
            check(first.toJson().equals(second.toJson()), "retry should return the stored response");
            check(second.getStatus() == 201, "retry status should be the original 201");
        });

        test("listByPriority returns only tasks with that priority", () -> {
            TaskApi api = api();
            api.createTask(new TaskRequest("A", "d", "false", null, "HIGH"), null);
            api.createTask(new TaskRequest("B", "d", "false", null, "LOW"), null);
            ApiResponse r = api.listByPriority("HIGH");
            expectStatus(r, 200);
            check(items(r).size() == 1, "expected 1 HIGH task");
        });
    }

    // ================================================================== added after review

    private void validation() {
        group("Validation");

        test("one request with many problems reports all of them", () -> {
            ApiResponse r = api().createTask(
                    new TaskRequest("  ", null, "yes", "2026-02-30", "urgent", "SOMETIMES"), null);
            expectError(r, 400, "VALIDATION_ERROR");
            for (String field : new String[] {"title", "description", "completed", "dueDate", "priority", "recurrence"}) {
                expectDetail(r, field);
            }
            check(details(r).size() == 6, "expected 6 details, got " + details(r).size());
            expectStoreSize(0);
        });

        test("completed must be exactly \"true\" or \"false\"", () -> {
            for (String bad : new String[] {"TRUE", "False", "1", "", " true"}) {
                ApiResponse r = api().createTask(new TaskRequest("T", "D", bad, null, null), null);
                expectError(r, 400, "VALIDATION_ERROR");
                expectDetail(r, "completed");
            }
            ApiResponse missing = api().createTask(new TaskRequest("T", "D", null, null, null), null);
            expectStatus(missing, 201);
            check(Boolean.FALSE.equals(body(missing).get("completed")), "missing completed should default to false");
        });

        test("impossible or badly formatted due dates are rejected", () -> {
            String[] bad = {"2026-02-30", "2026-02-29", "1900-02-29", "2026-13-01", "2026-00-10", "2026-04-31",
                    "2026-1-5", "2026/01/05", "tomorrow", "0000-01-01"};
            for (String date : bad) {
                ApiResponse r = api().createTask(new TaskRequest("T", "D", "false", date, null), null);
                check(r.getStatus() == 400, date + " should be rejected but got " + r);
                expectDetail(r, "dueDate");
            }
        });

        test("real dates are accepted, including leap days", () -> {
            for (String date : new String[] {"2028-02-29", "2000-02-29", "2026-12-31", "2026-04-30"}) {
                expectStatus(api().createTask(new TaskRequest("T", "D", "false", date, null), null), 201);
            }
        });

        test("priority must be LOW, MEDIUM or HIGH exactly", () -> {
            for (String bad : new String[] {"high", "Urgent", "CRITICAL"}) {
                expectDetail(api().createTask(new TaskRequest("T", "D", "false", null, bad), null), "priority");
            }
        });

        test("blank and over-long titles are rejected", () -> {
            expectDetail(api().createTask(new TaskRequest(" \t ", "D", "false", null, null), null), "title");
            String longTitle = "x".repeat(201);
            expectDetail(api().createTask(new TaskRequest(longTitle, "D", "false", null, null), null), "title");
        });

        test("a null request is a 400, not a crash", () -> {
            ApiResponse r = api().createTask(null, null);
            expectError(r, 400, "VALIDATION_ERROR");
            expectDetail(r, "request");
        });

        test("invalid update returns 400 and leaves the task unchanged", () -> {
            TaskApi api = api();
            api.createTask(request("Original"), null);
            expectError(api.updateTask(1, new TaskRequest("", "D", "false", null, null)), 400, "VALIDATION_ERROR");
            check("Original".equals(body(api.getTask(1)).get("title")), "task should be unchanged");
        });

        test("update keeps id, parentId and createdAt", () -> {
            TaskApi api = api();
            api.createTask(request("Parent"), null);
            ApiResponse sub = api.createSubtask(1, request("Child"));
            ApiResponse updated = api.updateTask(2, new TaskRequest("Child v2", "D", "false", null, "LOW"));
            expectStatus(updated, 200);
            check(Integer.valueOf(1).equals(body(updated).get("parentId")), "parentId kept");
            check(body(sub).get("createdAt").equals(body(updated).get("createdAt")), "createdAt kept");
        });
    }

    private void errorShape() {
        group("Error shape");

        test("every error status uses the same JSON shape with string code and message", () -> {
            TaskApi api = api();
            api.createTask(request("A"), "k");
            List<ApiResponse> errors = List.of(
                    api.createTask(new TaskRequest("", "D", "false", null, null), null),          // 400
                    api.getTask(99),                                                              // 404
                    api.createTask(request("B"), "k"),                                            // 422
                    llmApi(new FaultyLlmClient(FaultyLlmClient.Fault.MALFORMED)).createTaskFromText("x", null), // 502
                    noKeyApi().createTaskFromText("x", null),                                     // 503
                    brokenRepositoryApi().createTask(request("C"), null));                        // 500
            int[] statuses = {400, 404, 422, 502, 503, 500};
            for (int i = 0; i < errors.size(); i++) {
                ApiResponse r = errors.get(i);
                check(r.getStatus() == statuses[i], "expected " + statuses[i] + " but got " + r);
                Map<String, Object> parsed = SimpleJson.parseObject(r.toJson()); // check the JSON text itself
                check(parsed.size() == 1 && parsed.get("error") instanceof Map, "body must be {\"error\": {...}}: " + r);
                Map<?, ?> error = (Map<?, ?>) parsed.get("error");
                check(error.get("code") instanceof String, "code must be a string: " + r);
                check(error.get("message") instanceof String, "message must be a string: " + r);
                check(error.get("details") instanceof List, "details must be a list: " + r);
                for (Object detail : (List<?>) error.get("details")) {
                    Map<?, ?> d = (Map<?, ?>) detail;
                    check(d.get("field") instanceof String && d.get("issue") instanceof String,
                            "each detail needs string field and issue: " + r);
                }
            }
        });

        test("an unexpected exception becomes a 500 INTERNAL_ERROR without leaking its message", () -> {
            ApiResponse r = brokenRepositoryApi().createTask(request("C"), null);
            expectError(r, 500, "INTERNAL_ERROR");
            check(!r.toJson().contains("disk on fire"), "internal message must not be sent to the client");
        });
    }

    private void unknownIds() {
        group("Unknown IDs");

        test("every operation that takes an id returns 404 for an unknown one", () -> {
            TaskApi api = api();
            expectError(api.updateTask(7, request("X")), 404, "NOT_FOUND");
            expectError(api.deleteTask(7), 404, "NOT_FOUND");
            expectError(api.listSubtasks(7), 404, "NOT_FOUND");
            expectError(api.createSubtask(7, request("X")), 404, "NOT_FOUND");
            expectError(api.breakDownTask(7, null), 404, "NOT_FOUND");
            expectError(api.getTask(0), 404, "NOT_FOUND");
            expectError(api.getTask(-1), 404, "NOT_FOUND");
        });

        test("a deleted id stays unknown and is never reused", () -> {
            TaskApi api = api();
            api.createTask(request("A"), null);
            api.deleteTask(1);
            ApiResponse next = api.createTask(request("B"), null);
            check(Integer.valueOf(2).equals(body(next).get("id")), "ids must not be reused");
        });
    }

    private void idempotencyCases() {
        group("Idempotency");

        test("same key with a different request returns 422 and creates nothing", () -> {
            TaskApi api = api();
            api.createTask(request("Buy milk"), "key-1");
            expectError(api.createTask(request("Buy bread"), "key-1"), 422, "IDEMPOTENCY_CONFLICT");
            expectStoreSize(1);
        });

        test("errors are not stored, so a fixed request can reuse the key", () -> {
            TaskApi api = api();
            expectStatus(api.createTask(new TaskRequest("", "D", "false", null, null), "key-2"), 400);
            expectStatus(api.createTask(request("Fixed"), "key-2"), 201);
            expectStatus(api.createTask(request("Fixed"), "key-2"), 201);
            expectStoreSize(1);
        });

        test("a blank key is rejected; a null key means no idempotency", () -> {
            TaskApi api = api();
            expectDetail(api.createTask(request("A"), "   "), "idempotencyKey");
            api.createTask(request("A"), null);
            api.createTask(request("A"), null);
            expectStoreSize(2);
        });

        test("one key cannot be reused across different operations", () -> {
            TaskApi api = llmApi(new FakeLlmClient(PAY_RENT_JSON));
            expectStatus(api.createTask(request("A"), "shared"), 201);
            expectError(api.createTaskFromText("A", "shared"), 422, "IDEMPOTENCY_CONFLICT");
        });

        test("repeating createTaskFromText with a key never calls the model twice", () -> {
            FakeLlmClient fake = new FakeLlmClient(PAY_RENT_JSON);
            TaskApi api = llmApi(fake);
            ApiResponse first = api.createTaskFromText("pay rent monthly", "text-1");
            ApiResponse second = api.createTaskFromText("pay rent monthly", "text-1");
            expectStatus(first, 201);
            check(first.toJson().equals(second.toJson()), "same response expected");
            check(fake.getCalls() == 1, "model should be called once, was called " + fake.getCalls());
            expectStoreSize(1);
        });

        test("repeating breakDownTask with a key creates the subtasks once", () -> {
            FakeLlmClient fake = new FakeLlmClient(OFFSITE_BREAKDOWN);
            TaskApi api = llmApi(fake);
            api.createTask(request("Plan offsite"), null);
            api.breakDownTask(1, "bd-1");
            api.breakDownTask(1, "bd-1");
            check(fake.getCalls() == 1, "model should be called once, was called " + fake.getCalls());
            expectStoreSize(4);
        });

        test("a failed AI call is not stored, so the client can retry with the same key", () -> {
            TaskApi faulty = llmApi(new FaultyLlmClient(FaultyLlmClient.Fault.THROWS));
            expectStatus(faulty.createTaskFromText("pay rent", "retry"), 502);
            FakeLlmClient fake = new FakeLlmClient(PAY_RENT_JSON);
            TaskApi working = new TaskApi(new TaskService(repository, ParserFactory.createTaskParser(fake),
                    ParserFactory.createBreakdownStrategy(fake), FIXED_CLOCK), idempotency);
            expectStatus(working.createTaskFromText("pay rent", "retry"), 201);
        });

        test("eight threads retrying the same key at once create one task", () -> {
            TaskApi api = api();
            List<ApiResponse> responses = runConcurrently(8, () -> api.createTask(request("Concurrent"), "same-key"));
            expectStoreSize(1);
            for (ApiResponse r : responses) {
                check(r.getStatus() == 201 && Integer.valueOf(1).equals(body(r).get("id")), "every thread should get task 1: " + r);
            }
        });
    }

    private void aiParsing() {
        group("AI task creation with fake and faulty models");

        Object[][] faults = {
                {FaultyLlmClient.Fault.IMPOSSIBLE_DATE, "dueDate"},
                {FaultyLlmClient.Fault.WRONG_TYPE, "title"},
                {FaultyLlmClient.Fault.MALFORMED, null},
                {FaultyLlmClient.Fault.EMPTY_REPLY, null},
                {FaultyLlmClient.Fault.THROWS, null},
        };
        for (Object[] row : faults) {
            FaultyLlmClient.Fault fault = (FaultyLlmClient.Fault) row[0];
            String field = (String) row[1];
            test("model fault " + fault + " returns 502 and saves nothing", () -> {
                ApiResponse r = llmApi(new FaultyLlmClient(fault)).createTaskFromText("Pay rent on the 1st", null);
                expectError(r, 502, "BAD_GATEWAY");
                if (field != null) {
                    expectDetail(r, field);
                }
                expectStoreSize(0);
            });
        }

        test("LLM mode without an API key returns 503 and saves nothing", () -> {
            expectError(noKeyApi().createTaskFromText("Pay rent", null), 503, "SERVICE_UNAVAILABLE");
            expectStoreSize(0);
        });

        test("JSON wrapped in a code fence is still read", () -> {
            String fenced = "```json\n" + PAY_RENT_JSON + "\n```";
            expectStatus(llmApi(new FakeLlmClient(fenced)).createTaskFromText("pay rent", null), 201);
        });

        test("the prompt includes today's date and weekday and the user's text", () -> {
            FakeLlmClient fake = new FakeLlmClient(PAY_RENT_JSON);
            llmApi(fake).createTaskFromText("Pay rent next Friday", null);
            check(fake.getLastPrompt().contains("2026-11-27 (FRIDAY)"), "prompt should contain today's date");
            check(fake.getLastPrompt().contains("Pay rent next Friday"), "prompt should contain the text");
        });

        test("empty text returns 400 without calling the model", () -> {
            FakeLlmClient fake = new FakeLlmClient(PAY_RENT_JSON);
            expectError(llmApi(fake).createTaskFromText("   ", null), 400, "VALIDATION_ERROR");
            check(fake.getCalls() == 0, "model must not be called for empty text");
        });

        test("a parser that crashes is reported as 502, not 500", () -> {
            TaskParser crashing = (text, today) -> {
                throw new NullPointerException("bug in parser");
            };
            TaskApi api = build(crashing, ParserFactory.createBreakdownStrategy(ParserFactory.Mode.RULE_BASED, null));
            expectError(api.createTaskFromText("anything", null), 502, "BAD_GATEWAY");
        });
    }

    private void ruleBasedParsing() {
        group("Rule-based parser (offline)");

        test("\"Remind me to pay rent on the 1st of every month\"", () -> {
            ParsedTask p = new RuleBasedTaskParser().parse("Remind me to pay rent on the 1st of every month", "2026-11-27");
            check("Pay rent".equals(p.getTitle()), "title was " + p.getTitle());
            check("2026-12-01".equals(p.getDueDate()), "dueDate was " + p.getDueDate());
            check("MONTHLY".equals(p.getRecurrence()), "recurrence was " + p.getRecurrence());
        });

        test("\"Call mom tomorrow\" is due 2026-11-28", () -> {
            ParsedTask p = new RuleBasedTaskParser().parse("Call mom tomorrow", "2026-11-27");
            check("Call mom".equals(p.getTitle()) && "2026-11-28".equals(p.getDueDate()), "got " + p);
        });

        test("\"Submit timesheet every friday\" is weekly from next Friday", () -> {
            ParsedTask p = new RuleBasedTaskParser().parse("Submit timesheet every friday", "2026-11-27");
            check("Submit timesheet".equals(p.getTitle()), "title was " + p.getTitle());
            check("2026-12-04".equals(p.getDueDate()) && "WEEKLY".equals(p.getRecurrence()), "got " + p);
        });

        test("\"the 31st\" skips months without a 31st", () -> {
            check("2026-12-31".equals(DateHelper.nextDayOfMonth("2026-11-27", 31)), "December has a 31st");
            check("2027-03-31".equals(DateHelper.nextDayOfMonth("2027-02-01", 31)), "February does not");
        });

        test("text that leaves no title returns 502 and saves nothing", () -> {
            expectError(api().createTaskFromText("tomorrow", null), 502, "BAD_GATEWAY");
            expectStoreSize(0);
        });
    }

    private void subtasksAndBreakdown() {
        group("Subtasks and breakdown");

        test("a subtask cannot get subtasks (400)", () -> {
            TaskApi api = api();
            api.createTask(request("Parent"), null);
            api.createSubtask(1, request("Child"));
            ApiResponse r = api.createSubtask(2, request("Grandchild"));
            expectError(r, 400, "VALIDATION_ERROR");
            expectDetail(r, "parentId");
            expectStoreSize(2);
        });

        test("a subtask cannot be broken down (400) and the model is not called", () -> {
            FakeLlmClient fake = new FakeLlmClient(OFFSITE_BREAKDOWN);
            TaskApi api = llmApi(fake);
            api.createTask(request("Parent"), null);
            api.createSubtask(1, request("Child"));
            expectError(api.breakDownTask(2, null), 400, "VALIDATION_ERROR");
            check(fake.getCalls() == 0, "model must not be called");
        });

        test("one badly formatted line means no subtask is saved (502)", () -> {
            TaskApi api = llmApi(new FaultyLlmClient(FaultyLlmClient.Fault.BAD_BREAKDOWN_LINE));
            api.createTask(request("Plan offsite"), null);
            ApiResponse r = api.breakDownTask(1, null);
            expectError(r, 502, "BAD_GATEWAY");
            expectDetail(r, "subtasks[1]");
            expectStoreSize(1);
        });

        test("one invalid subtask means no subtask is saved (502)", () -> {
            TaskApi api = llmApi(new FaultyLlmClient(FaultyLlmClient.Fault.BREAKDOWN_EMPTY_TITLE));
            api.createTask(request("Plan offsite"), null);
            ApiResponse r = api.breakDownTask(1, null);
            expectError(r, 502, "BAD_GATEWAY");
            expectDetail(r, "subtasks[1].title");
            expectStoreSize(1);
        });

        test("a failing model during breakdown returns 502; no key returns 503", () -> {
            TaskApi api = llmApi(new FaultyLlmClient(FaultyLlmClient.Fault.THROWS));
            api.createTask(request("Plan offsite"), null);
            expectError(api.breakDownTask(1, null), 502, "BAD_GATEWAY");
            TaskApi noKey = noKeyApi();
            noKey.createTask(request("Plan offsite"), null);
            expectError(noKey.breakDownTask(1, null), 503, "SERVICE_UNAVAILABLE");
            expectStoreSize(1);
        });

        test("subtasks from a breakdown copy the parent's priority and due date", () -> {
            TaskApi api = llmApi(new FakeLlmClient(OFFSITE_BREAKDOWN));
            api.createTask(new TaskRequest("Plan offsite", "D", "false", "2027-01-15", "HIGH"), null);
            for (Map<String, Object> sub : items(api.breakDownTask(1, null))) {
                check("HIGH".equals(sub.get("priority")) && "2027-01-15".equals(sub.get("dueDate"))
                        && Integer.valueOf(1).equals(sub.get("parentId")), "got " + sub);
            }
        });

        test("rule-based breakdown splits on commas and \"and\"; nothing to split is 502", () -> {
            TaskApi api = api();
            api.createTask(new TaskRequest("Plan party", "book venue, send invites and order cake", "false", null, null), null);
            ApiResponse r = api.breakDownTask(1, null);
            expectStatus(r, 201);
            check(items(r).size() == 3, "expected 3 subtasks, got " + items(r).size());
            check("Book venue".equals(items(r).get(0).get("title")), "first subtask title");
            api.createTask(new TaskRequest("Relax", "Just rest", "false", null, null), null);
            expectError(api.breakDownTask(5, null), 502, "BAD_GATEWAY");
        });

        test("deleting a parent deletes its subtasks and nothing else", () -> {
            TaskApi api = api();
            api.createTask(request("Parent"), null);
            api.createTask(request("Unrelated"), null);
            api.createSubtask(1, request("Child 1"));
            api.createSubtask(1, request("Child 2"));
            ApiResponse r = api.deleteTask(1);
            expectStatus(r, 200);
            check(List.of(3, 4).equals(body(r).get("deletedSubtaskIds")), "deleted subtasks were " + body(r).get("deletedSubtaskIds"));
            expectStoreSize(1);
            expectStatus(api.getTask(2), 200);
        });

        test("listSubtasks returns only that parent's subtasks", () -> {
            TaskApi api = api();
            api.createTask(request("P1"), null);
            api.createTask(request("P2"), null);
            api.createSubtask(1, request("A"));
            api.createSubtask(2, request("B"));
            api.createSubtask(1, request("C"));
            ApiResponse r = api.listSubtasks(1);
            expectStatus(r, 200);
            check(ids(r).equals(List.of(3, 5)), "expected [3, 5] but got " + ids(r));
        });
    }

    private void listing() {
        group("Filtering, search, sorting and pagination");

        test("a page after the last page is 200 with no items", () -> {
            TaskApi api = api();
            for (int i = 1; i <= 12; i++) {
                api.createTask(request("Task " + i), null);
            }
            ApiResponse page2 = api.listTasks(TaskQuery.builder().page(2).build());
            check(items(page2).size() == 2, "page 2 of 12 should have 2 items");
            ApiResponse r = api.listTasks(TaskQuery.builder().page(3).build());
            expectStatus(r, 200);
            check(items(r).isEmpty(), "page 3 should be empty");
            check(Integer.valueOf(2).equals(body(r).get("totalPages")), "totalPages should be 2");
            check(Integer.valueOf(12).equals(body(r).get("totalItems")), "totalItems should be 12");
        });

        test("page and pageSize limits are enforced", () -> {
            TaskApi api = api();
            expectDetail(api.listTasks(TaskQuery.builder().page(0).build()), "page");
            expectDetail(api.listTasks(TaskQuery.builder().pageSize(0).build()), "pageSize");
            expectDetail(api.listTasks(TaskQuery.builder().pageSize(101).build()), "pageSize");
        });

        test("bad sortBy, order and priority are all reported", () -> {
            ApiResponse r = api().listTasks(TaskQuery.builder().sortBy("title").order("DESC").priority("low").build());
            expectError(r, 400, "VALIDATION_ERROR");
            check(details(r).size() == 3, "expected 3 details");
        });

        test("filter, search, sort and paging work together in one call", () -> {
            TaskApi api = api();
            api.createTask(new TaskRequest("Write report", "Q4", "false", "2026-12-10", "HIGH"), null);   // 1 match
            api.createTask(new TaskRequest("Review", "the REPORT", "false", "2026-12-03", "HIGH"), null); // 2 match
            api.createTask(new TaskRequest("Send report", "Q4", "true", "2026-12-12", "HIGH"), null);    // 3 completed
            api.createTask(new TaskRequest("Report travel", "Q4", "false", "2026-12-01", "LOW"), null);  // 4 LOW
            api.createTask(new TaskRequest("Report expenses", "Q4", "false", null, "HIGH"), null);       // 5 match, no date
            api.createTask(new TaskRequest("Clean desk", "tidy", "false", "2026-12-02", "HIGH"), null);  // 6 no "report"
            TaskQuery.Builder query = TaskQuery.builder().completed(false).priority("HIGH").search("Report")
                    .sortBy("dueDate").order("desc").pageSize(2);
            ApiResponse p1 = api.listTasks(query.page(1).build());
            ApiResponse p2 = api.listTasks(query.page(2).build());
            check(ids(p1).equals(List.of(1, 2)), "page 1 should be [1, 2] but was " + ids(p1));
            check(ids(p2).equals(List.of(5)), "page 2 should be [5] (no due date last) but was " + ids(p2));
            check(Integer.valueOf(3).equals(body(p1).get("totalItems")), "3 matches in total");
        });

        test("tasks without a due date come last in both directions", () -> {
            TaskApi api = api();
            api.createTask(new TaskRequest("A", "D", "false", null, null), null);
            api.createTask(new TaskRequest("B", "D", "false", "2026-12-05", null), null);
            api.createTask(new TaskRequest("C", "D", "false", "2026-12-01", null), null);
            check(ids(api.listTasks(TaskQuery.builder().sortBy("dueDate").build())).equals(List.of(3, 2, 1)), "asc");
            check(ids(api.listTasks(TaskQuery.builder().sortBy("dueDate").order("desc").build())).equals(List.of(2, 3, 1)), "desc");
        });

        test("createdAt sort, newest first", () -> {
            TaskApi api = api();
            api.createTask(request("A"), null);
            api.createTask(request("B"), null);
            api.createTask(request("C"), null);
            check(ids(api.listTasks(TaskQuery.builder().order("desc").build())).equals(List.of(3, 2, 1)), "desc order");
        });

        test("listByPriority rejects anything but LOW, MEDIUM, HIGH", () -> {
            TaskApi api = api();
            for (String bad : new String[] {"high", "URGENT", "", null}) {
                expectError(api.listByPriority(bad), 400, "VALIDATION_ERROR");
            }
        });

        test("a new filter and a new sort plug in without changing existing classes", () -> {
            TaskApi api = api();
            service.registerSortStrategy(new SortStrategy() {
                @Override
                public String name() {
                    return "title";
                }

                @Override
                public Comparator<Task> comparator(boolean ascending) {
                    Comparator<Task> byTitle = Comparator.comparing(Task::getTitle);
                    return ascending ? byTitle : byTitle.reversed();
                }
            });
            api.createTask(request("Banana"), null);
            api.createTask(request("Apple"), null);
            api.createTask(request("Cherry pie"), null);
            ApiResponse r = api.listTasks(TaskQuery.builder().sortBy("title")
                    .filter(task -> task.getTitle().length() <= 6).build());
            check(ids(r).equals(List.of(2, 1)), "expected [2, 1] but got " + ids(r));
        });
    }

    private void events() {
        group("Events (Observer)");

        test("listeners hear CREATED, UPDATED, COMPLETED and DELETED", () -> {
            TaskApi api = api();
            AuditTrailListener audit = new AuditTrailListener();
            service.addListener(audit);
            api.createTask(request("A"), null);
            api.createSubtask(1, request("B"));
            api.updateTask(1, new TaskRequest("A", "D", "true", null, null));
            api.updateTask(1, new TaskRequest("A2", "D", "true", null, null)); // already completed
            api.deleteTask(1);
            check(audit.getCount(TaskEventType.CREATED) == 2, "CREATED " + audit.getCounts());
            check(audit.getCount(TaskEventType.UPDATED) == 2, "UPDATED " + audit.getCounts());
            check(audit.getCount(TaskEventType.COMPLETED) == 1, "COMPLETED only once " + audit.getCounts());
            check(audit.getCount(TaskEventType.DELETED) == 2, "DELETED parent + subtask " + audit.getCounts());
        });

        test("failed operations send no events", () -> {
            TaskApi api = api();
            AuditTrailListener audit = new AuditTrailListener();
            service.addListener(audit);
            api.createTask(request(""), null);
            api.deleteTask(9);
            check(audit.getTotal() == 0, "expected no events, got " + audit.getCounts());
        });

        test("a removed listener gets no more events", () -> {
            TaskApi api = api();
            AuditTrailListener audit = new AuditTrailListener();
            service.addListener(audit);
            api.createTask(request("A"), null);
            service.removeListener(audit);
            api.createTask(request("B"), null);
            check(audit.getTotal() == 1, "expected 1 event, got " + audit.getTotal());
        });

        test("a listener that throws does not break the operation or other listeners", () -> {
            TaskApi api = api();
            List<TaskEventType> seen = Collections.synchronizedList(new ArrayList<>());
            TaskEventListener broken = (type, task) -> {
                throw new IllegalStateException("listener bug (expected in this test)");
            };
            service.addListener(broken);
            service.addListener((type, task) -> seen.add(type));
            expectStatus(api.createTask(request("A"), null), 201);
            check(seen.equals(List.of(TaskEventType.CREATED)), "second listener should still be called");
        });
    }

    private void designChecks() {
        group("Design checks");

        test("the repository and idempotency store are singletons", () -> {
            check(InMemoryTaskRepository.getInstance() == InMemoryTaskRepository.getInstance(), "repository");
            check(IdempotencyStore.getInstance() == IdempotencyStore.getInstance(), "idempotency store");
        });

        test("Task can only be built with valid required fields", () -> {
            expectThrows(() -> new Task.Builder("", "D", false));
            expectThrows(() -> new Task.Builder("T", null, false));
            expectThrows(() -> new Task.Builder("T", "D", false).dueDate("2026-02-30"));
            Task task = new Task.Builder("T", "D", false).build();
            check(task.getPriority() != null && task.getId() == 0, "defaults");
        });

        test("SimpleJson writes and reads back quotes, newlines and unicode", () -> {
            String tricky = "He said \"hi\"\n\tand left \\ é";
            Map<String, Object> parsed = SimpleJson.parseObject(SimpleJson.write(Map.of("text", tricky)));
            check(tricky.equals(parsed.get("text")), "round trip failed: " + parsed.get("text"));
        });

        test("ParserFactory picks the parser from the key and the mode", () -> {
            check(ParserFactory.resolve(ParserFactory.Mode.AUTO, null) == ParserFactory.Mode.RULE_BASED, "no key -> rule-based");
            check(ParserFactory.resolve(ParserFactory.Mode.AUTO, " ") == ParserFactory.Mode.RULE_BASED, "blank key -> rule-based");
            check(ParserFactory.resolve(ParserFactory.Mode.AUTO, "sk-test") == ParserFactory.Mode.LLM, "key -> LLM");
            check(ParserFactory.resolve(ParserFactory.Mode.RULE_BASED, "sk-test") == ParserFactory.Mode.RULE_BASED, "forced offline");
        });
    }

    private void httpClient() {
        group("AnthropicLlmClient against a local fake server (no internet)");

        test("sends the key, version, model and prompt, and returns only the text blocks", () -> {
            String reply = "{\"content\": [{\"type\": \"thinking\", \"thinking\": \"\"},"
                    + " {\"type\": \"text\", \"text\": \"Hello \"}, {\"type\": \"text\", \"text\": \"world\"}],"
                    + " \"stop_reason\": \"end_turn\"}";
            try (LocalLlmServer server = new LocalLlmServer(200, reply, 0)) {
                String text = new AnthropicLlmClient("test-key", null, Duration.ofSeconds(5), server.uri())
                        .complete("Say \"hi\"");
                check("Hello world".equals(text), "text was " + text);
                check("test-key".equals(server.getLastApiKey()), "x-api-key header");
                check("2026-10-21".equals(server.getLastVersion()), "anthropic-version header");
                Map<String, Object> sent = SimpleJson.parseObject(server.getLastBody());
                check(AnthropicLlmClient.DEFAULT_MODEL.equals(sent.get("model")), "model in body");
                check(server.getLastBody().contains("Say \\\"hi\\\""), "prompt must be JSON-escaped in the body");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });

        Object[][] cases = {
                {500, "{\"type\": \"error\"}", 0L, BadGatewayException.class, "HTTP 500"},
                {401, "{\"type\": \"error\"}", 0L, ServiceUnavailableException.class, "rejected"},
                {200, "{\"content\": [], \"stop_reason\": \"refusal\"}", 0L, BadGatewayException.class, "declined"},
                {200, "not json", 0L, BadGatewayException.class, "not JSON"},
                {200, "{\"content\": [{\"type\": \"text\", \"text\": \"late\"}]}", 2000L, BadGatewayException.class, "did not answer"},
        };
        for (Object[] row : cases) {
            int status = (Integer) row[0];
            String reply = (String) row[1];
            long delay = (Long) row[2];
            Class<?> expected = (Class<?>) row[3];
            String message = (String) row[4];
            test("HTTP " + status + (delay > 0 ? " after a delay (timeout)" : "") + " -> " + expected.getSimpleName()
                    + " (\"" + message + "\")", () -> {
                try (LocalLlmServer server = new LocalLlmServer(status, reply, delay)) {
                    new AnthropicLlmClient("test-key", null, Duration.ofMillis(500), server.uri()).complete("x");
                    throw new AssertionError("expected " + expected.getSimpleName());
                } catch (ApiException e) {
                    check(expected.isInstance(e), "expected " + expected.getSimpleName() + " but got " + e);
                    check(e.getMessage().contains(message), "message was: " + e.getMessage());
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        test("no API key -> 503 without any network call", () -> {
            try {
                new AnthropicLlmClient(null).complete("x");
                throw new AssertionError("expected ServiceUnavailableException");
            } catch (ServiceUnavailableException expected) {
                check(expected.getMessage().contains("LLM_API_KEY"), "message should name the variable");
            }
        });
    }

    // ================================================================== wiring

    /** A fresh API with the offline parser and breakdown strategy, on an empty store. */
    private TaskApi api() {
        return build(ParserFactory.createTaskParser(ParserFactory.Mode.RULE_BASED, null),
                ParserFactory.createBreakdownStrategy(ParserFactory.Mode.RULE_BASED, null));
    }

    /** A fresh API whose parser and breakdown strategy use the given (fake) model client. */
    private TaskApi llmApi(LlmClient client) {
        return build(ParserFactory.createTaskParser(client), ParserFactory.createBreakdownStrategy(client));
    }

    /** A fresh API in LLM mode with no API key. */
    private TaskApi noKeyApi() {
        return build(ParserFactory.createTaskParser(ParserFactory.Mode.LLM, null),
                ParserFactory.createBreakdownStrategy(ParserFactory.Mode.LLM, null));
    }

    private TaskApi build(TaskParser parser, BreakdownStrategy breakdown) {
        repository.clear();
        idempotency.clear();
        service = new TaskService(repository, parser, breakdown, FIXED_CLOCK);
        return new TaskApi(service, idempotency);
    }

    /** An API on a repository that fails in an unexpected way, to check the 500 response. */
    private TaskApi brokenRepositoryApi() {
        TaskRepository broken = new TaskRepository() {
            @Override
            public Task save(Task task) {
                throw new IllegalStateException("disk on fire (expected in this test)");
            }

            @Override
            public Optional<Task> findById(int id) {
                return Optional.empty();
            }

            @Override
            public List<Task> findAll() {
                return new ArrayList<>();
            }

            @Override
            public boolean delete(int id) {
                return false;
            }
        };
        TaskService brokenService = new TaskService(broken, new RuleBasedTaskParser(),
                ParserFactory.createBreakdownStrategy(ParserFactory.Mode.RULE_BASED, null), FIXED_CLOCK);
        return new TaskApi(brokenService, idempotency);
    }

    private static TaskRequest request(String title) {
        return new TaskRequest(title, "Description of " + title, "false", null, null);
    }

    private static List<ApiResponse> runConcurrently(int threads, Callable<ApiResponse> call) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ApiResponse>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();
            List<ApiResponse> results = new ArrayList<>();
            for (Future<ApiResponse> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (Exception e) {
            throw new IllegalStateException("concurrent run failed", e);
        } finally {
            pool.shutdownNow();
        }
    }

    // ================================================================== tiny test framework

    private void group(String name) {
        System.out.println();
        System.out.println("--- " + name + " ---");
    }

    private void test(String name, Runnable body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (AssertionError e) {
            failures.add(name + " -> " + e.getMessage());
            System.out.println("FAIL  " + name + " -> " + e.getMessage());
        } catch (RuntimeException e) {
            failures.add(name + " -> threw " + e);
            System.out.println("FAIL  " + name + " -> threw " + e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectThrows(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private void expectStoreSize(int size) {
        check(repository.size() == size, "expected " + size + " stored tasks but found " + repository.size());
    }

    private static void expectStatus(ApiResponse r, int status) {
        check(r.getStatus() == status, "expected status " + status + " but got " + r);
    }

    private static void expectError(ApiResponse r, int status, String code) {
        expectStatus(r, status);
        Map<String, Object> error = error(r);
        check(code.equals(error.get("code")), "expected code " + code + " but got " + error.get("code"));
    }

    private static void expectDetail(ApiResponse r, String field) {
        check(r.getStatus() >= 400, "expected an error but got " + r);
        for (Map<String, Object> detail : details(r)) {
            if (field.equals(detail.get("field"))) {
                return;
            }
        }
        throw new AssertionError("expected a detail for field '" + field + "' in " + r);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ApiResponse r) {
        return (Map<String, Object>) r.getBody();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(ApiResponse r) {
        return (Map<String, Object>) body(r).get("error");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> details(ApiResponse r) {
        return (List<Map<String, Object>>) error(r).get("details");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ApiResponse r) {
        check(r.isSuccess(), "expected a success response but got " + r);
        return (List<Map<String, Object>>) body(r).get("items");
    }

    private static List<Integer> ids(ApiResponse r) {
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> item : items(r)) {
            ids.add((Integer) item.get("id"));
        }
        return ids;
    }
}
