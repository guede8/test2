package com.mycorp.handler;

import java.io.IOException;

import com.mycorp.exception.ZendeskException;
import com.ning.http.client.AsyncCompletionHandler;

public abstract class ZendeskAsyncCompletionHandler<T> extends AsyncCompletionHandler<T> {
	@Override
	public void onThrowable(Throwable t) {
		if (t instanceof IOException) {
			throw new ZendeskException(t);
		} else {
			super.onThrowable(t);
		}
	}
}