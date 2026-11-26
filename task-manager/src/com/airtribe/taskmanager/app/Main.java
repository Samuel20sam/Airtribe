package com.airtribe.taskmanager.app;

import com.airtribe.taskmanager.api.ApiResponse;
import com.airtribe.taskmanager.api.IdempotencyStore;
import com.airtribe.taskmanager.api.TaskApi;
import com.airtribe.taskmanager.event.AuditTrailListener;
import com.airtribe.taskmanager.event.ConsoleLogListener;
import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;
import com.airtribe.taskmanager.parser.BreakdownStrategy;
import com.airtribe.taskmanager.parser.ParserFactory;
import com.airtribe.taskmanager.parser.TaskParser;
import com.airtribe.taskmanager.query.TaskQuery;
import com.airtribe.taskmanager.repository.InMemoryTaskRepository;
import com.airtribe.taskmanager.service.TaskService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Wires the application together and calls every TaskApi operation, printing each response.
 *
 * <pre>
 * java -cp out com.airtribe.taskmanager.app.Main            # LLM if LLM_API_KEY is set, else rule-based
 * java -cp out com.airtribe.taskmanager.app.Main --offline  # always rule-based, no network
 * </pre>
 */
public class Main {

    private static final InMemoryTaskRepository REPOSITORY = InMemoryTaskRepository.getInstance();
    private static final IdempotencyStore IDEMPOTENCY = IdempotencyStore.getInstance();

    public static void main(String[] args) {
        System.out.println("Task Manager started");

        String apiKey = ParserFactory.apiKeyFromEnvironment();
        ParserFactory.Mode mode = Arrays.asList(args).contains("--offline")
                ? ParserFactory.Mode.RULE_BASED
                : ParserFactory.Mode.AUTO;
        ParserFactory.Mode resolved = ParserFactory.resolve(mode, apiKey);
        System.out.println("AI features: " + (resolved == ParserFactory.Mode.LLM
                ? "language model (LLM_API_KEY is set)"
                : "rule-based parser, offline (set LLM_API_KEY to use the language model)"));

        TaskParser parser = ParserFactory.createTaskParser(mode, apiKey);
        BreakdownStrategy breakdown = ParserFactory.createBreakdownStrategy(mode, apiKey);
        TaskService service = new TaskService(REPOSITORY, parser, breakdown);
        ConsoleLogListener consoleLog = new ConsoleLogListener();
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(consoleLog);
        service.addListener(audit);
        TaskApi api = new TaskApi(service, IDEMPOTENCY);

        crud(api);
        validation(api);
        aiTaskCreation(api);
        subtasks(api, service);
        idempotency(api, service);
        filtering(api);
        events(api, service, consoleLog, audit);

        System.out.println();
        System.out.println("Done.");
    }

    // ------------------------------------------------------------------ steps

    private static void crud(TaskApi api) {
        section("Step 2: CRUD");
        show("createTask(Buy groceries, HIGH, due 2026-12-05)",
                api.createTask(new TaskRequest("Buy groceries", "Milk, eggs and bread", "false", "2026-12-05", "HIGH"), null));
        show("createTask(Write report) with no optional fields",
                api.createTask(new TaskRequest("Write report", "Quarterly sales report", null, null, null), null));
        show("getTask(1)", api.getTask(1));
        show("updateTask(1, completed=true)",
                api.updateTask(1, new TaskRequest("Buy groceries", "Milk, eggs and bread", "true", "2026-12-05", "HIGH")));
        show("listTasks(all)", api.listTasks(TaskQuery.all()));
        show("deleteTask(2)", api.deleteTask(2));
        show("getTask(2) after delete", api.getTask(2));
    }

