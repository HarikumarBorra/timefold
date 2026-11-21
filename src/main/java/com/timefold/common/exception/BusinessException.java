package com.timefold.common.exception;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = -471679016250432352L;

	public BusinessException(String message) {
		super(message);
	}

}