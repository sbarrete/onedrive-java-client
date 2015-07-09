package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.Main;
import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Maps;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public class CheckFolderTask extends Task {

    private static final Logger log = Logger.getLogger(CheckFolderTask.class.getName());

    private final OneDriveAPI client;
    private final Item remoteFolder;
    private final File localFolder;

    public CheckFolderTask(OneDriveAPI client, Item remoteFolder, File localFolder) {

        Preconditions.checkNotNull(client);
        Preconditions.checkNotNull(remoteFolder);
        Preconditions.checkNotNull(localFolder);

        if (!remoteFolder.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }

        if (!localFolder.isDirectory()) {
            throw new IllegalArgumentException("Specified localFolder is not a folder");
        }

        this.client = client;
        this.remoteFolder = remoteFolder;
        this.localFolder = localFolder;
    }

    public int priority() {
        return 10;
    }

    @Override
    public String toString() {
        return "Check folder " + remoteFolder.getFullName();
    }

    @Override
    protected void taskBody() throws OneDriveAPIException {

        // Fetch the remote files
        log.info("Syncing folder " + remoteFolder.getFullName());
        Item[] remoteFiles = client.getChildren(remoteFolder);

        // Index the local files
        Map<String, File> localFiles = Maps.newHashMap();
        for (File file : localFolder.listFiles()) {
            localFiles.put(file.getName(), file);
        }

        for (Item remoteFile : remoteFiles) {

            File localFile = localFiles.get(remoteFile.getName());

            if (localFile != null) {

                if (remoteFile.isFolder() != localFile.isDirectory()) {
                    log.warning(String.format(
                            "Conflict detected in item '%s'. Local is %s, Remote is %s",
                            remoteFile.getFullName(),
                            remoteFile.isFolder() ? "directory" : "file",
                            localFile.isDirectory() ? "directory" : "file"));

                    continue;
                }

                if (remoteFile.isFolder()) {
                    Main.queue.add(new CheckFolderTask(client, remoteFile, localFile));
                } else {
                    Main.queue.add(new CheckFileTask(client, remoteFile, localFile));
                }

                localFiles.remove(remoteFile.getName());
            } else {
                log.info("TODO Item is extra - Would delete item?: " + remoteFile.getFullName());
            }
        }

        // Anything left does not exist on OneDrive

        // Filter stuff we don't want to upload
        // TODO config this out
        localFiles.remove("Thumbs.db");
        localFiles.remove(".picasa.ini");
        localFiles.remove("._.DS_Store");
        localFiles.remove(".DS_Store");

        for (File localFile : localFiles.values()) {
            if (localFile.isDirectory()) {
                Item createdItem = client.createFolder(remoteFolder, localFile.getName());
                log.info("Created new folder " + createdItem.getFullName());
                Main.queue.add(new CheckFolderTask(client, createdItem, localFile));
            } else {
                Main.queue.add(new UploadFileTask(client, remoteFolder, localFile, false));
            }
        }

    }
}