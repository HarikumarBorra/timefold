package com.timefold.scheduler;

import com.timefold.capsule.service.CapsuleUnlockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CapsuleUnlockScheduler {

	private static final Logger log = LoggerFactory.getLogger(CapsuleUnlockScheduler.class);

	private final CapsuleUnlockService capsuleUnlockService;

	public CapsuleUnlockScheduler(CapsuleUnlockService capsuleUnlockService) {
		this.capsuleUnlockService = capsuleUnlockService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void recoverMissedUnlocksOnStartup() {
		log.info("Running startup recovery check for missed capsule unlocks...");
		capsuleUnlockService.processDueCapsules("STARTUP_RECOVERY");
	}

	@Scheduled(fixedDelayString = "${app.scheduler.unlock-poll-ms:30000}", initialDelay = 15000)
	public void pollDueCapsules() {
		capsuleUnlockService.processDueCapsules("SCHEDULED_POLL");
	}

}