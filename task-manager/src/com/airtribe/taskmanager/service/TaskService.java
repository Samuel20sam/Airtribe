package com.airtribe.taskmanager.service;

import com.airtribe.taskmanager.event.TaskEventListener;
import com.airtribe.taskmanager.event.TaskEventType;
import com.airtribe.taskmanager.exception.ApiException;
import com.airtribe.taskmanager.exception.BadGatewayException;
import com.airtribe.taskmanager.exception.FieldIssue;
import com.airtribe.taskmanager.exception.NotFoundException;
import com.airtribe.taskmanager.exception.ValidationException;
import com.airtribe.taskmanager.model.Priority;
import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;
import com.airtribe.taskmanager.parser.BreakdownStrategy;
import com.airtribe.taskmanager.parser.ParsedTask;
import com.airtribe.taskmanager.parser.TaskParser;
import com.airtribe.taskmanager.query.CompletedFilter;
import com.airtribe.taskmanager.query.CreatedAtSort;
import com.airtribe.taskmanager.query.DueDateSort;
import com.airtribe.taskmanager.query.Page;
import com.airtribe.taskmanager.query.ParentIdFilter;
import com.airtribe.taskmanager.query.PriorityFilter;
import com.airtribe.taskmanager.query.SearchFilter;
import com.airtribe.taskmanager.query.SortStrategy;
import com.airtribe.taskmanager.query.TaskFilter;
import com.airtribe.taskmanager.query.TaskQuery;
import com.airtribe.taskmanager.repository.TaskRepository;
import com.airtribe.taskmanager.util.DateHelper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The business logic. Every dependency arrives through the constructor as an interface
 * (repository, parser, breakdown strategy), so each one can be swapped without changing this
 * class (Dependency Inversion). The service throws {@link ApiException}s; it never builds
 * responses, which is the job of {@code TaskApi}.
 *
 * <p>It is also the subject of the Observer pattern: listeners added with
 * {@link #addListener(TaskEventListener)} are told after a task is created, updated, completed
 * or deleted.
 */
public class TaskService {

    public static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository repository;
    private final TaskParser taskParser;
    private final BreakdownStrategy breakdownStrategy;
    private final Clock clock;
    private final TaskValidator validator = new TaskValidator();
    /** CopyOnWriteArrayList: a listener may remove itself while events are being sent. */
    private final List<TaskEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, SortStrategy> sortStrategies = new LinkedHashMap<>();

    public TaskService(TaskRepository repository, TaskParser taskParser, BreakdownStrategy breakdownStrategy) {
        this(repository, taskParser, breakdownStrategy, Clock.systemDefaultZone());
    }

    /** The clock decides what "today" is for the parser; tests pass a fixed clock. */
    public TaskService(TaskRepository repository, TaskParser taskParser, BreakdownStrategy breakdownStrategy,
                       Clock clock) {
        if (repository == null || taskParser == null || breakdownStrategy == null || clock == null) {
            throw new IllegalArgumentException("TaskService dependencies must not be null");
        }
        this.repository = repository;
        this.taskParser = taskParser;
        this.breakdownStrategy = breakdownStrategy;
        this.clock = clock;
        // Built-in sorts go straight into the map: calling an overridable method from a constructor is unsafe.
        for (SortStrategy builtIn : List.of(new CreatedAtSort(), new DueDateSort())) {
            sortStrategies.put(builtIn.name(), builtIn);
        }
    }

    // ------------------------------------------------------------------ observers

    public void addListener(TaskEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    public void removeListener(TaskEventListener listener) {
        listeners.remove(listener);
    }

    /** A listener that throws must not undo or break the operation, so its error is only logged. */
    private void notifyListeners(TaskEventType type, Task task) {
        for (TaskEventListener listener : listeners) {
            try {
                listener.onEvent(type, task);
            } catch (RuntimeException e) {
                System.err.println("Listener " + listener.getClass().getSimpleName() + " failed on "
                        + type + ": " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ sorting (Open/Closed)

    /** Adds a sort order that {@code TaskQuery.sortBy} can select by name. */
    public void registerSortStrategy(SortStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("strategy must not be null");
        }
        sortStrategies.put(strategy.name(), strategy);
    }

    // ------------------------------------------------------------------ CRUD

    public Task createTask(TaskRequest request) {
        Task task = repository.save(validator.validate(request).build());
        notifyListeners(TaskEventType.CREATED, task);
        return task;
    }

    public Task getTask(int id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.task(id));
    }

    /**
     * Replaces the task's fields with the request's (same rules as create). The ID, parent and
     * creation time stay the same. Sends UPDATED, then COMPLETED if the task was just completed.
     */
    public Task updateTask(int id, TaskRequest request) {
        Task existing = getTask(id);
        Task updated = repository.save(validator.validate(request)
                .id(existing.getId())
                .parentId(existing.getParentId())
                .createdAt(existing.getCreatedAt())
                .build());
        notifyListeners(TaskEventType.UPDATED, updated);
        if (!existing.isCompleted() && updated.isCompleted()) {
            notifyListeners(TaskEventType.COMPLETED, updated);
        }
        return updated;
    }

    /** Deletes the task and its subtasks. Returns everything deleted, subtasks first. */
    public List<Task> deleteTask(int id) {
        Task task = getTask(id);
        List<Task> deleted = new ArrayList<>(findSubtasks(id));
        deleted.add(task);
        for (Task t : deleted) {
            repository.delete(t.getId());
            notifyListeners(TaskEventType.DELETED, t);
        }
        return deleted;
    }

    // ------------------------------------------------------------------ listing

    /** Filters, searches, sorts and pages in one call. */
    public Page<Task> listTasks(TaskQuery query) {
        TaskQuery q = query == null ? TaskQuery.all() : query;
        List<FieldIssue> issues = new ArrayList<>();
        List<TaskFilter> filters = new ArrayList<>();

        if (q.getCompleted() != null) {
            filters.add(new CompletedFilter(q.getCompleted()));
        }
        if (q.getPriority() != null) {
            Priority priority = validator.parsePriority(q.getPriority(), "priority", issues);
            if (priority != null) {
                filters.add(new PriorityFilter(priority));
            }
        }
        if (q.getSearch() != null && !q.getSearch().isBlank()) {
            filters.add(new SearchFilter(q.getSearch()));
        }
        filters.addAll(q.getExtraFilters());

        String sortBy = q.getSortBy() == null ? CreatedAtSort.NAME : q.getSortBy();
        SortStrategy sort = sortStrategies.get(sortBy);
        if (sort == null) {
            issues.add(new FieldIssue("sortBy", "must be one of " + String.join(", ", sortStrategies.keySet())));
        }
        String order = q.getOrder() == null ? "asc" : q.getOrder();
        if (!order.equals("asc") && !order.equals("desc")) {
            issues.add(new FieldIssue("order", "must be \"asc\" or \"desc\""));
        }
        if (q.getPage() < 1) {
            issues.add(new FieldIssue("page", "must be at least 1"));
        }
        if (q.getPageSize() < 1 || q.getPageSize() > MAX_PAGE_SIZE) {
            issues.add(new FieldIssue("pageSize", "must be between 1 and " + MAX_PAGE_SIZE));
        }
        if (!issues.isEmpty()) {
            throw new ValidationException(issues);
        }

        List<Task> matching = filter(repository.findAll(), filters);
        matching.sort(sort.comparator(order.equals("asc")));
        return Page.of(matching, q.getPage(), q.getPageSize());
    }

    /** Every task with one priority, oldest first. 400 if the level is not LOW, MEDIUM or HIGH. */
    public List<Task> listByPriority(String level) {
        List<FieldIssue> issues = new ArrayList<>();
        Priority priority = validator.parsePriority(level, "level", issues);
        if (priority == null) {
            throw new ValidationException(issues);
        }
        List<Task> tasks = filter(repository.findAll(), List.of(new PriorityFilter(priority)));
        tasks.sort(new CreatedAtSort().comparator(true));
        return tasks;
    }

    private static List<Task> filter(List<Task> tasks, List<TaskFilter> filters) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            boolean keep = true;
            for (TaskFilter filter : filters) {
                if (!filter.matches(task)) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                result.add(task);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ subtasks

    public Task createSubtask(int parentId, TaskRequest request) {
        Task parent = requireTopLevel(parentId, "parentId", "cannot have subtasks");
        Task subtask = repository.save(validator.validate(request).parentId(parent.getId()).build());
        notifyListeners(TaskEventType.CREATED, subtask);
        return subtask;
    }

    /** The subtasks of a task, in ID order. 404 if the parent does not exist. */
    public List<Task> listSubtasks(int parentId) {
        getTask(parentId);
        return findSubtasks(parentId);
    }

    private List<Task> findSubtasks(int parentId) {
        return filter(repository.findAll(), List.of(new ParentIdFilter(parentId)));
    }

    /** 404 if the task does not exist, 400 if it is itself a subtask (only one level is allowed). */
    private Task requireTopLevel(int id, String field, String what) {
        Task task = getTask(id);
        if (task.isSubtask()) {
            throw new ValidationException(field, "task " + id + " is a subtask, so it " + what
                    + " (only one level of subtasks is allowed)");
        }
        return task;
    }

    // ------------------------------------------------------------------ AI features

    /**
     * Turns a sentence into a task with the configured parser. The parser's answer is checked with
     * the same rules as a normal request ("never trust the model"); if it fails, the answer is 502
     * and nothing is saved.
     */
    public Task createTaskFromText(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("text", "must not be empty");
        }
        ParsedTask parsed;
        try {
            parsed = taskParser.parse(text.trim(), DateHelper.today(clock));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BadGatewayException("The task parser failed: " + e.getMessage());
        }
        if (parsed == null) {
            throw new BadGatewayException("The task parser returned nothing");
        }

        Task.Builder builder;
        try {
            builder = validator.validate(parsed.toTaskRequest());
        } catch (ValidationException e) {
            throw new BadGatewayException("The task parser returned an invalid task", e.getDetails());
        }
        Task task = repository.save(builder.build());
        notifyListeners(TaskEventType.CREATED, task);
        return task;
    }

    /**
     * Splits a task into subtasks with the configured breakdown strategy. Every subtask is
     * validated first; if any one is invalid, or the strategy fails, none are saved (502).
     */
    public List<Task> breakDownTask(int id) {
        Task parent = requireTopLevel(id, "id", "cannot be broken down");

        List<TaskRequest> requests;
        try {
            requests = breakdownStrategy.breakDown(parent);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BadGatewayException("The breakdown strategy failed: " + e.getMessage());
        }
        if (requests == null || requests.isEmpty()) {
            throw new BadGatewayException("The breakdown strategy returned no subtasks");
        }

        // Validate everything before saving anything.
        List<FieldIssue> issues = new ArrayList<>();
        List<Task.Builder> builders = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            try {
                builders.add(validator.validate(requests.get(i)).parentId(parent.getId()));
            } catch (ValidationException e) {
                for (FieldIssue issue : e.getDetails()) {
                    issues.add(new FieldIssue("subtasks[" + i + "]." + issue.getField(), issue.getIssue()));
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new BadGatewayException("The breakdown strategy returned invalid subtasks", issues);
        }

        List<Task> saved = new ArrayList<>();
        for (Task.Builder builder : builders) {
            Task subtask = repository.save(builder.build());
            saved.add(subtask);
            notifyListeners(TaskEventType.CREATED, subtask);
        }
        return saved;
    }

    /** Sorted by ID; used by Main and the tests to print the store. */
    public List<Task> allTasks() {
        List<Task> all = repository.findAll();
        all.sort(Comparator.comparingInt(Task::getId));
        return all;
    }
}
