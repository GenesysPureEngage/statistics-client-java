package com.genesys.statistics;

import com.genesys.internal.common.ApiClient;
import com.genesys.internal.common.ApiException;
import com.genesys.internal.statistics.api.StatisticsApi;
import com.genesys.internal.statistics.model.ModelApiResponse;
import com.genesys.internal.statistics.model.PeekedStatistic;
import com.genesys.internal.statistics.model.PeekedStatisticResponse;
import com.genesys.internal.statistics.model.PeekedStatisticValue;
import com.genesys.internal.statistics.model.PeekedStatisticsResponse;
import com.genesys.internal.statistics.model.StatisticData;
import com.genesys.internal.statistics.model.StatisticDataResponse;
import com.genesys.internal.statistics.model.StatisticValue;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    public CompletableFuture<Void> initialize(String token) {
        
        final CompletableFuture<Void> future = new CompletableFuture<>();
        
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

                        future.complete(null);
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
            notifications.subscribe("/statistics/v3/values", new Notifications.NotificationListener() {
                @Override
                public void onNotification(String channel, Map<String, Object> data) {
                    onValues(data);
                }
            });
            notifications.initialize(serviceUrl + "/notifications", apiKey, token);
        }
        catch(Exception ex) {
            future.completeExceptionally(ex);
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
            
            StatisticDataResponse resp = api.createSubscriptionUsingPOST(body, verbose? "INFO": "OFF");
            Util.throwIfNotOk(resp.getStatus());
            
            return resp.getData();
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot create subscription", ex);
        }
    }

    public void deleteSubscription(String id) throws StatisticsException {
        try {
            ModelApiResponse resp = api.deleteSubscription(id);
            Util.throwIfNotOk(resp.getStatus());            
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot delete subscription", ex);
        }
    }

    public PeekedStatisticValue getStatValue(String statisticName, String objectId, String objectType) throws StatisticsException {
        try {
            PeekedStatisticResponse resp = api.getStatValue(statisticName, objectId, objectType);
            Util.throwIfNotOk(resp.getStatus());
            
            PeekedStatistic data = resp.getData();
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
            
            PeekedStatisticsResponse resp = api.getStatValues(body);
            Util.throwIfNotOk(resp.getStatus());
            
            return resp.getData().getStatistics();
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

    public List<StatisticValue> peekSubscriptionStats(String subscriptionId, String[] statisticIds, boolean verbose) throws StatisticsException {
        try {
            String list = Arrays.stream(statisticIds).collect(Collectors.joining(","));
            StatisticDataResponse resp = api.peekSubscriptionStats(subscriptionId, list, verbose? "INFO": "OFF");
            Util.throwIfNotOk(resp.getStatus());
            
            StatisticData data = resp.getData();
            return data.getStatistics();
        }
        catch(ApiException ex) {
            throw new StatisticsException("Cannot peek subscription statistics", ex);
        }
    }
    
    private StatisticValue getValue(Map<String, Object> map) {
        StatisticValue v = new StatisticValue();
        v.setStatisticId((String)map.get("statisticId"));
        v.setTimestamp((Long)map.get("timestamp"));
        v.setName((String)map.get("name"));
        v.setValue(map.get("value"));
        v.setObjectId((String)map.get("objectId"));
        v.setObjectType((String)map.get("objectType"));
        
        return v;
    }

    private void onValues(Map<String, Object> msg) {
        final String key = "serviceState";
        if(msg.containsKey(key))  {
            List<StatisticValue> list = new ArrayList<>();
            Object[] data = (Object[]) msg.get(key);
            for(Object obj: data) {
                Map<String, Object> map = (Map<String,Object>)obj;
                StatisticValue v = getValue(map);
                list.add(v);
            }
            
            for(StatisticsListener l: listeners) {
                try {
                    l.onValues(list);
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

    private void onServiceChange(Map<String, Object> msg) {
        final String key = "serviceState";
        if(msg.containsKey(key))  {
            String value = (String)msg.get(key);
            ServiceState state = ServiceState.fromString(value);
            if(state == ServiceState.Unknown) {
                logger.warn("Unknown service state: {}", value);
            }
            
            for(StatisticsListener l: listeners) {
                try {
                    l.onServiceChange(state);
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
