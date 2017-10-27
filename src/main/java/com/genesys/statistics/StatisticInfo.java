package com.genesys.statistics;

public class StatisticInfo {
    private String objectId;
    private String objectType;
    private String name;

    public StatisticInfo(String objectId, String objectType, String name) {
        this.name = name;
        this.objectId = objectId;
        this.objectType = objectType;
    }

    public String getName() {
        return name;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectType() {
        return objectType;
    }
}
