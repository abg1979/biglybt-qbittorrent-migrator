package com.biglybt.qbmigrator;

import com.biglybt.pif.logging.LoggerChannel;
import com.biglybt.pif.logging.LoggerChannelListener;
import com.biglybt.pif.ui.model.BasicPluginViewModel;

import java.io.StringWriter;

public class LoggingHelper implements LoggerChannelListener {
    private final LoggerChannel channel;
    private final BasicPluginViewModel loggingModel;

    public LoggingHelper(LoggerChannel channel, BasicPluginViewModel loggingModel) {
        this.channel = channel;
//        this.channel.addListener(this);
        this.loggingModel = loggingModel;
    }

    public void info(String message) {
        channel.log(LoggerChannel.LT_INFORMATION, message);
    }

    public void error(String message) {
        channel.log(LoggerChannel.LT_ERROR, message);
    }

    public void error(String message, Throwable throwable) {
        channel.log(message, throwable);
    }

    public void warn(String message) {
        channel.log(LoggerChannel.LT_WARNING, message);
    }

    private static final String LOG_FORMAT = "[%s]|%s|%s\n";

    @Override
    public void messageLogged(int type, String content) {
        String threadName = Thread.currentThread().getName();
        switch (type) {
            case LoggerChannel.LT_INFORMATION:
                loggingModel.getLogArea().appendText(String.format(LOG_FORMAT, "INFO", threadName, content));
                break;
            case LoggerChannel.LT_WARNING:
                loggingModel.getLogArea().appendText(String.format(LOG_FORMAT, "WARN", threadName, content));
                break;
            case LoggerChannel.LT_ERROR:
                loggingModel.getLogArea().appendText(String.format(LOG_FORMAT, "ERROR", threadName, content));
                break;
        }
    }

    @Override
    public void messageLogged(String str, Throwable error) {
        StringWriter sw = new StringWriter();
        sw.write(str + "\n");
        error.printStackTrace(new java.io.PrintWriter(sw));
        messageLogged(LoggerChannel.LT_ERROR, sw.toString());
    }
}
