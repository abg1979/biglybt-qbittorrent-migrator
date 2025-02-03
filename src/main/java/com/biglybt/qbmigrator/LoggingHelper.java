package com.biglybt.qbmigrator;

import com.biglybt.pif.logging.LoggerChannel;

public class LoggingHelper {
    private final LoggerChannel channel;

    public LoggingHelper(LoggerChannel channel) {
        this.channel = channel;
    }

    public void info(String message) {
        channel.log(LoggerChannel.LT_INFORMATION, message);
    }

    public void error(String message) {
        channel.log(LoggerChannel.LT_ERROR, message);
    }

    public void error(String message, Throwable throwable) {
        channel.log(LoggerChannel.LT_ERROR, message, throwable);
    }

    public void warn(String message) {
        channel.log(LoggerChannel.LT_WARNING, message);
    }

}
