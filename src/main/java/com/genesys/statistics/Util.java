package com.genesys.statistics;

import com.genesys.internal.statistics.model.ResponseStatus;

public class Util
{
	private static final int STATUS_ASYNC_OK = 1;
	private static final int STATUS_OK = 0;

	public static void throwIfNotOk(ResponseStatus status) throws StatisticsException
	{
		Integer code = status.getCode();
		if (code != STATUS_ASYNC_OK && code != STATUS_OK)
		{
			throw new StatisticsException(String.format("%s (code: %d)", status.getMessage(), code));
		}
	}
}
