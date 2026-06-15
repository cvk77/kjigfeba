package koeln.netzwerk.bap.migration;

import de.toyotafinance.keycloak.spi.user.database.DataSourceManager;
import de.toyotafinance.keycloak.spi.user.database.DatabaseConfig;
import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserMigrationAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "user-migration-authenticator";

    private static final Logger logger = Logger.getLogger(UserMigrationAuthenticatorFactory.class);
    
    public static final String TENANT_DB_URL = "tenantDbUrl";
    public static final String TENANT_DB_USER = "tenantDbUser";
    public static final String TENANT_DB_PASSWORD = "tenantDbPassword";
    
    public static final String TOYA_DB_URL = "toyaDbUrl";
    public static final String TOYA_DB_USER = "toyaDbUser";
    public static final String TOYA_DB_PASSWORD = "toyaDbPassword";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        // Tenant DB (Informix)
        property = new ProviderConfigProperty();
        property.setName(TENANT_DB_URL);
        property.setLabel("Tenant DB (Informix) URL");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("JDBC URL for the legacy Informix database");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TENANT_DB_USER);
        property.setLabel("Tenant DB User");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TENANT_DB_PASSWORD);
        property.setLabel("Tenant DB Password");
        property.setType(ProviderConfigProperty.PASSWORD);
        configProperties.add(property);

        // Toya DB (Oracle)
        property = new ProviderConfigProperty();
        property.setName(TOYA_DB_URL);
        property.setLabel("Toya DB (Oracle) URL");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("JDBC URL for the new Oracle database");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TOYA_DB_USER);
        property.setLabel("Toya DB User");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TOYA_DB_PASSWORD);
        property.setLabel("Toya DB Password");
        property.setType(ProviderConfigProperty.PASSWORD);
        configProperties.add(property);
    }

    @Override
    public String getDisplayType() {
        return "Legacy User Migration Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "migration";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Migrates users from legacy Informix database to Oracle database before login.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new UserMigrationAuthenticator(this);
    }

    public AgroalDataSource getTenantDataSource(AuthenticatorConfigModel config) {
        return getDataSource(config, TENANT_DB_URL, TENANT_DB_USER, TENANT_DB_PASSWORD);
    }

    public AgroalDataSource getToyaDataSource(AuthenticatorConfigModel config) {
        return getDataSource(config, TOYA_DB_URL, TOYA_DB_USER, TOYA_DB_PASSWORD);
    }

    private AgroalDataSource getDataSource(AuthenticatorConfigModel configModel, String urlKey, String userKey, String passKey) {
        Map<String, String> config = configModel.getConfig();
        if (config.get(urlKey) == null || config.get(urlKey).isEmpty()) {
            return null;
        }
        DatabaseConfig dbConfig = DatabaseConfig.fromMap(config, urlKey, userKey, passKey);
        return DataSourceManager.getOrCreateDataSource(dbConfig);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        logger.info("Closing all Agroal data sources via DataSourceManager");
        DataSourceManager.closeAll();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
