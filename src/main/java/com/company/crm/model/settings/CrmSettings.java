package com.company.crm.model.settings;

import com.company.crm.model.datatype.PercentDataType;
import io.jmix.appsettings.defaults.AppSettingsDefaultBoolean;
import io.jmix.appsettings.entity.AppSettingsEntity;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
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

    @Min(0)
    @PropertyDatatype(PercentDataType.NAME)
    @Column(name = "DEFAULT_VAT", precision = 19, scale = 2)
    private BigDecimal defaultVatPercent = BigDecimal.valueOf(20);

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
        return getClass().getSimpleName() + "{"
                + "navigationBarTouchOptimized = " + navigationBarTouchOptimized
                + ",\ndefaultVat = " + defaultVatPercent
                + '}';
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

    public BigDecimal getDefaultVatPercent() {
        return defaultVatPercent == null ? BigDecimal.valueOf(20) : defaultVatPercent;
    }

    public void setDefaultVatPercent(BigDecimal defaultVatPercent) {
        this.defaultVatPercent = defaultVatPercent == null ? BigDecimal.ZERO : defaultVatPercent;
    }
}