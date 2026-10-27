package com.hugoboard.dashboard;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
class DashboardEventBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(DashboardEventBroadcaster.class);

	private static final long EMITTER_TIMEOUT_MS = 30L * 60 * 1000;

	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

	SseEmitter register(Dashboard initialState) {
		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));
		send(emitter, initialState);
		return emitter;
	}

	void broadcast(Dashboard dashboard) {
		for (SseEmitter emitter : emitters) {
			send(emitter, dashboard);
		}
	}

	private void send(SseEmitter emitter, Dashboard dashboard) {
		try {
			emitter.send(SseEmitter.event().name("dashboard").data(dashboard));
		} catch (Exception e) {
			emitters.remove(emitter);
			log.debug("Removing a dashboard SSE client after a failed send: {}", e.getMessage());
		}
	}

}
