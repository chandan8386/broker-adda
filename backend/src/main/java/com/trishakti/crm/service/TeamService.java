package com.trishakti.crm.service;

import com.trishakti.crm.domain.Team;
import com.trishakti.crm.dto.UserDtos.TeamRequest;
import com.trishakti.crm.dto.UserDtos.TeamResponse;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.TeamRepository;
import com.trishakti.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> list() {
        return teamRepository.findAll().stream()
                .map(t -> CrmMappers.team(t, 0))
                .toList();
    }

    @Transactional
    public TeamResponse create(TeamRequest req) {
        Team team = new Team();
        apply(team, req);
        teamRepository.save(team);
        return CrmMappers.team(team, 0);
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest req) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team", id));
        apply(team, req);
        return CrmMappers.team(team, 0);
    }

    @Transactional
    public void delete(Long id) {
        if (!teamRepository.existsById(id)) throw new ResourceNotFoundException("Team", id);
        teamRepository.deleteById(id);
    }

    private void apply(Team team, TeamRequest req) {
        team.setName(req.name());
        team.setDescription(req.description());
        team.setActive(req.active() == null || req.active());
        team.setManager(req.managerId() != null
                ? userRepository.findById(req.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.managerId()))
                : null);
    }
}
