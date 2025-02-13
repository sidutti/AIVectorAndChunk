package com.sidutti.charlie.task;

public record TaskError(String message, Throwable throwable) {
}
