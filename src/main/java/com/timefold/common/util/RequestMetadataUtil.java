package com.timefold.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestMetadataUtil {

	private RequestMetadataUtil() {
	}

	public static String extractClientIp(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	public static String extractUserAgent(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		return request.getHeader("User-Agent");
	}

}