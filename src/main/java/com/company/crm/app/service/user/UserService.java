package com.company.crm.app.service.user;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.model.base.OffsetLimitPageRequest;
import com.company.crm.model.user.UserActivity;
import com.company.crm.model.user.UserActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class UserService {

    private final UserActivityRepository userActivityRepository;

    public UserService(UserActivityRepository userActivityRepository, DateTimeService dateTimeService) {
        this.userActivityRepository = userActivityRepository;
    }

    public List<UserActivity> loadActivities(LocalDate from, LocalDate to, int offset, int limit) {
        OffsetDateTime startOfDay = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return userActivityRepository.findAllByCreatedDateBetween(startOfDay, endOfDay, OffsetLimitPageRequest.of(offset, limit));
    }

    public List<UserActivity> loadActivities(LocalDate from, LocalDate to, int limit) {
        return loadActivities(from, to, 0, limit);
    }

    public List<UserActivity> loadActivities(LocalDate date, int offset, int limit) {
        return loadActivities(date, date, offset, limit);
    }

    public List<UserActivity> loadActivities(LocalDate date, int limit) {
        return loadActivities(date, 0, limit);
    }
}
