package com.genesys.statistics;

public class StatisticDesc extends StatisticInfo {
    private String name;
    private Object definition;

    public StatisticDesc(String statisticId, String objectId, String objectType, String name) {
        this(statisticId, objectId, objectType, name, null);
    }
    
    public StatisticDesc(String statisticId, String objectId, String objectType, Object definition) {
        this(statisticId, objectId, objectType, null, definition);
    }

    private StatisticDesc(String statisticId, String objectId, String objectType, String name, Object definition) {
        super(statisticId, objectId, objectType);
        this.name = name;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getDefinition() {
        return definition;
    }

    public void setDefinition(Object definition) {
        this.definition = definition;
    }
}
