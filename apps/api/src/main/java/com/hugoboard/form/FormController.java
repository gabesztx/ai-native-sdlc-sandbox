package com.hugoboard.form;

import com.hugoboard.review.FormResponse;
import com.hugoboard.review.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/forms")
public class FormController {

	private final FormService formService;

	private final ReviewService reviewService;

	public FormController(FormService formService, ReviewService reviewService) {
		this.formService = formService;
		this.reviewService = reviewService;
	}

	@GetMapping
	public List<FormSummary> list() {
		return formService.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FormDetail create(@Valid @RequestBody FormInput input) {
		return formService.create(input);
	}

	@GetMapping("/{formId}")
	public FormDetail get(@PathVariable long formId) {
		return formService.get(formId);
	}

	@PutMapping("/{formId}")
	public FormDetail update(@PathVariable long formId, @Valid @RequestBody FormInput input) {
		return formService.update(formId, input);
	}

	@DeleteMapping("/{formId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long formId) {
		formService.delete(formId);
	}

	@PostMapping("/{formId}/send")
	public FormDetail send(@PathVariable long formId) {
		return formService.send(formId);
	}

	@GetMapping("/{formId}/responses")
	public List<FormResponse> responses(@PathVariable long formId) {
		return reviewService.responses(formId);
	}

}
