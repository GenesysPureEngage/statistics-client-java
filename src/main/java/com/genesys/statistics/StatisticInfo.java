package com.genesys.statistics;

public class StatisticInfo {
    private String statisticId;
    private String objectId;
    private String objectType;

    public StatisticInfo(String statisticId, String objectId, String objectType) {
        this.statisticId = statisticId;
        this.objectId = objectId;
        this.objectType = objectType;
    }

    public String getStatisticId() {
        return statisticId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectType() {
        return objectType;
    }
}
