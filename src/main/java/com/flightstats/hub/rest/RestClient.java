package com.flightstats.hub.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;


public class RestClient {

    private final static Logger logger = LoggerFactory.getLogger(RestClient.class);
    private final static Client client = RestClient.createClient(15, 60, true, false);
    private final static Client gzipClient = RestClient.createClient(15, 60, true, true);
    private final static Client noRedirect = RestClient.createClient(15, 60, false, false);

    public static Client defaultClient() {
        return client;
    }

    public static Client noRedirectClient() {
        return noRedirect;
    }

    public static Client gzipClient() {
        return gzipClient;
    }

    public static Client createClient(int connectTimeout, int readTimeout, boolean followRedirects, boolean gzip) {
        try {
            TrustManager[] certs = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }
                    }
            };
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, certs, new SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

//            ClientConfig config = new DefaultClientConfig();
//            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
//                    new HTTPSProperties((hostname, session) -> true, ctx));

            HttpClient apacheClient = HttpClientBuilder.create().setSSLContext(ctx).build();
            Client client = new Client(new ApacheHttpClient4Handler(apacheClient, new BasicCookieStore(), true));

//            Client client = Client.create(config);
            client.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectTimeout));
            client.setReadTimeout((int) TimeUnit.SECONDS.toMillis(readTimeout));
            client.setFollowRedirects(followRedirects);
            if (gzip) {
                client.addFilter(new GZIPContentEncodingFilter());
            }
            return client;
        } catch (Exception e) {
            logger.warn("can't create client ", e);
            throw new RuntimeException(e);
        }
    }
}
