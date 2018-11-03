package com.genesys.statistics;

import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClientTransportImpl extends LongPollingTransport
{
	private static final Logger logger = LoggerFactory.getLogger(ClientTransportImpl.class);

	private final TraceInterceptor interceptor = new TraceInterceptor();
	private final String apiKey;
	private final String token;

	public ClientTransportImpl(final String apiKey, final String token, final HttpClient httpClient)
	{
		this(apiKey, token, httpClient, null);
	}

	public ClientTransportImpl(final String apiKey, final String token, final HttpClient httpClient, final Map<String, Object> options)
	{
		super(options != null ? new HashMap<>(options) : new HashMap<String, Object>(), httpClient);
		this.apiKey = apiKey;
		this.token = token;
	}

	@Override
	protected void customize(Request request)
	{
		request.header("x-api-key", apiKey);
		request.header("Authorization", String.format("Bearer %s", token));
		request.header(TraceInterceptor.TRACEID_HEADER, interceptor.makeUniqueId());
		request.header(TraceInterceptor.SPANID_HEADER, interceptor.makeUniqueId());

		logger.debug("{}\n{}", request.toString(), request.getHeaders().toString());

		request.onComplete(new Response.Listener.Adapter()
		{
			@Override
			public void onSuccess(Response response)
			{
				logger.debug("{}\n{}", response, response.getHeaders());
			}
		});
	}
}
