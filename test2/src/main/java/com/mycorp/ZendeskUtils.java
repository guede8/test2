package com.mycorp;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mycorp.bean.Builder;
import com.ning.http.client.Response;

public class ZendeskUtils {
	
	/**
	 * 
	 * @param response
	 * @throws IOException
	 */
	public void logResponse(Response response) throws IOException {
		
		Logger logger = LoggerFactory.getLogger(Zendesk.class);
		
        if (logger.isDebugEnabled()) {
            logger.debug("Response HTTP/{} {}\n{}", response.getStatusCode(), response.getStatusText(),
                    response.getResponseBody());
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Response headers {}", response.getHeaders());
        }
    }
	
	/**
	 * 
	 * @param response
	 * @return
	 */
	public boolean isStatus2xx(Response response) {
        return response.getStatusCode() / 100 == 2;
    }
	
	/**
	 * 
	 * @param builder
	 * @return
	 */
	public Zendesk build(Builder builder) {
		if (builder.getToken() != null) {
			return new Zendesk(builder.getClient(), builder.getUrl(), builder.getUsername() + "/token", builder.getToken());
		}
		return new Zendesk(builder.getClient(), builder.getUrl(), builder.getUsername(), builder.getPassword());
	}
}
