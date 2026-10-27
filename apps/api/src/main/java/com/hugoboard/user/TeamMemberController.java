package com.hugoboard.user;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TeamMemberController {

	private final TeamMemberService service;

	public TeamMemberController(TeamMemberService service) {
		this.service = service;
	}

	@GetMapping("/api/auth/team-members")
	@Operation(operationId = "listLoginTeamMembers")
	public List<TeamMember> listForLogin() {
		return service.findAll();
	}

	@GetMapping("/api/admin/team-members")
	@Operation(operationId = "listTeamMembers")
	public List<TeamMember> listForAdmin() {
		return service.findAll();
	}

}
