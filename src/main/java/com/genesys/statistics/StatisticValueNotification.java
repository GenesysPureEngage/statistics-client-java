package com.genesys.statistics;

import com.genesys.internal.statistics.model.StatisticValue;

public class StatisticValueNotification
{
	private final String subscriptionId;
	private final StatisticValue value;

	public StatisticValueNotification(String subscriptionId, StatisticValue value)
	{
		this.subscriptionId = subscriptionId;
		this.value = value;
	}

	public String getSubscriptionId()
	{
		return subscriptionId;
	}

	public StatisticValue getValue()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return "StatisticValueNotification{" + "subscriptionId='" + subscriptionId + '\'' + ", value=" + value + '}';
	}

}
