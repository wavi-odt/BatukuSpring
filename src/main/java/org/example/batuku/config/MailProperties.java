package org.example.batuku.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "batuku.mail")
public class MailProperties {

    private String provider = "smtp";
    private String fromEmail = "batuku.suporte@gmail.com";
    private String fromName = "Batuku";
    private Brevo brevo = new Brevo();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public Brevo getBrevo() { return brevo; }
    public void setBrevo(Brevo brevo) { this.brevo = brevo; }

    public static class Brevo {
        private String apiKey = "";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
}
