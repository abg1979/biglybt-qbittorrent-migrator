package com.biglybt.qbmigrator;

import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.UnloadablePlugin;
import com.biglybt.pif.logging.LoggerChannel;
import com.biglybt.pif.ui.config.BooleanParameter;
import com.biglybt.pif.ui.config.StringParameter;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.ui.model.BasicPluginViewModel;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MigratorPlugin implements UnloadablePlugin {
    private static MigratorPlugin _instance;
    private PluginInterface pluginInterface;
    private LoggerChannel loggingChannel;
    private BasicPluginViewModel loggingModel;
    private Semaphore semaphore;
    private StringParameter host;
    private StringParameter username;
    private StringParameter password;
    private BooleanParameter onlyLogBooleanParameter;
    private final AtomicInteger requestIndex = new AtomicInteger(0);

    public static MigratorPlugin get() {
        return _instance;
    }

    public LoggingHelper logger() {
        return new LoggingHelper(loggingChannel, loggingModel);
    }

    @Override
    public void unload() {
    }

    public void run() {
        try {
            if (semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                var qb = new QBittorrent(host.getValue(), username.getValue(), password.getValue());
                qb.migrate(pluginInterface, onlyLogBooleanParameter.getValue());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }

    private void execute() {
        if (semaphore.tryAcquire()) {
            try {
                String threadName = MigratorPlugin.class.getName() + "-" + requestIndex.incrementAndGet();
                pluginInterface.getUtilities().createThread(threadName, this::run);
            } finally {
                semaphore.release();
            }
        } else {
            logger().warn("Migration already in progress.");
        }
    }

    @Override
    public void initialize(PluginInterface pluginInterface) {
        _instance = this;
        this.pluginInterface = pluginInterface;
        loggingChannel = pluginInterface.getLogger().getTimeStampedChannel(MigratorPlugin.class.getName());
        loggingModel = pluginInterface.getUIManager().createLoggingViewModel(loggingChannel, true);
        semaphore = new Semaphore(1);
        BasicPluginConfigModel configModel = pluginInterface.getUIManager().createBasicPluginConfigModel("qbittorrent_migrator");
        host = configModel.addStringParameter2("qbt.migrator.host", "qbt.migrator.host", "http://localhost:8080");
        username = configModel.addStringParameter2("qbt.migrator.login", "qbt.migrator.login", "admin");
        password = configModel.addStringParameter2("qbt.migrator.password", "qbt.migrator.password", "adminadmin");
        onlyLogBooleanParameter = configModel.addBooleanParameter2("qbt.migrator.log", "qbt.migrator.log", true);
        var btn = configModel.addActionParameter2("qbt.migrator.run", "qbt.migrator.run");
        btn.addListener(parameter -> execute());
    }
}
