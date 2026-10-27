package com.hugoboard.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamMemberController {

	private final TeamMemberService service;

	public TeamMemberController(TeamMemberService service) {
		this.service = service;
	}

	@GetMapping("/api/auth/team-members")
	public List<TeamMember> listForLogin() {
		return service.findAll();
	}

}
