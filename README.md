# BiglyBT qBittorrent Migrator (Proof of concept)

## Step.0 Upgrade Java installation

This plugin write for Java 11+, so you need upgrade your Java installation to 11 and up.

## Step.1 Tweak BiglyBT settings

Before you begin, there are a few BiglyBT settings that need to be adjusted in order for the migration to run smoothly and efficiently.

* Backup everything, include your qBitorrent and BiglyBT configuration and .torrent files.
* Go `Files->Torrents`, turn on `Save .torrent files`
* Go `Files->Performance Options`, check `Recheck smallest downloads first` and `Allocate smallest downloads first`. This can increase the speed of restoring your Torrents
* Go `Startup & Shutdown`, change `Java Options -> Max heap memory size` up to `512MB` or higher, If you have a fairly large amount of torrents, adjust it a bit larger.

## Step.2 Stuff on qBittorrent side

Turn on qBittorrent's webui.

The IP is *listen-port*, It is not a real IP address. Leave the default `*` unchanged.

![image](https://github.com/user-attachments/assets/cbcd9c42-f7ee-473f-b01d-01be752af1f5)

## Step.3 Install plugin

Download BiglyBT plugin from this repository releases, and install it in BiglyBT.

![image](https://github.com/user-attachments/assets/4f49b292-b6f1-4e20-810e-8b83dc16e1f7)

![image](https://github.com/user-attachments/assets/0a3e4f23-e92c-49b2-ada4-c85d5ae39330)

## Step.4 Fill the WebUI information

Go `Plugins` and enter the new installed plugin's menu. (As you can see, there are still some issues with the translation, but it doesn't matter.)

Fill the WebUI information into inputs. (http://<host>:<port>) -> Host = Your qBittorrent machine IP (if they are on same machine, use `127.0.0.1`); Port = The port you have set in qBittorrent's WebUI settings.

![image](https://github.com/user-attachments/assets/3196b4b5-f010-40e6-ad71-004ce9b198cc)

And click **only once** `Execute/Migrate` button.

![image](https://github.com/user-attachments/assets/a4bb985e-c92b-41ee-b689-b412ab57693d)

Your qBittorrent torrents should to your BiglyBT with preserved settings below:

* Torrent display name
* Torrent upload/download speed limit settings
* Torrent categories
* Torrent tags
* Torrent save path settings

![image](https://github.com/user-attachments/assets/971a2e0b-6c13-4ba7-9f71-10d706e45008)

