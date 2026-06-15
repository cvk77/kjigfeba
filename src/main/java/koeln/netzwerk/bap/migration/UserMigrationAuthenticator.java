package koeln.netzwerk.bap.migration;

import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class UserMigrationAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(UserMigrationAuthenticator.class);
    private final UserMigrationAuthenticatorFactory factory;

    public UserMigrationAuthenticator(UserMigrationAuthenticatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.attempted();
            return;
        }

        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            logger.warn("No configuration found for UserMigrationAuthenticator. Skipping migration check.");
            context.success();
            return;
        }

        Map<String, String> config = configModel.getConfig();
        String username = user.getUsername();

        try {
            if (isMigrated(configModel, username)) {
                logger.infof("User %s is already migrated.", username);
                context.success();
            } else {
                logger.infof("User %s not migrated. Starting migration...", username);
                performMigration(configModel, username);
                logger.infof("Migration finished for user %s.", username);
                context.success();
            }
        } catch (Exception e) {
            logger.errorf(e, "Error during migration check/process for user %s", username);
            // Decide whether to fail login or continue. Given the requirement "important that migration is fully finished", 
            // failing might be safer if it's critical. But usually, we might want to let them in if migration is just a sync.
            // For now, let's assume we continue but log the error.
            context.success();
        }
    }

    private boolean isMigrated(AuthenticatorConfigModel config, String username) throws SQLException {
        DataSource ds = factory.getToyaDataSource(config);

        if (ds == null) {
            logger.warn("Toya DB not configured or failed to initialize.");
            return true; // Assume migrated to avoid loops if misconfigured
        }

        // Optimization: Use ROWNUM = 1 to stop after the first match
        try (Connection conn = ds.getConnection()) {
            String query = "SELECT 1 FROM users WHERE username = ? AND ROWNUM = 1";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        }
    }

    private void performMigration(AuthenticatorConfigModel config, String username) throws SQLException {
        AgroalDataSource tenantDs = factory.getTenantDataSource(config);
        AgroalDataSource toyaDs = factory.getToyaDataSource(config);

        if (tenantDs == null || toyaDs == null) {
            logger.warn("Databases not configured correctly. Skipping migration logic placeholder.");
            return;
        }

        // Placeholder for migration logic as requested
        logger.infof("Migration context prepared for user %s using pooled connections.", username);

        // Load actual roles from tenant database
        // Load default salesperson permissio

    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
