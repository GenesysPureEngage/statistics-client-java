package com.genesys.statistics;

public class StatisticDesc extends StatisticInfo
{
	private String statisticId;
	private Object definition;

	public StatisticDesc(String statisticId, String objectId, String objectType, String name)
	{
		this(statisticId, objectId, objectType, name, null);
	}

	public StatisticDesc(String statisticId, String objectId, String objectType, Object definition)
	{
		this(statisticId, objectId, objectType, null, definition);
	}

	private StatisticDesc(String statisticId, String objectId, String objectType, String name, Object definition)
	{
		super(objectId, objectType, name);

		this.statisticId = statisticId;
		this.definition = definition;
	}

	public String getStatisticId()
	{
		return statisticId;
	}

	public Object getDefinition()
	{
		return definition;
	}

	public void setDefinition(Object definition)
	{
		this.definition = definition;
	}
}
