package com.hugoboard.review;

import com.hugoboard.auth.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/me/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping
	@Operation(operationId = "listMyReviews")
	public List<ReviewSummary> list(CurrentUser user) {
		return reviewService.listFor(user.memberId());
	}

	@GetMapping("/{reviewId}")
	@Operation(operationId = "getMyReview")
	public ReviewDetail get(@PathVariable long reviewId, CurrentUser user) {
		return reviewService.get(reviewId, user.memberId());
	}

	@PostMapping("/{reviewId}/submit")
	@Operation(operationId = "submitMyReview")
	public ReviewSummary submit(@PathVariable long reviewId, @Valid @RequestBody SubmitInput input,
			CurrentUser user) {
		return reviewService.submit(reviewId, user.memberId(), input);
	}

}