    private static void validation(TaskApi api) {
        section("Step 3: Validation and one error shape");
        show("createTask(title=\"  \", completed=\"yes\", dueDate=2026-02-30, priority=URGENT)",
                api.createTask(new TaskRequest("  ", "Something", "yes", "2026-02-30", "URGENT"), null));
        show("createTask(dueDate=2028-02-29, a leap day) is accepted",
                api.createTask(new TaskRequest("Leap day party", "Celebrate the 29th", "false", "2028-02-29", "LOW"), null));
        show("getTask(999)", api.getTask(999));
        show("updateTask(999, ...)", api.updateTask(999, new TaskRequest("X", "Y", "false", null, null)));
        show("deleteTask(999)", api.deleteTask(999));
    }

    private static void aiTaskCreation(TaskApi api) {
        section("Step 4: AI task creation");
        show("createTaskFromText(\"Remind me to pay rent on the 1st of every month\")",
                api.createTaskFromText("Remind me to pay rent on the 1st of every month", null));
        show("createTaskFromText(\"Call the dentist tomorrow\")",
                api.createTaskFromText("Call the dentist tomorrow", null));
        show("createTaskFromText(\"\")", api.createTaskFromText("", null));

        System.out.println();
        System.out.println("API key rule: a service built in LLM mode without LLM_API_KEY answers 503, it does not crash.");
        TaskService noKeyService = new TaskService(REPOSITORY,
                ParserFactory.createTaskParser(ParserFactory.Mode.LLM, null),
                ParserFactory.createBreakdownStrategy(ParserFactory.Mode.LLM, null));
        TaskApi noKeyApi = new TaskApi(noKeyService, IDEMPOTENCY);
        show("createTaskFromText(...) with no key", noKeyApi.createTaskFromText("Water the plants every day", null));
        show("breakDownTask(1) with no key", noKeyApi.breakDownTask(1, null));
    }

    private static void subtasks(TaskApi api, TaskService service) {
        section("Step 5: Subtasks and breakdown");
        ApiResponse parent = api.createTask(new TaskRequest("Plan team offsite",
                "Book a venue, send the invites and order catering", "false", "2027-01-15", "HIGH"), null);
        show("createTask(Plan team offsite)", parent);
        int parentId = idOf(parent);

        show("breakDownTask(" + parentId + ")", api.breakDownTask(parentId, null));
        ApiResponse manual = api.createSubtask(parentId,
                new TaskRequest("Collect dietary needs", "Ask everyone before ordering food", "false", null, null));
        show("createSubtask(" + parentId + ", Collect dietary needs)", manual);
        show("listSubtasks(" + parentId + ")", api.listSubtasks(parentId));

        int subtaskId = idOf(manual);
        show("createSubtask(" + subtaskId + ", ...) on a subtask",
                api.createSubtask(subtaskId, new TaskRequest("Too deep", "Not allowed", "false", null, null)));
        show("breakDownTask(" + subtaskId + ") on a subtask", api.breakDownTask(subtaskId, null));
        show("listSubtasks(999)", api.listSubtasks(999));

        printRepository("Repository before deleting the parent", service);
        show("deleteTask(" + parentId + ")", api.deleteTask(parentId));
        printRepository("Repository after deleting the parent (its subtasks are gone too)", service);
    }

    private static void idempotency(TaskApi api, TaskService service) {
        section("Step 6: Idempotent creation");
        clearStore();
        TaskRequest request = new TaskRequest("Renew passport", "Book an appointment", "false", "2027-03-01", "MEDIUM");
        show("createTask(Renew passport, key=\"retry-123\")", api.createTask(request, "retry-123"));
        show("createTask(same request, key=\"retry-123\") again", api.createTask(request, "retry-123"));
        printRepository("Repository: only one task exists", service);

        show("createTask(different request, key=\"retry-123\")",
                api.createTask(new TaskRequest("Something else", "Different body", "false", null, null), "retry-123"));
        show("createTask(invalid, key=\"fix-me\")",
                api.createTask(new TaskRequest("", "Missing title", "false", null, null), "fix-me"));
        show("createTask(fixed, key=\"fix-me\"): errors are not stored, so the retry works",
                api.createTask(new TaskRequest("Fixed title", "Missing title", "false", null, null), "fix-me"));
        show("createTaskFromText(\"Buy flowers tomorrow\", key=\"text-1\")",
                api.createTaskFromText("Buy flowers tomorrow", "text-1"));
        show("createTaskFromText(\"Buy flowers tomorrow\", key=\"text-1\") again (parser not called)",
                api.createTaskFromText("Buy flowers tomorrow", "text-1"));
        printRepository("Repository after the retries", service);
    }

