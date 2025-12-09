package com.company.crm.model.user;

import com.company.crm.model.base.FullAuditEntity;
import io.jmix.core.HasTimeZone;
import io.jmix.core.annotation.Secret;
import io.jmix.core.entity.annotation.SystemLevel;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.security.authentication.JmixUserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JmixEntity
@Entity
@Table(name = "USER_", indexes = {
        @Index(name = "IDX_USER__ON_USERNAME", columnList = "USERNAME", unique = true)
})
public class User extends FullAuditEntity implements JmixUserDetails, HasTimeZone {

    @Column(name = "USERNAME", nullable = false)
    private String username;

    @Composition
    @OneToMany(mappedBy = "author")
    private List<UserTask> tasks;

    @Secret
    @SystemLevel
    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Email
    @Column(name = "EMAIL")
    private String email;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @Column(name = "TIME_ZONE_ID")
    private String timeZoneId;

    @Transient
    private Collection<? extends GrantedAuthority> authorities;

    public List<UserTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<UserTask> tasks) {
        this.tasks = tasks;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : Collections.emptyList();
    }

    @Override
    public void setAuthorities(final Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(active);
    }


    @DependsOnProperties({"firstName", "lastName"})
    public String getFullNameName() {
        return String.format("%s %s", (firstName != null ? firstName : ""),
                (lastName != null ? lastName : "")).trim();
    }

    @InstanceName
    @DependsOnProperties({"firstName", "lastName", "username"})
    public String getDisplayName() {
        return String.format("%s [%s]", getFullNameName(), username).trim();
    }

    @Override
    public String getTimeZoneId() {
        return timeZoneId;
    }

    @Override
    public boolean isAutoTimeZone() {
        return true;
    }

    public void setTimeZoneId(final String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }
}