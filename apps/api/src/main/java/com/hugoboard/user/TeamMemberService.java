package com.hugoboard.user;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TeamMemberService {

	private final TeamMemberRepository repository;

	public TeamMemberService(TeamMemberRepository repository) {
		this.repository = repository;
	}

	public List<TeamMember> findAll() {
		return repository.findAll();
	}

	public Optional<TeamMember> findById(long id) {
		return repository.findById(id);
	}

}
