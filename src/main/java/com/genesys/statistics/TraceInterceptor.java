package com.genesys.statistics;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

public class TraceInterceptor implements Interceptor
{
	public static final String TRACEID_HEADER = "X-B3-TraceId";
	public static final String SPANID_HEADER = "X-B3-SnapId";
	private static final Logger logger = LoggerFactory.getLogger(TraceInterceptor.class);
	final Random random = new SecureRandom();
	final int traceIdLength = 8 * 2;   //64-bit ids

	@Override
	public Response intercept(Chain chain) throws IOException
	{
		Request request = chain.request();
		request = request.newBuilder()
			.addHeader(TRACEID_HEADER, makeUniqueId())
			.addHeader(SPANID_HEADER, makeUniqueId()).build();

		return chain.proceed(request);
	}

	public final String makeUniqueId()
	{
		return makeUniqueId(traceIdLength);
	}

	private String makeUniqueId(int len)
	{
		final StringBuilder builder = new StringBuilder();
		while (builder.length() < len)
		{
			builder.append(Integer.toHexString(random.nextInt()));
		}

		return builder.substring(0, len);
	}
}
