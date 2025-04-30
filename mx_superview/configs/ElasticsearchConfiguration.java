package com.finsurge.tmr_portal.mx_superview.configs;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.CounterPartyDocument;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.DormantCounterparty;
import com.finsurge.tmr_portal.mx_superview.elastic_search.repository.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

@Configuration
@Slf4j
public class ElasticsearchConfiguration {

    @Autowired Environment environment;

    @Bean
    public RestClientBuilder getRestClientBuilder() {
        String scheme = environment.getProperty("spring.elasticsearch.host.scheme");
        String host =  environment.getProperty("spring.elasticsearch.host.name");
        Integer port = environment.getProperty("spring.elasticsearch.host.port", Integer.class);
        String username =  environment.getProperty("spring.elasticsearch.username");
        String password =  environment.getProperty("spring.elasticsearch.password");
        String caCertPath =  environment.getProperty("spring.elasticsearch.ssl.ca_cert");
        Boolean setNoCaCertDefault = environment.getProperty("spring.elasticsearch.ssl.no_ca_cert.set_default", Boolean.class);
        return getRestClientBuilder(scheme, host, port, username, password, caCertPath, setNoCaCertDefault, this.getClass());
    }

    public static RestClientBuilder getRestClientBuilder(String scheme, String host, Integer port, String username, String password, String caCertPath, Boolean setNoCaCertDefault, Class<?> clazz) {
        if(host == null) {
            host = "localhost";
        }
        if(port == null) {
            port = 9200;
        }

        try(Socket s = new Socket(host, port)) {
            log.info("Can reach elasticsearch server at {}:{}..", host, port);
        } catch (IOException ex) {
            log.error("Cannot reach elasticsearch server at {}:{}..", host, port);
        }
        CredentialsProvider credentialsProvider = null;
        if(username != null && !username.isBlank() && password != null && !password.isBlank()) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        }

        SSLContext context = null;
        if("https".equals(scheme)) {
            scheme = "http";
            try {
                if(caCertPath == null || caCertPath.isBlank()) {
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }

                                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                }

                                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                }
                            }
                    };

                    // Create an SSLContext that uses our TrustManager
                    context = SSLContext.getInstance("TLS");
                    context.init(null, trustAllCerts, null);

                    // You don't have to set this as the default context,
                    // it depends on the library you're using.
                    if(Boolean.TRUE.equals(setNoCaCertDefault)) {
                        SSLContext.setDefault(context);
                    }
                } else {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    Certificate ca;
                    try (InputStream certificateInputStream = clazz.getResourceAsStream(caCertPath)) {
                        ca = cf.generateCertificate(certificateInputStream);
                    }

                    // Create a KeyStore containing our trusted CAs
                    String keyStoreType = KeyStore.getDefaultType();
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("ca", ca);

                    // Create a TrustManager that trusts the CAs in our KeyStore
                    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                    tmf.init(keyStore);

                    // Create an SSLContext that uses our TrustManager
                    context = SSLContext.getInstance("TLS");
                    context.init(null, tmf.getTrustManagers(), null);
                }
                scheme = "https";
            } catch (CertificateException cex) {
                log.error("Cannot configure elasticsearch SSL due to CertificateException. Will try non-SSL method.", cex);
            } catch (NoSuchAlgorithmException aex) {
                log.error("Cannot configure elasticsearch SSL due to NoSuchAlgorithmException. Will try non-SSL method.", aex);
            } catch (KeyManagementException kmx) {
                log.error("Cannot configure elasticsearch SSL due to KeyManagementException. Will try non-SSL method.", kmx);
            } catch (KeyStoreException ksx) {
                log.error("Cannot configure elasticsearch SSL due to KeyStoreException. Will try non-SSL method.", ksx);
            } catch (FileNotFoundException fnx) {
                log.error("Cannot configure elasticsearch SSL due to FileNotFoundException. Will try non-SSL method.", fnx);
            } catch (IOException iox) {
                log.error("Cannot configure elasticsearch SSL due to IOException. Will try non-SSL method.", iox);
            } catch (Exception ex) {
                log.error("Cannot configure elasticsearch SSL due to Exception. Will try non-SSL method.", ex);
            }
        }

        log.info("Connecting to elastic instance at {}://{}@{}:{}", scheme, username, host, port);

        CredentialsProvider finalCredentialsProvider = credentialsProvider;
        SSLContext finalContext = context;
        return RestClient.builder(new HttpHost(host, port, scheme))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectTimeout(5000) // 5 seconds connection timeout
                                .setSocketTimeout(60000))
                .setHttpClientConfigCallback(
                        httpClientBuilder -> {
                            if(finalCredentialsProvider != null) {
                                httpClientBuilder.setDefaultCredentialsProvider(finalCredentialsProvider);
                            }
                            if(finalContext != null) {
                                httpClientBuilder.setSSLContext(finalContext);
                            }
                            return httpClientBuilder;
                        }
                );
    }

    @Bean
    public RestClient getRestClient() {
        RestClient restClient = getRestClientBuilder().build();

        if(restClient.isRunning()) {
            log.info("Yes its runnin..");
        } else {
            log.error("Nope it ain't runnin..");
        }
        return restClient;
    }

    @Bean
    public ElasticsearchTransport getElasticsearchTransport() {
        return getElasticsearchTransportFromRestClient(getRestClient());
    }
    public static ElasticsearchTransport getElasticsearchTransportFromRestClient(RestClient restClient) {
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        mapper.objectMapper().registerModule(new JavaTimeModule());
        return new RestClientTransport(restClient, mapper);
    }

    @Bean
    public ElasticsearchClient getElasticsearchClient() {
        ElasticsearchClient client = new ElasticsearchClient(getElasticsearchTransport());
        return client;
    }

    @Bean
    public ElasticsearchAsyncClient getElasticsearchAsyncClient() {
        ElasticsearchAsyncClient client = new ElasticsearchAsyncClient(getElasticsearchTransport());
        return client;
    }

}
