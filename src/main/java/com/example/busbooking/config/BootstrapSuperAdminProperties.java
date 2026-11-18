package com.example.busbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.bootstrap-super-admin")
public class BootstrapSuperAdminProperties {
    private boolean enabled;
    private String email;
    private String password;
    private String fullName;
    private String phone;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }

    @Override public String toString() { return "BootstrapSuperAdminProperties[REDACTED]"; }
}
