package com.garodriguezlp.ccdh;

public class AssemblyEntry {

    private String name;
    private long sizeInBytes;
    private String sha256;

    public AssemblyEntry(String name, long sizeInBytes, String sha256) {
        this.name = name;
        this.sizeInBytes = sizeInBytes;
        this.sha256 = sha256;
    }

    public String getName() {
        return name;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getSha256() {
        return sha256;
    }
}
