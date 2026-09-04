package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.dto.NotificationDtos.NotificationResponse;
import com.trishakti.crm.dto.NotificationDtos.UnreadCountResponse;
import com.trishakti.crm.security.SecurityUtils;
import com.trishakti.crm.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(SecurityUtils.currentUserId(), unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return notificationService.unreadCount(SecurityUtils.currentUserId());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead(SecurityUtils.currentUserId()));
    }
}
