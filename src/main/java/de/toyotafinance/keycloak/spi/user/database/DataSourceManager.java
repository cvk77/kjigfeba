package de.toyotafinance.keycloak.spi.user.database;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

/**
 * Thread-safe handling of database pools: One pool for each jdbc url + username-combination.
 * Connection pools are closed automatically after CONNECTION_POOL_TTL to prevent resource leaks.
 *
 * @history	bcvkrue; 27.01.2026; Anlage. DW-8266
 */

@JBossLog
@NoArgsConstructor(access = AccessLevel.NONE)
public class DataSourceManager {

	private static final Duration CONNECTION_POOL_TTL = Duration.ofHours(4);

	private static final Cache<String, AgroalDataSource> dataSources = Caffeine.newBuilder().expireAfterAccess(CONNECTION_POOL_TTL)
			.removalListener((key, ds, cause) -> {
				if (ds != null) {
					log.debugf("Closing connection pool %s", key);
					((AgroalDataSource) ds).close();
				}
			}).build();

	/**
	 * Retrieve an existing or create a new data source.
	 *
	 * @param configMap database configuration map
	 * @return instance of AgroalDataSource
	 * @history	bcvkrue; 27.01.2026; Anlage. DW-8266
	 */
	public static AgroalDataSource getOrCreateDataSource(Map<String, String> configMap) {
		var config = DatabaseConfig.fromMap(configMap);
		return getOrCreateDataSource(config);
	}

	/**
	 * Retrieve an existing or create a new data source.
	 *
	 * @param config   database configuration
	 * @return instance of AgroalDataSource
	 * @history	bcvkrue; 27.01.2026; Anlage. DW-8266
	 */
	public static AgroalDataSource getOrCreateDataSource(DatabaseConfig config) {
		String configId = createCacheKey(config.jdbcUrl(), config.username());
		return dataSources.get(configId, id -> {
			try {
				log.infof("Creating data source %s", configId);
				return createDataSource(config);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to create datasource!", e);
			}
		});
	}

	private static String createCacheKey(String jdbcUrl, String username) {
		return String.format("%s-%s", jdbcUrl, username);
	}

	/**
	 * Create a new data source.
	 *
	 * @param config database configuration
	 * @return AgroalDataSource
	 * @throws SQLException if creation fails
	 * @history	bcvkrue; 27.01.2026; Anlage. DW-8266
	 */
	private static AgroalDataSource createDataSource(DatabaseConfig config) throws SQLException {

		var configuration = new AgroalDataSourceConfigurationSupplier().metricsEnabled(false)
				.connectionPoolConfiguration(
						cp -> cp.maxSize(config.maxPoolSize()).minSize(config.minPoolSize()).initialSize(config.minPoolSize())
								.acquisitionTimeout(config.acquisitionTimeout()).validationTimeout(Duration.ofSeconds(5))
								.connectionFactoryConfiguration(cf -> connectionFactoryConfiguration(cf, config)))
				.get();
		return AgroalDataSource.from(configuration);
	}

	private static AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration(
			AgroalConnectionFactoryConfigurationSupplier cf,
			DatabaseConfig config) {
		cf.jdbcUrl(config.jdbcUrl()).principal(new NamePrincipal(config.username())).autoCommit(true);
		if (config.password() != null && !config.password().isBlank())
			cf.credential(new SimplePassword(config.password()));
		return cf;
	}

	/**
	 * Close a single data source.
	 *
	 * @param configId configuration id
	 * @history	bcvkrue; 27.01.2026; Anlage. DW-8266
	 */
	public static void closeDataSource(String configId) {
		log.debugf("Closing data source for config %s", configId);

		// Triggers cache's removalListener, so no need to manually handle anything here.
		dataSources.invalidate(configId);
	}

	/**
	 * Close all data sources.
	 * @history	bcvkrue; 27.01.2026; Anlage. DW-8266
	 */
	public static void closeAll() {
		dataSources.invalidateAll();
	}

}
