package com.hugoboard.user;

import com.hugoboard.common.Timestamps;
import com.hugoboard.jira.JiraClient;
import com.hugoboard.jira.JiraImportException;
import com.hugoboard.jira.JiraProperties;
import com.hugoboard.jira.JiraUser;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JiraTeamImportService {

	private static final Logger log = LoggerFactory.getLogger(JiraTeamImportService.class);

	private final TeamMemberRepository repository;

	private final JiraClient jiraClient;

	private final JiraProperties properties;

	private final Clock clock;

	public JiraTeamImportService(TeamMemberRepository repository, JiraClient jiraClient,
			JiraProperties properties, Clock clock) {
		this.repository = repository;
		this.jiraClient = jiraClient;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Everything (the empty-table check and, when it runs, the Jira fetch and the save)
	 * happens inside one transaction, so a mid-import failure leaves team_member exactly
	 * as it was, and a successful import either saves every member or none.
	 */
	@Transactional
	public void importIfNeeded() {
		if (repository.count() != 0) {
			return;
		}
		if (!properties.isConfigured()) {
			if (properties.isPartiallyConfigured()) {
				log.warn("Jira import skipped: HUGO_JIRA_* configuration is incomplete");
			} else {
				log.info("Jira import skipped: HUGO_JIRA_* environment variables are not configured");
			}
			return;
		}
		List<ImportedTeamMember> members;
		try {
			members = fetchMembers();
		} catch (JiraImportException e) {
			log.error("Jira import failed, starting with an empty team: {}", e.getMessage());
			return;
		}
		if (members.isEmpty()) {
			log.warn("Jira import found no assignees in the active sprint; team_member stays empty");
			return;
		}
		members.forEach(repository::insertImported);
	}

	private List<ImportedTeamMember> fetchMembers() {
		long sprintId = jiraClient.findActiveSprintId();
		List<JiraUser> assignees = jiraClient.fetchAssignees(sprintId);
		String importedAt = Timestamps.now(clock);
		Map<String, ImportedTeamMember> byAccountId = new LinkedHashMap<>();
		for (JiraUser assignee : assignees) {
			byAccountId.putIfAbsent(assignee.accountId(), new ImportedTeamMember(
					assignee.displayName(),
					assignee.accountId(),
					assignee.emailAddress() == null ? "" : assignee.emailAddress(),
					assignee.avatarUrl(),
					assignee.isActive(),
					importedAt));
		}
		return List.copyOf(byAccountId.values());
	}

}
