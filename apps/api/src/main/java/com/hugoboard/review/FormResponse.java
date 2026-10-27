package com.hugoboard.review;

import com.hugoboard.user.TeamMember;
import java.util.List;

public record FormResponse(TeamMember member, String submittedAt, List<Answer> answers) {
}
