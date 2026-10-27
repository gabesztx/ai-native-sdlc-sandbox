package com.hugoboard.user;

record ImportedTeamMember(
		String name,
		String jiraAccountId,
		String email,
		String avatarUrl,
		boolean active,
		String importedAt) {
}
