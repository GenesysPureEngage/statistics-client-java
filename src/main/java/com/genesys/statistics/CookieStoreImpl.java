package com.genesys.statistics;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

public class CookieStoreImpl implements CookieStore
{
	private final CookieStore manager = new CookieManager().getCookieStore();

	@Override
	public void add(URI uri, HttpCookie cookie)
	{
		manager.add(uri, cookie);
	}

	@Override
	public List<HttpCookie> get(URI uri)
	{
		return manager.get(uri);
	}

	@Override
	public List<HttpCookie> getCookies()
	{
		return manager.getCookies();
	}

	@Override
	public List<URI> getURIs()
	{
		return manager.getURIs();
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie)
	{
		return manager.remove(uri, cookie);
	}

	@Override
	public boolean removeAll()
	{
		return manager.removeAll();
	}
}
