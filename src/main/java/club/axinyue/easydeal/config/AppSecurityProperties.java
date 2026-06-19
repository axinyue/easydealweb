package club.axinyue.easydeal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    private String[] allowedOrigins = {"*"};
    private String[] allowedHosts = {};
    private boolean cookieSecure;

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String[] getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(String[] allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }
}
