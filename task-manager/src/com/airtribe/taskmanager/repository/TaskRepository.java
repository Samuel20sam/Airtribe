package com.airtribe.taskmanager.repository;

import com.airtribe.taskmanager.model.Task;

import java.util.List;
import java.util.Optional;

/**
 * Where tasks are stored. The service depends on this interface only, so the in-memory
 * version can later be swapped for a database without touching the service (Dependency Inversion).
 */
public interface TaskRepository {

    /**
     * Stores a task. A task with ID 0 is new: it gets the next free ID (starting at 1) and the
     * stored copy is returned. A task with an ID replaces the stored task with that ID.
     */
    Task save(Task task);

    Optional<Task> findById(int id);

    /** Every stored task, in ID order. The returned list is a copy. */
    List<Task> findAll();

    /** Removes the task with this ID. Returns false when there was no such task. */
    boolean delete(int id);
}
