package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.Drive;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.ItemSet;
import jersey.repackaged.com.google.common.collect.Lists;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ROOneDriveAPI implements OneDriveAPI {

    protected final Client client;
    protected final OneDriveAuth authoriser;

    public ROOneDriveAPI(Client client, OneDriveAuth authoriser) {
        this.authoriser = authoriser;
        this.client = client;
    }

    public Drive getDefaultDrive() throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive")
                .method("GET");

        return request.getResponse(Drive.class);
    }

    public Item getRoot() throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root")
                .method("GET");

        return request.getResponse(Item.class);
    }

    public Item[] getChildren(OneDriveItem parent) throws OneDriveAPIException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        List<Item> itemsToReturn = Lists.newArrayList();

        String token = null;

        do {
            OneDriveRequest request = getDefaultRequest()
                    .path("/drive/items/" + parent.getId() + "/children")
                    .skipToken(token)
                    .method("GET");

            ItemSet items = request.getResponse(ItemSet.class);
            Collections.addAll(itemsToReturn, items.getValue());
            token = items.getNextToken();

        } while (token != null); // If we have a token for the next page we need to keep going

        return itemsToReturn.toArray(new Item[itemsToReturn.size()]);
    }

    public Item getPath(String path) throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root:/" + path)
                .withChildren()
                .method("GET");

        return request.getResponse(Item.class);
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, file.getName());
    }

    public OneDriveItem uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, file.getName());
    }

    public OneDriveItem uploadFileInChunks(OneDriveItem parent, File file, int chunkSize) throws OneDriveAPIException, IOException {
        return OneDriveItem.FACTORY.create(parent, file.getName());
    }

    public OneDriveItem updateFile(Item item, Date createdDate, Date modifiedDate) throws OneDriveAPIException {
        // Do nothing, just return the unmodified item
        return item;
    }

    public OneDriveItem createFolder(OneDriveItem parent, String name) throws OneDriveAPIException {
        // Return a dummy folder
        return OneDriveItem.FACTORY.create(parent, name, true);
    }

    public void download(Item item, File target) throws OneDriveAPIException {
        // Do nothing
    }

    public void delete(Item remoteFile) throws OneDriveAPIException {
        // Do nothing
    }

    protected OneDriveRequest getDefaultRequest() {
        return new OneDriveRequest(client, authoriser, "https://api.onedrive.com/v1.0");
    }
}