    private static void filtering(TaskApi api) {
        section("Step 7: Filtering, search, sorting and pagination");
        clearStore();
        api.createTask(new TaskRequest("Write report draft", "Sales report for Q4", "false", "2026-12-10", "HIGH"), null);
        api.createTask(new TaskRequest("Review report", "Check the Q4 numbers", "false", "2026-12-03", "HIGH"), null);
        api.createTask(new TaskRequest("Send report", "Email the final report", "true", "2026-12-12", "HIGH"), null);
        api.createTask(new TaskRequest("Book flights", "Holiday travel", "false", "2026-12-01", "LOW"), null);
        api.createTask(new TaskRequest("Report expenses", "Upload receipts", "false", null, "HIGH"), null);
        api.createTask(new TaskRequest("Clean desk", "Before the holidays", "false", "2026-12-20", "MEDIUM"), null);
        System.out.println("(6 tasks created)");

        show("listTasks(completed=false, priority=HIGH, search=\"report\", sortBy=dueDate, order=desc, page=1, pageSize=2)",
                api.listTasks(TaskQuery.builder().completed(false).priority("HIGH").search("report")
                        .sortBy("dueDate").order("desc").page(1).pageSize(2).build()));
        show("same query, page=2",
                api.listTasks(TaskQuery.builder().completed(false).priority("HIGH").search("report")
                        .sortBy("dueDate").order("desc").page(2).pageSize(2).build()));
        show("listTasks(page=5) beyond the last page", api.listTasks(TaskQuery.builder().page(5).build()));
        show("listTasks(sortBy=title, order=up, page=0)",
                api.listTasks(TaskQuery.builder().sortBy("title").order("up").page(0).build()));
        show("listByPriority(\"LOW\")", api.listByPriority("LOW"));
        show("listByPriority(\"urgent\")", api.listByPriority("urgent"));
    }

    private static void events(TaskApi api, TaskService service, ConsoleLogListener consoleLog,
                               AuditTrailListener audit) {
        section("Step 8: Events (Observer)");
        System.out.println("Audit trail so far: " + audit.getCounts());
        System.out.println("Removing the console listener; the next create prints no [event] line.");
        service.removeListener(consoleLog);
        int before = audit.getTotal();
        show("createTask(Quiet task)", api.createTask(new TaskRequest("Quiet task", "No console event", "false", null, null), null));
        System.out.println("The audit listener is still registered: total events " + before + " -> " + audit.getTotal());
        System.out.println("Audit trail: " + audit.getCounts());
        service.addListener(consoleLog);
    }

    // ------------------------------------------------------------------ helpers

    private static void section(String title) {
        System.out.println();
        System.out.println("==================== " + title + " ====================");
    }

    private static void show(String call, ApiResponse response) {
        System.out.println("> " + call);
        System.out.println("  " + response);
    }

    private static void printRepository(String heading, TaskService service) {
        System.out.println(heading + ":");
        List<Task> tasks = service.allTasks();
        if (tasks.isEmpty()) {
            System.out.println("  (empty)");
        }
        for (Task task : tasks) {
            System.out.println("  " + task);
        }
    }

    private static void clearStore() {
        REPOSITORY.clear();
        IDEMPOTENCY.clear();
        System.out.println("(store cleared for this step)");
    }

    @SuppressWarnings("unchecked")
    private static int idOf(ApiResponse response) {
        if (!response.isSuccess()) {
            throw new IllegalStateException("Expected a created task but got " + response);
        }
        return ((Number) ((Map<String, Object>) response.getBody()).get("id")).intValue();
    }
}
