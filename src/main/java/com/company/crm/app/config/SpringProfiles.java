package com.company.crm.app.config;

import com.company.crm.app.util.constant.CrmConstants;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SpringProfiles {

    private final Environment environment;

    public SpringProfiles(Environment environment) {
        this.environment = environment;
    }

    public boolean isOnlineProfile() {
        return getActiveProfiles().contains(CrmConstants.SpringProfiles.ONLINE);
    }

    public boolean isLocalProfile() {
        return getActiveProfiles().contains(CrmConstants.SpringProfiles.LOCAL);
    }

    public Set<String> getActiveProfiles() {
        return Arrays.stream(environment.getActiveProfiles()).collect(Collectors.toSet());
    }
}
