package com.genesys.statistics;

public enum ServiceState
{
	Unknown,
	Available,
	Unavailable;

	public static ServiceState fromString(String s)
	{
		for (ServiceState state : ServiceState.values())
		{
			if (state.toString().equalsIgnoreCase(s))
			{
				return state;
			}
		}

		return Unknown;
	}
}
