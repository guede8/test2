package com.mycorp.bean;

import com.mycorp.Zendesk;
import com.ning.http.client.AsyncHttpClient;

public class Builder {
	
	private final String url;
	private AsyncHttpClient client = null;
	private String username = null;
	private String password = null;
	private String token = null;
	private String oauthToken = null;

	public Builder(String url) {
		this.url = url;
	}

	public AsyncHttpClient getClient() {
		return client;
	}

	public void setClient(AsyncHttpClient client) {
		this.client = client;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getOauthToken() {
		return oauthToken;
	}

	public void setOauthToken(String oauthToken) {
		this.oauthToken = oauthToken;
	}

	public String getUrl() {
		return url;
	}

	public String getPassword() {
		return password;
	}

	public String getToken() {
		return token;
	}

	public void setPassword(String password) {
		this.password = password;
		if (password != null) {
			setToken(null);
			setOauthToken(null);
		}
	}

	public void setToken(String token) {
		this.token = token;
		if (token != null) {
			setPassword(null);
			setOauthToken(null);
		}
	}

	public Zendesk build() {
		if (token != null) {
			return new Zendesk(client, url, username + "/token", token);
		}
		return new Zendesk(client, url, username, password);
	}
}