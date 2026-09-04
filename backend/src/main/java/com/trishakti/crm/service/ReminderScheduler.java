package com.trishakti.crm.service;

import com.trishakti.crm.domain.enums.NotificationType;
import com.trishakti.crm.repository.FollowUpRepository;
import com.trishakti.crm.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scans pending follow-ups and tasks whose reminder time has arrived and pushes
 * in-app notifications to their owners. Runs on the cron defined by {@code app.reminders.scan-cron}.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final FollowUpRepository followUpRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final long lookaheadMinutes;

    public ReminderScheduler(FollowUpRepository followUpRepository, TaskRepository taskRepository,
                             NotificationService notificationService,
                             @Value("${app.reminders.lookahead-minutes:60}") long lookaheadMinutes) {
        this.followUpRepository = followUpRepository;
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.lookaheadMinutes = lookaheadMinutes;
    }

    @Scheduled(cron = "${app.reminders.scan-cron:0 */5 * * * *}")
    @Transactional
    public void run() {
        Instant cutoff = Instant.now().plus(lookaheadMinutes, ChronoUnit.MINUTES);
        int reminders = 0;

        for (var fu : followUpRepository.findDueForReminder(cutoff)) {
            boolean overdue = fu.getDueAt().isBefore(Instant.now());
            notificationService.push(fu.getOwner(),
                    overdue ? NotificationType.FOLLOW_UP_OVERDUE : NotificationType.FOLLOW_UP_DUE,
                    (overdue ? "Overdue follow-up: " : "Follow-up due: ") + fu.getLead().getReference(),
                    fu.getLead().getCustomerName() + " (" + fu.getLead().getMobile() + ") — due " + fu.getDueAt(),
                    "Lead", fu.getLead().getId());
            fu.setNotified(true);
            reminders++;
        }

        for (var task : taskRepository.findDueForReminder(cutoff)) {
            notificationService.push(task.getAssignee(), NotificationType.TASK_DUE,
                    "Task reminder: " + task.getTitle(),
                    task.getDescription(), "Task", task.getId());
            task.setNotified(true);
            reminders++;
        }

        if (reminders > 0) {
            log.info("ReminderScheduler dispatched {} reminder notification(s)", reminders);
        }
    }
}
