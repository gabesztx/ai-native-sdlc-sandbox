package com.hugoboard.review;

import com.hugoboard.auth.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping
	public List<ReviewSummary> list(CurrentUser user) {
		return reviewService.listFor(user.memberId());
	}

	@GetMapping("/{reviewId}")
	public ReviewDetail get(@PathVariable long reviewId, CurrentUser user) {
		return reviewService.get(reviewId, user.memberId());
	}

	@PostMapping("/{reviewId}/submit")
	public ReviewSummary submit(@PathVariable long reviewId, @Valid @RequestBody SubmitInput input,
			CurrentUser user) {
		return reviewService.submit(reviewId, user.memberId(), input);
	}

}
