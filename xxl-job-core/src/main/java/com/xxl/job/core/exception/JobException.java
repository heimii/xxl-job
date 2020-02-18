package com.xxl.job.core.exception;

/**
 * @author zhong
 * @date 2020-2-18
 */
public class JobException extends RuntimeException {

    public JobException() {
    }

    public JobException(String message) {
        super(message);
    }

    public JobException(String message, Exception ex) {
        super(message, ex);
    }

}
