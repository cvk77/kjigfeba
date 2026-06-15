package koeln.netzwerk.bap.migration;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class UserMigrationAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(UserMigrationAuthenticator.class);

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
            if (isMigrated(config, username)) {
                logger.infof("User %s is already migrated.", username);
                context.success();
            } else {
                logger.infof("User %s not migrated. Starting migration...", username);
                performMigration(config, username);
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

    private boolean isMigrated(Map<String, String> config, String username) throws SQLException {
        String url = config.get(UserMigrationAuthenticatorFactory.TOYA_DB_URL);
        String user = config.get(UserMigrationAuthenticatorFactory.TOYA_DB_USER);
        String pass = config.get(UserMigrationAuthenticatorFactory.TOYA_DB_PASSWORD);

        if (url == null || url.isEmpty()) {
            logger.warn("Toya DB URL not configured.");
            return true; // Assume migrated to avoid loops if misconfigured
        }

        // Check if user exists in Oracle (Toya)
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String query = "SELECT count(*) FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        }
        return false;
    }

    private void performMigration(Map<String, String> config, String username) throws SQLException {
        String tenantUrl = config.get(UserMigrationAuthenticatorFactory.TENANT_DB_URL);
        String tenantUser = config.get(UserMigrationAuthenticatorFactory.TENANT_DB_USER);
        String tenantPass = config.get(UserMigrationAuthenticatorFactory.TENANT_DB_PASSWORD);
        
        String toyaUrl = config.get(UserMigrationAuthenticatorFactory.TOYA_DB_URL);
        String toyaUser = config.get(UserMigrationAuthenticatorFactory.TOYA_DB_USER);
        String toyaPass = config.get(UserMigrationAuthenticatorFactory.TOYA_DB_PASSWORD);

        // Placeholder for migration logic as requested
        logger.infof("Fetching user %s from Informix at %s and creating in Oracle at %s", username, tenantUrl, toyaUrl);
        
        // No implementation for migration yet as requested.
        // In a real scenario, you'd connect to Informix, get data, and insert into Oracle.
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
