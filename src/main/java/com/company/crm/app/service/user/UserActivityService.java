package com.company.crm.app.service.user;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.app.util.date.range.OffsetDateTimeRange;
import com.company.crm.model.base.OffsetLimitPageRequest;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.UserActivity;
import com.company.crm.model.user.activity.UserActivityRepository;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.activity.client.ClientUserActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserActivityService {

    private final List<UserActivityRepository<?>> userActivityRepositories;
    private final ClientUserActivityRepository clientUserActivityRepository;

    public UserActivityService(List<UserActivityRepository<?>> userActivityRepositories, ClientUserActivityRepository clientUserActivityRepository) {
        this.userActivityRepositories = userActivityRepositories;
        this.clientUserActivityRepository = clientUserActivityRepository;
    }

    @SuppressWarnings("unchecked")
    public List<? extends UserActivity> loadActivities(OffsetDateTimeRange dateTimeRange, int offset, int limit) {
        return userActivityRepositories.stream()
                .flatMap(repository -> repository.findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanEqual(
                        dateTimeRange.startDate(), dateTimeRange.endDate(),
                        OffsetLimitPageRequest.of(offset, limit)).stream())
                .toList();
    }

    public List<? extends UserActivity> loadActivities(LocalDate date, int offset, int limit) {
        var dateTimeRange = new LocalDateRange(date).asOffsetDateTimeRange();
        return loadActivities(dateTimeRange, offset, limit);
    }

    public List<? extends UserActivity> loadActivities(LocalDate date, int limit) {
        return loadActivities(date, 0, limit);
    }

    public List<? extends UserActivity> loadActivities(User user, OffsetDateTimeRange dateRange, int offset, int limit) {
        return userActivityRepositories.stream()
                .flatMap(repository -> repository.findAllByUserAndCreatedDateGreaterThanEqualAndCreatedDateLessThanEqual(
                        user, dateRange.startDate(), dateRange.endDate(),
                        OffsetLimitPageRequest.of(offset, limit)).stream())
                .toList();
    }

    public List<? extends UserActivity> loadActivities(User user, LocalDate date, int offset, int limit) {
        var dateTimeRange = new LocalDateRange(date).asOffsetDateTimeRange();
        return loadActivities(user, dateTimeRange, offset, limit);
    }

    public List<? extends UserActivity> loadActivities(User user, LocalDate date, int limit) {
        return loadActivities(user, date, 0, limit);
    }

    public List<ClientUserActivity> loadClientActivities(Client client, OffsetDateTimeRange dateRange, int offset, int limit) {
        return clientUserActivityRepository.findAllByClientAndCreatedDateBetween(
                client, dateRange.startDate(), dateRange.endDate(),
                OffsetLimitPageRequest.of(offset, limit));
    }

    public List<ClientUserActivity> loadClientActivities(Client client, LocalDate date, int offset, int limit) {
        var dateTimeRange = new LocalDateRange(date).asOffsetDateTimeRange();
        return loadClientActivities(client, dateTimeRange, offset, limit);
    }

    public List<ClientUserActivity> loadClientActivities(Client client, LocalDate date, int limit) {
        return loadClientActivities(client, date, 0, limit);
    }

    public List<ClientUserActivity> loadClientActivities(User user, Client client, OffsetDateTimeRange dateRange, int offset, int limit) {
        return clientUserActivityRepository.findAllByClientAndUserAndCreatedDateBetween(
                client, user, dateRange.startDate(), dateRange.endDate(),
                OffsetLimitPageRequest.of(offset, limit));
    }

    public List<ClientUserActivity> loadClientActivities(User user, Client client, LocalDate date, int offset, int limit) {
        var dateTimeRange = new LocalDateRange(date).asOffsetDateTimeRange();
        return loadClientActivities(user, client, dateTimeRange, offset, limit);
    }

    public List<ClientUserActivity> loadClientActivities(User user, Client client, LocalDate date, int limit) {
        return loadClientActivities(user, client, date, 0, limit);
    }
}
