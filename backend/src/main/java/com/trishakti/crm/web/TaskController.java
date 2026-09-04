package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.enums.TaskStatus;
import com.trishakti.crm.dto.TaskDtos.TaskRequest;
import com.trishakti.crm.dto.TaskDtos.TaskResponse;
import com.trishakti.crm.dto.TaskDtos.UpdateTaskRequest;
import com.trishakti.crm.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Task & Reminder Management")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/mine")
    @Operation(summary = "Tasks assigned to the current user (optionally filtered by status)")
    public PageResponse<TaskResponse> myTasks(@RequestParam(required = false) TaskStatus status,
                                              @PageableDefault(size = 20, sort = "dueAt") Pageable pageable) {
        return taskService.myTasks(status, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
