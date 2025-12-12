package com.company.crm.app.service.catalog;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.app.util.date.range.OffsetDateTimeRange;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import io.jmix.core.FluentValuesLoader;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CatalogService {

    private final CategoryItemRepository categoryItemRepository;

    public CatalogService(CategoryItemRepository categoryItemRepository) {
        this.categoryItemRepository = categoryItemRepository;
    }

    /**
     * Retrieves the top ordered items based on their quantity, optionally limited to a specific number of items.
     *
     * @param amount    an optional integer specifying the maximum number of items to return.
     *                  If null or not greater than 0, no limit is applied.
     *
     * @param dateRange an optional parameter specifying the date range for the query.
     *
     * @return          a map where the keys are the top {@link CategoryItem} objects
     *                  and the values are their respective total quantities as {@code BigDecimal}.
     */
    public Map<CategoryItem, BigDecimal> getBestItems(@Nullable Integer amount,
                                                      @Nullable LocalDateRange dateRange) {
        Map<CategoryItem, BigDecimal> orderItem2Amount = new LinkedHashMap<>();

        FluentValuesLoader loader = categoryItemRepository
                .fluentValuesLoader(buildBestItemsQuery(dateRange))
                .properties("categoryItem", "quantity");

        if (dateRange != null) {
            OffsetDateTimeRange offsetRange = dateRange.asOffsetDateTimeRange();
            loader.parameter("startDate", offsetRange.startDate())
                    .parameter("endDate", offsetRange.endDate());
        }

        if (amount != null) {
            loader.maxResults(amount);
        }

        loader.list().forEach(keyValue -> {
            CategoryItem orderItem = keyValue.getValue("categoryItem");
            BigDecimal total = keyValue.getValue("quantity");
            orderItem2Amount.merge(orderItem, total, BigDecimal::add);
        });

        return orderItem2Amount;
    }

    private String buildBestItemsQuery(@Nullable LocalDateRange dateRange) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder
                .append("select e.categoryItem as categoryItem, sum(e.quantity) as quantity ")
                .append("from OrderItem e ");

        if (dateRange != null) {
            queryBuilder.append("where e.createdDate >= :startDate and e.createdDate <= :endDate ");
        }

        queryBuilder.append("group by e.categoryItem ")
                .append("order by quantity desc");

        return queryBuilder.toString();
    }
}
