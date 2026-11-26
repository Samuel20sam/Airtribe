package com.airtribe.taskmanager.repository;

import com.airtribe.taskmanager.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Keeps tasks in a map in memory. Written as a Singleton so the whole program shares one store:
 * the constructor is private and {@link #getInstance()} always returns the same object.
 *
 * <p>The instance is created eagerly when the class is loaded, which the JVM guarantees happens
 * once, so {@code getInstance()} is thread-safe without locking. The methods that read or change
 * the map are {@code synchronized}, so two threads can never hand out the same ID.
 */
public final class InMemoryTaskRepository implements TaskRepository {

    private static final InMemoryTaskRepository INSTANCE = new InMemoryTaskRepository();

    /** TreeMap keeps the tasks sorted by ID. */
    private final Map<Integer, Task> tasks = new TreeMap<>();
    private int nextId = 1;

    private InMemoryTaskRepository() {
    }

    public static InMemoryTaskRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized Task save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        Task stored = task.getId() == 0 ? task.toBuilder().id(nextId).build() : task;
        if (stored.getId() >= nextId) {
            nextId = stored.getId() + 1;
        }
        tasks.put(stored.getId(), stored);
        return stored;
    }

    @Override
    public synchronized Optional<Task> findById(int id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public synchronized List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public synchronized boolean delete(int id) {
        return tasks.remove(id) != null;
    }

    public synchronized int size() {
        return tasks.size();
    }

    /** Removes every task and starts IDs from 1 again. Used by Main sections and the TestRunner. */
    public synchronized void clear() {
        tasks.clear();
        nextId = 1;
    }
}
