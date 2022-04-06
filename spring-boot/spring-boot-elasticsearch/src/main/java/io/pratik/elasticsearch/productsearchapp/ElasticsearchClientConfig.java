/**
 * 
 */
package io.pratik.elasticsearch.productsearchapp;

import com.nebhale.bindings.Binding;
import com.nebhale.bindings.Bindings;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;


/**
 * @author Pratik Das
 *
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "io.pratik.elasticsearch.repositories")
@ComponentScan(basePackages = { "io.pratik.elasticsearch" })
public class ElasticsearchClientConfig extends AbstractElasticsearchConfiguration {

	Logger log = LoggerFactory.getLogger(ElasticsearchClientConfig.class);

	@Override
	@Bean
	public RestHighLevelClient elasticsearchClient() {
		if (System.getenv("SERVICE_BINDING_ROOT") == null) {
			log.error("SERVICE_BINDING_ROOT environment variable is not set");
			return null;
		}
		final Binding[] bindings = Bindings.filter(Bindings.fromServiceBindingRoot(), "elasticsearch");

		if (bindings.length != 1) {
			log.error("Unable to find 'elasticsearch` binding under SERVICE_BINDING_ROOT=%s", System.getenv("SERVICE_BINDING_ROOT"));
			return null;
		}
		final Binding config = bindings[0];
		final String uri = config.get("uri");
		final String username = config.get("username");
		final String password = config.get("password");

		SSLContext sslContext = null;

		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
			log.error("Unable to setup SSL Context", e);
		}

		final ClientConfiguration clientConfiguration = 
				ClientConfiguration
				.builder()
				.connectedTo(uri)
				.usingSsl(sslContext, NoopHostnameVerifier.INSTANCE)
				.withBasicAuth(username, password)
				.build();

		return RestClients
				.create(clientConfiguration)
				.rest();
	}
}
