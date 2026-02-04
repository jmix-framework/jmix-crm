package com.company.crm.model.user.activity.userprofile;

import com.company.crm.model.user.activity.UserActivity;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@JmixEntity
@Table(name = "USER_PROFILE_USER_ACITIVTY")
@Entity
public class UserProfileUserActivity extends UserActivity {
}