package com.biglybt.qbmigrator.downloader;

import com.biglybt.core.tag.TagManager;
import com.biglybt.core.tag.TagManagerFactory;
import com.biglybt.core.tag.TagType;
import com.biglybt.core.util.TorrentUtils;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pifimpl.local.download.DownloadImpl;
import com.biglybt.pifimpl.local.torrent.TorrentImpl;
import com.biglybt.qbmigrator.LoggingHelper;
import com.biglybt.qbmigrator.MigratorPlugin;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.gudy.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class QBittorrent {
    private static final Gson GSON = new Gson();
    public static final Type TYPE_LIST_TORRENT_METADATA = new TypeToken<List<QBittorrentTorrentMeta>>() {
    }.getType();
    private final String apiEndpoint;
    private final Methanol httpClient;
    private final String username;
    private final String password;

    public QBittorrent(String endpoint, String username, String password) {
        this.apiEndpoint = endpoint + "/api/v2";
        this.username = username;
        this.password = password;
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        Methanol.Builder builder = Methanol.newBuilder().version(HttpClient.Version.HTTP_1_1).defaultHeader("Accept-Encoding", "gzip,deflate").followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.of(10, ChronoUnit.SECONDS)).headersTimeout(Duration.of(10, ChronoUnit.SECONDS)).readTimeout(Duration.of(30, ChronoUnit.SECONDS)).requestTimeout(Duration.of(30, ChronoUnit.SECONDS)).cookieHandler(cm);
        this.httpClient = builder.build();
    }

    private LoggingHelper logger() {
        return MigratorPlugin.get().logger();
    }

    public void migrate(PluginInterface pif) {
        if (!login()) {
            logger().error("Failed to login qBittorrent WebUI!");
            return;
        }
        int success = 0;
        int failed = 0;
        logger().info("Migrating torrents from qBittorrent WebUI...");
        for (QBittorrentTorrentMeta qbTorrent : getTorrentsMeta()) {
            try {
                // check if this is already added
                if (pif.getDownloadManager().getDownload(Hex.decode(qbTorrent.getHash())) != null) {
                    logger().info("Skipping torrent " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "] as it is already added.");
                    continue;
                }
                logger().info("Migrating torrent " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "]...");
                var torrentFile = downloadTorrent(qbTorrent.getHash());
                logger().info("Downloaded torrent file for " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "]");
                var torrent = pif.getTorrentManager().createFromBEncodedData(torrentFile);
                var download = pif.getDownloadManager().addDownloadStopped(torrent, null, new File(qbTorrent.getSavePath()));
                logger().info("Added download for " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "] with save path [" + qbTorrent.getSavePath() + "]");
                if (!qbTorrent.getCategory().isBlank()) {
                    download.setCategory(qbTorrent.getCategory());
                    logger().info("Set category for " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "] to [" + qbTorrent.getCategory() + "]");
                }
                download.setDownloadRateLimitBytesPerSecond(qbTorrent.getDlLimit().intValue());
                download.setUploadRateLimitBytesPerSecond(qbTorrent.getUpLimit().intValue());
                TorrentUtils.setDisplayName(((TorrentImpl) torrent).getTorrent(), qbTorrent.getName());
                TagManager tm = TagManagerFactory.getTagManager();
                var tagType = tm.getTagType(TagType.TT_DOWNLOAD_MANUAL);
                if (!qbTorrent.getTags().trim().isBlank()) {
                    for (String s : qbTorrent.getTags().split(",")) {
                        s = s.trim();
                        var tag = tagType.getTag(s, true);
                        if (tag == null) tag = tagType.createTag(s, true);
                        tag.addTaggable(((DownloadImpl) download).getDownload());
                        logger().info("Added tag [" + s + "] to " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "]");
                    }
                }
                logger().info("Successfully migrated torrent " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "]");
                success++;
                // download.recheckData();
            } catch (Exception e) {
                logger().error("Failed to migrate torrent " + qbTorrent.getName() + " [" + qbTorrent.getHash() + "]", e);
                failed++;
            }
        }
        logger().info("Migrated [" + success + "] torrents with [" + failed + "] fails.");
    }

    public byte[] downloadTorrent(String hash) throws IOException, InterruptedException {
        File file = Files.createTempFile("bbt-pbh-qbmigrator", ".torrent").toFile();
        file.deleteOnExit();
        var resp = httpClient.send(MutableRequest.GET(apiEndpoint + "/torrents/export?hash=" + hash), HttpResponse.BodyHandlers.ofByteArray());
        return resp.body();
    }

    public List<QBittorrentTorrentMeta> getTorrentsMeta() {
        HttpResponse<String> request = null;
        logger().info("Retrieving torrents meta from qBittorrent WebUI...");
        try {
            request = httpClient.send(MutableRequest.GET(apiEndpoint + "/torrents/info"), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (request.statusCode() == 200) {
                List<QBittorrentTorrentMeta> torrents = GSON.fromJson(request.body(), TYPE_LIST_TORRENT_METADATA);
                logger().info("Retrieved " + torrents.size() + " torrents meta from qBittorrent WebUI");
                return torrents;
            }
            logger().error("Failed to retrieve torrents meta from qBittorrent WebUI: " + request.statusCode());
        } catch (IOException | InterruptedException e) {
            logger().error("Failed to retrieve torrents meta from qBittorrent WebUI", e);
        }
        return List.of();
    }

    public boolean login() {
        try {
            logger().info("Logging in qBittorrent WebUI...");
            HttpResponse<String> request = httpClient.send(MutableRequest.POST(apiEndpoint + "/auth/login", FormBodyPublisher.newBuilder().query("username", username).query("password", password).build()).header("Content-Type", "application/x-www-form-urlencoded"), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // return request.statusCode() == 200;
            return request.statusCode() == 200 && isLoggedIn();
        } catch (Exception e) {
            logger().error("Could not login qBittorrent WebUI", e);
            return false;
        }
    }

    public boolean isLoggedIn() {
        HttpResponse<Void> resp;
        try {
            logger().info("Checking qBittorrent WebUI login status...");
            resp = httpClient.send(MutableRequest.GET(apiEndpoint + "/app/version"), HttpResponse.BodyHandlers.discarding());
            logger().info("qBittorrent WebUI login status: " + resp.statusCode());
        } catch (Exception e) {
            return false;
        }
        return resp.statusCode() == 200;
    }
}
