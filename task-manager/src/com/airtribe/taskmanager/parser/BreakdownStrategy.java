package com.airtribe.taskmanager.parser;

import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;

import java.util.List;

/**
 * Strategy pattern: splits one task into a list of subtask requests.
 *
 * <p>Like {@link TaskParser}, implementations return raw requests and the service validates every
 * one of them before saving any. Throw {@code BadGatewayException} (502) when the source fails or
 * its output does not follow the expected format.
 */
public interface BreakdownStrategy {

    List<TaskRequest> breakDown(Task task);
}
