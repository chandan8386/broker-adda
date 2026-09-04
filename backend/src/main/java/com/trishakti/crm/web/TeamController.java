package com.trishakti.crm.web;

import com.trishakti.crm.dto.UserDtos.TeamRequest;
import com.trishakti.crm.dto.UserDtos.TeamResponse;
import com.trishakti.crm.service.TeamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@Tag(name = "User / Team Management")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamResponse> list() {
        return teamService.list();
    }

    @PostMapping
    public TeamResponse create(@Valid @RequestBody TeamRequest request) {
        return teamService.create(request);
    }

    @PutMapping("/{id}")
    public TeamResponse update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return teamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
