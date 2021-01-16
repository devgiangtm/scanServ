package com.loffler.scanServ;

import java.net.URL;

public class DownloadData {
    public String path;
    public URL url;
    public DownloadData(String path, URL url) {
        this.path = path;
        this.url = url;
    }
}