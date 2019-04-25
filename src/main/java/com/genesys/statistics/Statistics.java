package com.genesys.statistics;

import com.genesys.internal.common.ApiClient;
import com.genesys.internal.common.ApiException;
import com.genesys.internal.statistics.api.StatisticsApi;
import com.genesys.internal.statistics.model.*;
import com.google.common.util.concurrent.SettableFuture;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Statistics
{
	static final String SESSION_COOKIE = "STATISTICS_SESSIONID";
	private static final Logger logger = LoggerFactory.getLogger(Statistics.class);
	final Collection<StatisticsListener> listeners = new ConcurrentLinkedQueue<>();
	private final String apiKey;
	private final String serviceUrl;
	private final StatisticsApi api;
	private final Notifications notifications;
	private final Map<String, Object> notificationOptions;
	private Proxy proxy;

	public Statistics(final String apiKey, final String baseUrl)
	{
		this(apiKey, baseUrl, new StatisticsApi(), new Notifications(), null);
	}

	public Statistics(final String apiKey, final String baseUrl, final Map<String, Object> notificationOptions)
	{
		this(apiKey, baseUrl, new StatisticsApi(), new Notifications(), notificationOptions);
	}

	private Statistics(final String apiKey, final String baseUrl, final StatisticsApi api, final Notifications notifications, final Map<String, Object> options)
	{
		this.apiKey = apiKey;
		this.serviceUrl = String.format("%s/statistics/v3", baseUrl);
		this.api = api;
		this.api.getApiClient().setBasePath(serviceUrl);
		this.notifications = notifications;
		this.notificationOptions = options == null ? new HashMap<String, Object>(1) : new HashMap<String, Object>(options);
		api.getApiClient().addDefaultHeader("x-api-key", apiKey);
	}
	
	public Proxy getProxy() {
		return proxy;
	}
	
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
		api.getApiClient().getHttpClient().setProxy(proxy);
		notifications.setProxy(proxy);
	}

        /**
         *  Initialize the Statistics client library.
         * 
         * @param token The authorization token you received during authentication by following the Authorization Code Grant flow.
         * @return 
         */
	public Future<Void> initialize(String token)
	{
		this.api.getApiClient().addDefaultHeader("Authorization", String.format("Bearer %s", token));
		final SettableFuture<Void> future = SettableFuture.create();

		try
		{
			final ApiClient client = new ApiClient();
			client.getHttpClient().setProxy(proxy);
			List<Interceptor> interceptors = client.getHttpClient().interceptors();
			interceptors.add(new TraceInterceptor());

			final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger()
			{
				@Override
				public void log(String message)
				{
					logger.debug(message);
				}
			});
			loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
			interceptors.add(loggingInterceptor);

			CookieStoreImpl cookieStore = new CookieStoreImpl()
			{
				@Override
				public void add(URI uri, HttpCookie cookie)
				{
					if (!future.isDone() && SESSION_COOKIE.equalsIgnoreCase(cookie.getName()))
					{
						String value = cookie.getValue();
						logger.debug("Session created: {}; {}", value, cookie.getPath());

						future.set(null);
					}

					super.add(uri, cookie); //To change body of generated methods, choose Tools | Templates.
				}
			};
			client.getHttpClient().setCookieHandler(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));
			client.setBasePath(serviceUrl);
			client.addDefaultHeader("x-api-key", apiKey);
			client.addDefaultHeader("Authorization", String.format("Bearer %s", token));

			api.setApiClient(client);

			notifications.setCookieStore(cookieStore);
			notifications.subscribe("/statistics/v3/service", new Notifications.NotificationListener()
			{
				@Override
				public void onNotification(String channel, Map<String, Object> data)
				{
					onServiceChange(data);
				}
			});
			notifications.subscribe("/statistics/v3/updates", new Notifications.NotificationListener()
			{
				@Override
				public void onNotification(String channel, Map<String, Object> data)
				{
					onValues(data);
				}
			});
			notifications.initialize(serviceUrl + "/notifications", apiKey, token, notificationOptions);
		}
		catch (Exception ex)
		{
			future.setException(ex);
		}

		return future;
	}

        /**
        *  Open a subscription for the specified set of statistics. Verbose is set to `true` by default.
        * 
        * @param operationId A unique string (we recommend using a UUID/GUID) that the Statistics API uses as the subscriptionId.
        * @param descriptors Definitions of the statistics to be monitored.
        * @return
        * @throws StatisticsException 
        */
	public StatisticDataResponse createSubscription(String operationId, StatisticDesc[] descriptors) throws StatisticsException
	{
		return createSubscription(operationId, descriptors, true);
	}

        /**
        *  Open a subscription for the specified set of statistics.
        * 
        * @param operationId A unique string (we recommend using a UUID/GUID) that the Statistics API uses as the subscriptionId.
        * @param descriptors Definitions of the statistics to be monitored.
        * @param verbose Specifies whether the Statistics API should return additional information about opened statistics in the response.
        * @return
        * @throws StatisticsException 
        */
	public StatisticDataResponse createSubscription(String operationId, StatisticDesc[] descriptors, boolean verbose) throws StatisticsException
	{
		try
		{
			Map<String, Object> data = new HashMap<>();
			data.put("statistics", descriptors);
			Map<String, Object> body = new HashMap<>();
			body.put("data", data);
			body.put("operationId", operationId);

			return api.createSubscription(body, verbose ? "INFO" : "OFF");
		}
		catch (ApiException ex)
		{
			throw new StatisticsException("Cannot create subscription", ex);
		}
	}
        
        /**
         *  Delete the specified subscription by closing all its statistics.
         * 
         * @param id The ID of the subscription to delete.
         * @return
         * @throws StatisticsException 
         */
	public ModelApiResponse deleteSubscription(String id) throws StatisticsException
	{
		try
		{
			return api.deleteSubscription(id);
		}
		catch (ApiException ex)
		{
			throw new StatisticsException("Cannot delete subscription", ex);
		}
	}

        /**
        *  Get the current value of a statistic from Stat Server.
        * 
        * @param statisticName The name of the pre-configured statistic to retrieve.
        * @param objectId The ID of the object.
        * @param objectType The type of object the statistic is for.
        * @return
        * @throws StatisticsException 
        */
	public PeekedStatisticValue getStatValue(String statisticName, String objectId, String objectType) throws StatisticsException
	{
		try
		{
			PeekedStatisticResponse response = api.getStatValue(objectId, objectType, statisticName);
			Util.throwIfNotOk(response.getStatus());

			PeekedStatistic data = response.getData();
			return data.getStatistic();
		}
		catch (ApiException ex)
		{
			throw new StatisticsException("Cannot get statistics value", ex);
		}
	}

        /**
        *  Get the current value of predefined statistics from Stat Server without a subscription.
        * 
        * @param infos The set of statistics you want to get the values for from Stat Server.
        * @return
        * @throws StatisticsException 
        */
	public PeekedStatisticsResponse getStatValues(StatisticInfo[] infos) throws StatisticsException
	{
		try
		{
			Map<String, Object> data = new HashMap<>();
			data.put("statistics", infos);
			Map<String, Object> body = new HashMap<>();
			body.put("data", data);

			return api.getStatValues(body);
		}
		catch (ApiException ex)
		{
			throw new StatisticsException("Cannot get statistics values", ex);
		}
	}

        /**
        *  Get the value of a set of statistics that was opened with a subscription.
        * 
        * @param subscriptionId The ID of the subscription.
        * @return
        * @throws StatisticsException 
        */ 
	public StatisticDataResponse peekSubscriptionStats(String subscriptionId) throws StatisticsException
	{
		return peekSubscriptionStats(subscriptionId, new String[] {});
	}

        /**
        *  Get the values of a set of statistics that was opened with a subscription (verbose is true by default)
        * 
        * @param subscriptionId The ID of the subscription.
        * @param statisticIds A list of statistic IDs that belong to the specified subscription. If omitted, the Statistics API returns the current values of all statistics opened within the subscription. If specified, the Statistics API returns values for the statistics with the specified IDs.
        * @return
        * @throws StatisticsException 
        */
	public StatisticDataResponse peekSubscriptionStats(String subscriptionId, String[] statisticIds) throws StatisticsException
	{
		return peekSubscriptionStats(subscriptionId, statisticIds, true);
	}

        /**
        *  Get the values of a set of statistics that was opened with a subscription.
        * 
        * @param subscriptionId
        * @param statisticIds A list of statistic IDs that belong to the specified subscription. If omitted, the Statistics API returns the current values of all statistics opened within the subscription. If specified, the Statistics API returns values for the statistics with the specified IDs.
        * @param verbose Specifies whether the Statistics API should return additional information about opened statistics in the response. 
        * @return
        * @throws StatisticsException 
        */
	public StatisticDataResponse peekSubscriptionStats(String subscriptionId, String[] statisticIds, boolean verbose) throws StatisticsException
	{
		try
		{
			final String statIds = formStatIds(statisticIds);
			return api.peekSubscriptionStats(subscriptionId, statIds, verbose ? "INFO" : "OFF");
		}
		catch (ApiException ex)
		{
			throw new StatisticsException("Cannot peek subscription statistics", ex);
		}
	}

	private String formStatIds(String[] statisticIds)
	{
		if (statisticIds == null)
		{
			return null;
		}
		final ArrayList<String> ids = new ArrayList<>(statisticIds.length);
		for (String statId : statisticIds)
		{
			String stripped = StringUtils.stripToNull(statId);
			if (StringUtils.isNotBlank(stripped))
			{
				ids.add(stripped);
			}
		}
		return StringUtils.stripToNull(StringUtils.join(ids, ','));
	}

	private StatisticValueNotification getValueNotification(Map map)
	{
		StatisticValue value = new StatisticValue();
		value.setStatisticId(safeCast(map.get("statisticId"), String.class));
		value.setTimestamp(safeCast(map.get("timestamp"), Long.class));
		value.setName(safeCast(map.get("name"), String.class));
		value.setValue(map.get("value"));
		value.setObjectId(safeCast(map.get("objectId"), String.class));
		value.setObjectType(safeCast(map.get("objectType"), String.class));
		final String subscriptionId = safeCast(map.get("subscriptionId"), String.class);
		return new StatisticValueNotification(subscriptionId, value);
	}

	private <T> T safeCast(Object argument, Class<T> clazz)
	{
		if (clazz.isInstance(argument))
		{
			return (T) argument;
		}
		return null;
	}

	private Map extractData(Map<String, Object> msg)
	{
		final Object data = msg.get("data");
		if (data instanceof Map)
		{
			return (Map) data;
		}
		else
			return msg;
	}

	private void onValues(Map<String, Object> msg)
	{
		Map msgData = extractData(msg);
		final String key = "statistics";
		if (msgData.containsKey(key))
		{
			List<StatisticValueNotification> list = new ArrayList<>();
			Object[] statistics = safeCast(msgData.get(key), Object[].class);
			if (statistics != null)
			{
				for (Object obj : statistics)
				{
					Map map = safeCast(obj, Map.class);
					if (map != null)
					{
						StatisticValueNotification value = getValueNotification(map);
						list.add(value);
					}
				}
			}

			for (StatisticsListener listener : listeners)
			{
				try
				{
					listener.onValues(list);
				}
				catch (Exception ex)
				{
					logger.error("Error notifying listener", ex);
				}
			}
		}
		else
		{
			logger.debug("{}", msg);
			logger.error("Invalid message");
		}
	}

	private void onServiceChange(Map<String, Object> msg)
	{
		Map msgData = extractData(msg);
		final String key = "serviceState";
		if (msgData.containsKey(key))
		{
			String value = String.valueOf(msgData.get(key));
			ServiceState state = ServiceState.fromString(value);
			if (state == ServiceState.Unknown)
			{
				logger.warn("Unknown service state: {}", value);
			}

			for (StatisticsListener listener : listeners)
			{
				try
				{
					listener.onServiceChange(state);
				}
				catch (Exception ex)
				{
					logger.error("", ex);
				}
			}
		}
		else
		{
			logger.debug("{}", msg);
			logger.error("Invalid message");
		}
	}

        /**
        *  Add a listener for the ServiceState (`onServiceChange`) and StatisticValueNotification (`onValues`) event notifications.
        * 
        * @param listener 
        */
	public void addListener(StatisticsListener listener)
	{
		listeners.add(listener);
	}

        /**
         *  Remove the events listener.
         * 
         * @param listener 
         */
	public void removeListener(StatisticsListener listener)
	{
		listeners.remove(listener);
	}

	public void destroy() throws StatisticsException
	{
		notifications.disconnect();
	}

	public void destroy(long disconnectRequestTimeout) throws StatisticsException
	{
		notifications.disconnect(disconnectRequestTimeout);
	}

	public interface StatisticsListener
	{
		void onServiceChange(ServiceState state);

		void onValues(Collection<StatisticValueNotification> list);
	}
}
