package com.hugoboard.common;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

public final class Timestamps {

	private Timestamps() {
	}

	public static String now(Clock clock) {
		return clock.instant().truncatedTo(ChronoUnit.SECONDS).toString();
	}

}
