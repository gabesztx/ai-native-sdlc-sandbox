package com.hugoboard.dashboard;

import com.hugoboard.common.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(path = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
class DashboardController {

	private final DashboardService dashboardService;

	private final DashboardEventBroadcaster broadcaster;

	DashboardController(DashboardService dashboardService, DashboardEventBroadcaster broadcaster) {
		this.dashboardService = dashboardService;
		this.broadcaster = broadcaster;
	}

	@GetMapping
	@Operation(operationId = "getDashboard")
	public Dashboard get() {
		return dashboardService.getDashboard();
	}

	@GetMapping("/issues")
	@Operation(operationId = "listDashboardIssues")
	public List<Issue> issues(@RequestParam String tile) {
		return dashboardService.getIssues(parseTile(tile));
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(operationId = "streamDashboard")
	public SseEmitter stream() {
		return broadcaster.register(dashboardService.getDashboard());
	}

	private static Tile parseTile(String value) {
		try {
			return Tile.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Unknown tile", Map.of("tile", "Unknown tile: " + value));
		}
	}

}
