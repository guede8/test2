package com.mycorp;

import java.io.Closeable;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.exception.ZendeskException;
import com.mycorp.handler.ZendeskAsyncCompletionHandler;
import com.mycorp.support.Ticket;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.uri.Uri;

public class Zendesk implements Closeable {
	private static final String JSON = "application/json; charset=UTF-8";
	private static final Pattern RESTRICTED_PATTERN = Pattern.compile("%2B", Pattern.LITERAL);
	private final boolean closeClient;
	private final AsyncHttpClient client;
	private final Realm realm;
	private final String url;
	private final String oauthToken;
	private final ObjectMapper mapper;
	private final Logger logger;
	private boolean closed = false;

	/**
	 * Genera un objeto Zendesk con los atributos de la clase
	 * 
	 * @return
	 */
	public Zendesk(AsyncHttpClient client, String url, String username, String password) {
		this.logger = LoggerFactory.getLogger(Zendesk.class);
		this.closeClient = client == null;
		this.oauthToken = null;
		this.client = client == null ? new AsyncHttpClient() : client;
		this.url = url.endsWith("/") ? url + "api/v2" : url + "/api/v2";
		if (username != null) {
			this.realm = new Realm.RealmBuilder().setScheme(Realm.AuthScheme.BASIC).setPrincipal(username)
					.setPassword(password).setUsePreemptiveAuth(true).build();
		} else {
			if (password != null) {
				throw new IllegalStateException("Cannot specify token or password without specifying username");
			}
			this.realm = null;
		}
		this.mapper = createMapper();
	}

	public Ticket createTicket(Ticket ticket) {
		return complete(
				submit(req("POST", cnst("/tickets.json"), JSON, json(Collections.singletonMap("ticket", ticket))),
						handle(Ticket.class, "ticket")));
	}

	private byte[] json(Object object) {
		try {
			return mapper.writeValueAsBytes(object);
		} catch (JsonProcessingException e) {
			throw new ZendeskException(e.getMessage(), e);
		}
	}

	private Request req(String method, Uri template, String contentType, byte[] body) {
		RequestBuilder builder = new RequestBuilder(method);
		if (realm != null) {
			builder.setRealm(realm);
		} else {
			builder.addHeader("Authorization", "Bearer " + oauthToken);
		}
		builder.setUrl(RESTRICTED_PATTERN.matcher(template.toString()).replaceAll("+")); // replace
																							// out
																							// %2B
																							// with
																							// +
																							// due
																							// to
																							// API
																							// restriction
		builder.addHeader("Content-type", contentType);
		builder.setBody(body);
		return builder.build();
	}

	public static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper;
	}

	private Uri cnst(String template) {
		return Uri.create(url + template);
	}

	private <T> ListenableFuture<T> submit(Request request, ZendeskAsyncCompletionHandler<T> handler) {
		if (logger.isDebugEnabled()) {
			if (request.getStringData() != null) {
				logger.debug("Request {} {}\n{}", request.getMethod(), request.getUrl(), request.getStringData());
			} else if (request.getByteData() != null) {
				logger.debug("Request {} {} {} {} bytes", request.getMethod(), request.getUrl(),
						request.getHeaders().getFirstValue("Content-type"), request.getByteData().length);
			} else {
				logger.debug("Request {} {}", request.getMethod(), request.getUrl());
			}
		}
		return client.executeRequest(request, handler);
	}

	private boolean isRateLimitResponse(Response response) {
		return response.getStatusCode() == 429;
	}

	protected <T> ZendeskAsyncCompletionHandler<T> handle(final Class<T> clazz, final String name,
			final Class... typeParams) {
		return new BasicAsyncCompletionHandler<T>(clazz, name, typeParams);
	}

	//////////////////////////////////////////////////////////////////////
	// Closeable interface methods
	//////////////////////////////////////////////////////////////////////

	public boolean isClosed() {
		return closed || client.isClosed();
	}

	public void close() {
		if (closeClient && !client.isClosed()) {
			client.close();
		}
		closed = true;
	}

	//////////////////////////////////////////////////////////////////////
	// Static helper methods
	//////////////////////////////////////////////////////////////////////

	private static <T> T complete(ListenableFuture<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new ZendeskException(e.getMessage(), e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ZendeskException) {
				throw (ZendeskException) e.getCause();
			}
			throw new ZendeskException(e.getMessage(), e);
		}
	}

	private class BasicAsyncCompletionHandler<T> extends ZendeskAsyncCompletionHandler<T> {
		private final Class<T> clazz;
		private final String name;
		private final Class[] typeParams;

		public BasicAsyncCompletionHandler(Class clazz, String name, Class... typeParams) {
			this.clazz = clazz;
			this.name = name;
			this.typeParams = typeParams;
		}

		@Override
		public T onCompleted(Response response) throws Exception {
			ZendeskUtils zendeskutils = new ZendeskUtils();
			zendeskutils.logResponse(response);
			if (zendeskutils.isStatus2xx(response)) {
				if (typeParams.length > 0) {
					JavaType type = mapper.getTypeFactory().constructParametricType(clazz, typeParams);
					return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), type);
				}
				return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), clazz);
			} else if (isRateLimitResponse(response)) {
				throw new ZendeskException(response.toString());
			}
			if (response.getStatusCode() == 404) {
				return null;
			}
			throw new ZendeskException(response.toString());
		}
	}

}