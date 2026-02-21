package com.company.crm.report.dataloader;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.report.mapper.ReportOrderMapper;
import com.company.crm.report.util.ReportDataLoaderUtils;
import io.jmix.core.DataManager;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DataLoader for Orders section of Client360Report.
 * Loads orders for a client within the specified date range.
 */
@Component("ordersReportDataLoader")
public class OrdersReportDataLoader implements ReportDataLoader {

    private final DataManager dataManager;
    private final ReportOrderMapper orderMapper;

    public OrdersReportDataLoader(DataManager dataManager, ReportOrderMapper orderMapper) {
        this.dataManager = dataManager;
        this.orderMapper = orderMapper;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        LocalDateRange dateRange = ReportDataLoaderUtils.getDateRangeFromParams(params);
        UUID clientId = client.getId();

        List<Order> orders = dataManager.load(Order.class)
                .query("SELECT o FROM Order_ o WHERE o.client.id = :clientId " +
                       "AND o.date BETWEEN :fromDate AND :toDate " +
                       "ORDER BY o.date DESC")
                .parameter("clientId", clientId)
                .parameter("fromDate", dateRange.startDate())
                .parameter("toDate", dateRange.endDate())
                .fetchPlanProperties("number", "date", "status", "total", "comment", "discountValue", "discountPercent")
                .list();

        if (orders.isEmpty()) {
            return List.of();
        }

        return orders.stream()
                .map(orderMapper::toReportMap)
                .toList();
    }
}