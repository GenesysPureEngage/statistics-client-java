package com.genesys.statistics;

import com.genesys.internal.common.ApiClient;
import com.genesys.internal.common.ApiException;
import com.genesys.internal.statistics.api.StatisticsApi;
import com.genesys.internal.statistics.model.*;
import com.google.common.util.concurrent.SettableFuture;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class Statistics {
    private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

    public static interface StatisticsListener {
        void onServiceChange(ServiceState state);
        void onValues(Collection<StatisticValue> list);
    }
    
    static final String SESSION_COOKIE = "STATISTICS_SESSIONID";
    
    private String apiKey;
    private String serviceUrl;
    private StatisticsApi api;
    private Notifications notifications;
    
    final Collection<StatisticsListener> listeners = new ConcurrentLinkedQueue<>();

    public Statistics(String apiKey, String baseUrl) {
        this(apiKey, baseUrl, new StatisticsApi(), new Notifications());
    }

    Statistics(String apiKey, String baseUrl, StatisticsApi api, Notifications notifications) {
        this.apiKey = apiKey;
        this.serviceUrl = String.format("%s/statistics/v3", baseUrl);
        this.api = api;
        this.notifications = notifications;
    }
    
    public Future<Void> initialize(String token) {
        
        final SettableFuture<Void> future = SettableFuture.create();
        
        try {
            final ApiClient client = new ApiClient();
            List<Interceptor> interceptors = client.getHttpClient().interceptors();
            interceptors.add(new TraceInterceptor());

            final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    logger.debug(message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            interceptors.add(loggingInterceptor);

            CookieStoreImpl cookieStore = new CookieStoreImpl() {
                @Override
                public void add(URI uri, HttpCookie cookie) {
                    if(!future.isDone() && SESSION_COOKIE.equalsIgnoreCase(cookie.getName())) {
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
            notifications.subscribe("/statistics/v3/service", new Notifications.NotificationListener() {
                @Override
                public void onNotification(String channel, Map<String, Object> data) {
                    onServiceChange(data);
                }
            });
            notifications.subscribe("/statistics/v3/updates", new Notifications.NotificationListener() {
                @Override
                public void onNotification(String channel, Map<String, Object> data) {
                    onValues(data);
                }
            });
            notifications.initialize(serviceUrl + "/notifications", apiKey, token);
        }
        catch(Exception ex) {
            future.setException(ex);
        }
        
        return future;
    }
    
    public StatisticData createSubscription(String operationId, StatisticDesc[] descriptors) throws StatisticsException {
        return createSubscription(operationId, descriptors, true);
    }
    
    public StatisticData createSubscription(String operationId, StatisticDesc[] descriptors, boolean verbose) throws StatisticsException {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("statistics", descriptors);
            Map<String,Object> body = new HashMap<>();
            body.put("data", data);
            body.put("operationId", operationId);
            
            StatisticDataResponse response = api.createSubscriptionUsingPOST(body, verbose? "INFO": "OFF");
            Util.throwIfNotOk(response.getStatus());
            
            return response.getData();
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot create subscription", ex);
        }
    }

    public void deleteSubscription(String id) throws StatisticsException {
        try {
            ModelApiResponse response = api.deleteSubscription(id);
            Util.throwIfNotOk(response.getStatus());
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot delete subscription", ex);
        }
    }

    public PeekedStatisticValue getStatValue(String statisticName, String objectId, String objectType) throws StatisticsException {
        try {
            PeekedStatisticResponse response = api.getStatValue(statisticName, objectId, objectType);
            Util.throwIfNotOk(response.getStatus());
            
            PeekedStatistic data = response.getData();
            return data.getStatistic();
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot get statistics value", ex);
        }
    }

    public List<PeekedStatisticValue> getStatValues(StatisticInfo[] infos) throws StatisticsException {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("statistics", infos);
            Map<String,Object> body = new HashMap<>();
            body.put("data", data);
            
            PeekedStatisticsResponse response = api.getStatValues(body);
            Util.throwIfNotOk(response.getStatus());
            
            return response.getData().getStatistics();
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot get statistics values", ex);
        }
    }
    
    public List<StatisticValue> peekSubscriptionStats(String subscriptionId) throws StatisticsException {
        return peekSubscriptionStats(subscriptionId, new String[] {});
    }
    
    public List<StatisticValue> peekSubscriptionStats(String subscriptionId, String[] statisticIds) throws StatisticsException {
        return peekSubscriptionStats(subscriptionId, statisticIds, true);
    }

    public List<StatisticValue> peekSubscriptionStats(String subscriptionId, String[] statisticIds, boolean verbose) throws StatisticsException
    {
        try
        {
            final String statIds = formStatIds(statisticIds);
            StatisticDataResponse resp = api.peekSubscriptionStats(subscriptionId, statIds, verbose ? "INFO" : "OFF");
            Util.throwIfNotOk(resp.getStatus());

            StatisticData data = resp.getData();
            return data.getStatistics();
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

    private StatisticValue getValue(Map map) {
        StatisticValue value = new StatisticValue();
        value.setStatisticId(safeCast(map.get("statisticId"),String.class));
        value.setTimestamp(safeCast(map.get("timestamp"),Long.class));
        value.setName(safeCast(map.get("name"),String.class));
        value.setValue(map.get("value"));
        value.setObjectId(safeCast(map.get("objectId"),String.class));
        value.setObjectType(safeCast(map.get("objectType"),String.class));
        
        return value;
    }

    private <T> T safeCast(Object argument, Class<T> clazz)
    {
        if(clazz.isInstance(argument))
        {
            return (T) argument;
        }
        return null;
    }

    private Map extractData(Map<String, Object> msg)
    {
        final Object data = msg.get("data");
        if(data instanceof Map)
        {
            return (Map) data;
        }
        else return msg;
    }

    private void onValues(Map<String, Object> msg) {
        Map msgData = extractData(msg);
        final String key = "statistics";
        if(msgData.containsKey(key))  {
            List<StatisticValue> list = new ArrayList<>();
            Object[] statistics = safeCast(msgData.get(key), Object[].class);
            if(statistics!=null)
            {
                for (Object obj : statistics)
                {
                    Map map =  safeCast(obj,Map.class);
                    if(map!=null)
                    {
                        StatisticValue value = getValue(map);
                        list.add(value);
                    }
                }
            }
            
            for(StatisticsListener listener: listeners) {
                try {
                    listener.onValues(list);
                }
                catch(Exception ex) {
                    logger.error("Error notifying listener", ex);
                }
            }
        }
        else {
            logger.debug("{}", msg);
            logger.error("Invalid message");            
        }
    }

    private void onServiceChange(Map<String, Object> msg) {
        Map msgData = extractData(msg);
        final String key = "serviceState";
        if(msgData.containsKey(key))  {
            String value = String.valueOf(msgData.get(key));
            ServiceState state = ServiceState.fromString(value);
            if(state == ServiceState.Unknown) {
                logger.warn("Unknown service state: {}", value);
            }
            
            for(StatisticsListener listener: listeners) {
                try {
                    listener.onServiceChange(state);
                }
                catch(Exception ex) {
                    logger.error("", ex);
                }
            }
        }
        else {
            logger.debug("{}", msg);
            logger.error("Invalid message");            
        }
    }
    
    public void addListener(StatisticsListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(StatisticsListener listener) {
        listeners.remove(listener);
    }
    
    public void destroy() throws StatisticsException {
        notifications.disconnect();
    }
}
