package com.timefold.capsule.event;

import java.time.Instant;

public record CapsuleUnlockedEvent(Long capsuleId, String capsulePublicId, Instant unlockedAtUtc) {

}