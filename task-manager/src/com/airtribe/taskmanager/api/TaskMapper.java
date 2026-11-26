package com.airtribe.taskmanager.api;

import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.query.Page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns tasks into JSON-style maps for response bodies. Keeps JSON concerns out of the model. */
final class TaskMapper {

    private TaskMapper() {
    }

    static Map<String, Object> toMap(Task task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("title", task.getTitle());
        map.put("description", task.getDescription());
        map.put("completed", task.isCompleted());
        map.put("dueDate", task.getDueDate());
        // Enums are stored as their names so the body holds only JSON types (String, number, boolean, null).
        map.put("priority", task.getPriority().name());
        map.put("parentId", task.getParentId());
        map.put("recurrence", task.getRecurrence() == null ? null : task.getRecurrence().name());
        map.put("createdAt", task.getCreatedAt());
        return map;
    }

    static List<Object> toList(List<Task> tasks) {
        List<Object> list = new ArrayList<>();
        for (Task task : tasks) {
            list.add(toMap(task));
        }
        return list;
    }

    static Map<String, Object> page(Page<Task> page) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", toList(page.getItems()));
        body.put("page", page.getPage());
        body.put("pageSize", page.getPageSize());
        body.put("totalItems", page.getTotalItems());
        body.put("totalPages", page.getTotalPages());
        return body;
    }

    static Map<String, Object> items(List<Task> tasks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", toList(tasks));
        body.put("totalItems", tasks.size());
        return body;
    }

    static Map<String, Object> subtasks(int parentId, List<Task> tasks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parentId", parentId);
        body.putAll(items(tasks));
        return body;
    }
}
