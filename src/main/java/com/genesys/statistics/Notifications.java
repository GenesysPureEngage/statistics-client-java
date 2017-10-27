package com.genesys.statistics;

import java.net.CookieManager;
import java.net.CookieStore;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notifications {
    private static final Logger logger = LoggerFactory.getLogger(Notifications.class);

    public static interface NotificationListener {
        void onNotification(String channel, Map<String,Object> data);
    }

    private BayeuxClient client;
    private HttpClient httpClient;

    private final Map<String, Collection<NotificationListener>> listeners = new ConcurrentHashMap<>();
    private CookieStore cookieStore = new CookieManager().getCookieStore();
    
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    private void onHandshake(Message msg) {
        if(msg.isSuccessful()) {
            logger.debug("Handshake successful.");
            
            logger.debug("Subscribing to channels...");            
            for (Map.Entry<String, Collection<NotificationListener>> entry : listeners.entrySet()) {
                final String name = entry.getKey();
                final Collection<NotificationListener> list = entry.getValue();
                
                client.getChannel(name).subscribe(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        Map<String, Object> data = message.getDataAsMap();
                        for (NotificationListener l : list) {
                            l.onNotification(name, data);
                        }
                    }
                }, new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        String subscription = (String) message.get("subscription");
                        if(message.isSuccessful()) {
                            logger.debug("Successfuly subscribed to channel: {}", subscription);
                        }
                        else {
                            logger.error("Cannot subscribe to channel: {}", subscription);
                        }
                    }
                });
            }
        }
        else {
            logger.debug("{}", msg);
            logger.error("Handshake failed");
        }
    }
    
    public void initialize(String endpoint, final String apiKey, final String token) throws StatisticsException {
        
        try {
            httpClient = new HttpClient(new SslContextFactory());
            httpClient.start();
            client = new BayeuxClient(endpoint, new ClientTransportImpl(apiKey, token, httpClient) {
                @Override
                protected CookieStore getCookieStore() {
                    return cookieStore;
                }
            });
            
            initialize(client);
        }
        catch(Exception ex) {
            throw new StatisticsException("Initialization failed.", ex);
        }
    }
    
    void initialize(BayeuxClient client) {
        this.client = client;
        
        logger.debug("Starting cometd handshake...");
        client.handshake(new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                onHandshake(message);
            }
        });
    }
    
    public void disconnect() throws StatisticsException  {
        try {
            if(client != null) {
                client.disconnect();
            }
            if(httpClient != null) {
                httpClient.stop();
            }
        }
        catch (Exception ex) {
            throw new StatisticsException("Cannot disconnect", ex);
        }
    }
    
    public void subscribe(String channelName, NotificationListener listener) {
        listeners.computeIfAbsent(channelName, new Function<String, Collection<NotificationListener>>() {
            @Override
            public Collection<NotificationListener> apply(String t) {
                return new ConcurrentLinkedQueue<>();
            }
        }).add(listener);
    }
}
