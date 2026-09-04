package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.Task;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.NotificationType;
import com.trishakti.crm.domain.enums.TaskStatus;
import com.trishakti.crm.dto.TaskDtos.TaskRequest;
import com.trishakti.crm.dto.TaskDtos.TaskResponse;
import com.trishakti.crm.dto.TaskDtos.UpdateTaskRequest;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.TaskRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       LeadRepository leadRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public TaskResponse create(TaskRequest req) {
        Task task = new Task();
        task.setTitle(req.title());
        task.setDescription(req.description());
        if (req.type() != null) task.setType(req.type());
        if (req.priority() != null) task.setPriority(req.priority());
        task.setDueAt(req.dueAt());
        task.setReminderAt(req.reminderAt() != null ? req.reminderAt() : req.dueAt());
        User assignee = req.assigneeId() != null ? user(req.assigneeId()) : currentUser();
        task.setAssignee(assignee);
        if (req.leadId() != null) {
            task.setLead(leadRepository.findById(req.leadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lead", req.leadId())));
        }
        taskRepository.save(task);
        notificationService.push(assignee, NotificationType.TASK_ASSIGNED, "New task: " + task.getTitle(),
                task.getDescription(), "Task", task.getId());
        return CrmMappers.task(task);
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest req) {
        Task task = find(id);
        if (req.title() != null) task.setTitle(req.title());
        if (req.description() != null) task.setDescription(req.description());
        if (req.priority() != null) task.setPriority(req.priority());
        if (req.dueAt() != null) task.setDueAt(req.dueAt());
        if (req.reminderAt() != null) task.setReminderAt(req.reminderAt());
        if (req.status() != null) {
            task.setStatus(req.status());
            if (req.status() == TaskStatus.DONE) task.setCompletedAt(Instant.now());
        }
        return CrmMappers.task(task);
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) throw new ResourceNotFoundException("Task", id);
        taskRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> myTasks(TaskStatus status, Pageable pageable) {
        Long uid = SecurityUtils.currentUserId();
        var page = status != null
                ? taskRepository.findByAssigneeIdAndStatus(uid, status, pageable)
                : taskRepository.findByAssigneeId(uid, pageable);
        return PageResponse.of(page, CrmMappers::task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return CrmMappers.task(find(id));
    }

    private Task find(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
