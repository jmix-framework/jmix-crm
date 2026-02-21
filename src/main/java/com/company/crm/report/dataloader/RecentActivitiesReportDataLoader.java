package com.company.crm.report.dataloader;

import com.company.crm.app.service.user.UserActivityService;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.report.config.ClientReportThresholds;
import com.company.crm.report.mapper.ReportActivityMapper;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DataLoader for Recent Activities section of Client360Report.
 * Loads recent user activities for a client using configurable thresholds.
 */
@Component("recentActivitiesReportDataLoader")
public class RecentActivitiesReportDataLoader implements ReportDataLoader {

    private final UserActivityService userActivityService;
    private final ReportActivityMapper activityMapper;

    public RecentActivitiesReportDataLoader(UserActivityService userActivityService, ReportActivityMapper activityMapper) {
        this.userActivityService = userActivityService;
        this.activityMapper = activityMapper;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        if (client == null) {
            return List.of();
        }

        // Use UserActivityService like the DetailView does - load activities for recent days
        List<ClientUserActivity> activities = new ArrayList<>();

        // Load activities for the last N days using centralized thresholds
        LocalDate currentDate = LocalDate.now();
        for (int i = 0; i < ClientReportThresholds.RECENT_ACTIVITIES_DAYS_RANGE; i++) {
            LocalDate dayToCheck = currentDate.minusDays(i);

            List<ClientUserActivity> dayActivities = userActivityService.loadClientActivities(
                    client, dayToCheck, ClientReportThresholds.ACTIVITIES_PER_DAY);

            activities.addAll(dayActivities);

            // Stop if we have enough
            if (activities.size() >= ClientReportThresholds.MAX_RECENT_ACTIVITIES) {
                break;
            }
        }

        if (activities.isEmpty()) {
            return List.of(); // Return empty list if no activities
        }

        return activities.stream()
                .map(activityMapper::toReportMap)
                .filter(fields -> fields.get("description") != null && !fields.get("description").toString().trim().isEmpty())
                .toList();
    }
}