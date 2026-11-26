package com.airtribe.taskmanager.api;

import com.airtribe.taskmanager.exception.ApiException;
import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;
import com.airtribe.taskmanager.query.TaskQuery;
import com.airtribe.taskmanager.service.TaskService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The public operations, written like REST endpoints without a web framework. Every method
 * returns an {@link ApiResponse} and never throws: each one is wrapped in try/catch, an
 * {@link ApiException} becomes its error response, and anything else becomes a 500 in the same
 * shape. See API_CONTRACT.md for every operation, body and error.
 */
public class TaskApi {

    private final TaskService service;
    private final IdempotencyStore idempotencyStore;

    public TaskApi(TaskService service, IdempotencyStore idempotencyStore) {
        if (service == null || idempotencyStore == null) {
            throw new IllegalArgumentException("TaskApi dependencies must not be null");
        }
        this.service = service;
        this.idempotencyStore = idempotencyStore;
    }

    /** 201 with the task. 400 invalid input, 422 key reused for a different request. */
    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        try {
            String fingerprint = "createTask:" + (request == null ? "null" : request.fingerprint());
            return idempotencyStore.execute(idempotencyKey, fingerprint,
                    () -> ApiResponse.created(TaskMapper.toMap(service.createTask(request))));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 200 with the task. 404 unknown ID. */
    public ApiResponse getTask(int id) {
        try {
            return ApiResponse.ok(TaskMapper.toMap(service.getTask(id)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 200 with one page of tasks. 400 for a bad priority, sortBy, order, page or pageSize. */
    public ApiResponse listTasks(TaskQuery query) {
        try {
            return ApiResponse.ok(TaskMapper.page(service.listTasks(query)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 200 with every task of one priority. 400 if the level is not LOW, MEDIUM or HIGH. */
    public ApiResponse listByPriority(String level) {
        try {
            return ApiResponse.ok(TaskMapper.items(service.listByPriority(level)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 200 with the updated task. 400 invalid input, 404 unknown ID. */
    public ApiResponse updateTask(int id, TaskRequest request) {
        try {
            return ApiResponse.ok(TaskMapper.toMap(service.updateTask(id, request)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 200 with the deleted IDs (the task and its subtasks). 404 unknown ID. */
    public ApiResponse deleteTask(int id) {
        try {
            List<Task> deleted = service.deleteTask(id);
            List<Object> subtaskIds = new ArrayList<>();
            for (Task task : deleted) {
                if (task.getId() != id) {
                    subtaskIds.add(task.getId());
                }
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("deletedId", id);
            body.put("deletedSubtaskIds", subtaskIds);
            return ApiResponse.ok(body);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /**
     * 201 with the task the parser understood. 400 empty text, 422 key reused, 502 parser failed
     * or returned an invalid task (nothing saved), 503 no API key.
     */
    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        try {
            return idempotencyStore.execute(idempotencyKey, "createTaskFromText:" + text,
                    () -> ApiResponse.created(TaskMapper.toMap(service.createTaskFromText(text))));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 201 with the subtask. 400 invalid input or the parent is itself a subtask, 404 unknown parent. */
    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        try {
            return ApiResponse.created(TaskMapper.toMap(service.createSubtask(parentId, request)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /** 200 with the subtasks. 404 unknown parent. */
    public ApiResponse listSubtasks(int parentId) {
        try {
            return ApiResponse.ok(TaskMapper.subtasks(parentId, service.listSubtasks(parentId)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    /**
     * 201 with the new subtasks. 400 the task is a subtask, 404 unknown ID, 422 key reused,
     * 502 the strategy failed or returned any invalid subtask (none saved), 503 no API key.
     */
    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        try {
            return idempotencyStore.execute(idempotencyKey, "breakDownTask:" + id,
                    () -> ApiResponse.created(TaskMapper.subtasks(id, service.breakDownTask(id))));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }
}
