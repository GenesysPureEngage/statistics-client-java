package com.genesys.statistics;

public class StatisticsException extends Exception
{
	public StatisticsException(String msg)
	{
		super(msg);
	}

	public StatisticsException(String msg, Exception cause)
	{
		super(msg, cause);
	}
}
