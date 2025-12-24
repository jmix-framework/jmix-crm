package com.company.crm.model.settings;

import io.jmix.appsettings.defaults.AppSettingsDefaultBoolean;
import io.jmix.appsettings.entity.AppSettingsEntity;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "CRM_SETTINGS")
@Entity
public class CrmSettings extends AppSettingsEntity {

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @AppSettingsDefaultBoolean(false)
    @Column(name = "NAVIGATION_BAR_TOUCH_OPTIMIZED")
    private Boolean navigationBarTouchOptimized;

    @CreatedDate
    @Column(name = "CREATED_DATE")
    private OffsetDateTime createdDate;

    @LastModifiedBy
    @Column(name = "LAST_MODIFIED_BY")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private OffsetDateTime lastModifiedDate;

    @InstanceName
    public String getInstanceName() {
        return getClass().getSimpleName();
    }

    public Boolean getNavigationBarTouchOptimized() {
        return navigationBarTouchOptimized;
    }

    public void setNavigationBarTouchOptimized(Boolean navigationBarTouchOptimized) {
        this.navigationBarTouchOptimized = navigationBarTouchOptimized;
    }

    public OffsetDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

}