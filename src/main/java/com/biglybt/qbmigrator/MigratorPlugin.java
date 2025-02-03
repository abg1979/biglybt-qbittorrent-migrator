package com.biglybt.qbmigrator;

import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.UnloadablePlugin;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.qbmigrator.downloader.QBittorrent;

public class MigratorPlugin implements UnloadablePlugin {
    private PluginInterface pluginInterface;
    private static MigratorPlugin _instance;

    public static MigratorPlugin get() {
        return _instance;
    }

    public LoggingHelper logger() {
        return new LoggingHelper(pluginInterface.getLogger().getTimeStampedChannel("qbittorrent-migrator"));
    }

    @Override
    public void unload() {
    }

    @Override
    public void initialize(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
        _instance = this;
        BasicPluginConfigModel configModel = pluginInterface.getUIManager().createBasicPluginConfigModel("qbittorrent-migrator.configui");
        var host = configModel.addStringParameter2("qbwebuihost", "qbittorrent-migrator.qbwebuihost", "http://localhost:8080");
        var username = configModel.addStringParameter2("qbwebuiusername", "qbittorrent-migrator.qbwebuiusername", "admin");
        var password = configModel.addStringParameter2("qbwebuipassword", "qbittorrent-migrator.qbwebuipassword", "adminadmin");
        var btn = configModel.addActionParameter2("execute", "qbittorrent-migrator.execute");
        btn.addListener(parameter -> {
            pluginInterface.getUtilities().createThread("qbittorrent-migrator", () -> {
                var qb = new QBittorrent(host.getValue(), username.getValue(), password.getValue());
                qb.migrate(pluginInterface);
            });
        });
    }
}
