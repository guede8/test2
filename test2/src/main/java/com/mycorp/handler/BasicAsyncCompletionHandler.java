package com.mycorp.handler;
import com.mycorp.ZendeskUtils;
import com.mycorp.exception.ZendeskException;
import com.ning.http.client.Response;

public class BasicAsyncCompletionHandler<T> extends ZendeskAsyncCompletionHandler<T> {
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
        	ZendeskUtils zendeskUtils = new ZendeskUtils();
        	zendeskUtils.logResponse(response);
        	
//            if (zendeskUtils.isStatus2xx(response)) {
//                if (typeParams.length > 0) {
//                    JavaType type = mapper.getTypeFactory().constructParametricType(clazz, typeParams);
//                    return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), type);
//                }
//                return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), clazz);
//            } else if (isRateLimitResponse(response)) {
//                throw new ZendeskException(response.toString());
//            }
            if (response.getStatusCode() == 404) {
                return null;
            }
            throw new ZendeskException(response.toString());
        }
    }