package com.ghostchu.peerbanhelper.downloaderplug.biglybt.qbmigrator;

import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.UnloadablePlugin;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.ghostchu.peerbanhelper.downloaderplug.biglybt.qbmigrator.downloader.QBittorrent;

public class MigratorPlugin implements UnloadablePlugin {
    private PluginInterface pluginInterface;

    @Override
    public void unload() {

    }

    @Override
    public void initialize(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
        BasicPluginConfigModel configModel = pluginInterface.getUIManager().createBasicPluginConfigModel("qbittorrent-migrator.configui");
        var host = configModel.addStringParameter2("qbwebuihost", "qbittorrent-migrator.qbwebuihost", "http://localhost:8080");
        var username = configModel.addStringParameter2("qbwebuiusername", "qbittorrent-migrator.qbwebuiusername", "admin");
        var password = configModel.addStringParameter2("qbwebuipassword", "qbittorrent-migrator.qbwebuipassword", "adminadmin");
        var btn = configModel.addActionParameter2("execute", "qbittorrent-migrator.execute");
        btn.addListener(parameter -> {
            var qb = new QBittorrent(host.getValue(),username.getValue(),password.getValue());
            qb.migrate(pluginInterface);
        });
    }
}
