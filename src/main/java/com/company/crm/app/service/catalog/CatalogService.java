package com.company.crm.app.service.catalog;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import io.jmix.core.FluentValuesLoader;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final CategoryItemRepository categoryItemRepository;

    public CatalogService(CategoryItemRepository categoryItemRepository) {
        this.categoryItemRepository = categoryItemRepository;
    }

    //@formatter:off
    /**
     * Retrieves the top-ordered items based on their quantity,
     * optionally limited to a specific number of items.
     *
     * @param limit         an optional integer specifying the maximum number of items to return.
     *                      If null or not greater than 0, no limit is applied.
     *
     * @param dateRange     an optional parameter specifying the date range for the query.
     *
     * @return              a map where the keys are the top {@link CategoryItem} objects
     *                      and the values are their respective total quantities as {@code BigDecimal}.
     */
    //@formatter:on
    public Map<CategoryItem, BigDecimal> getBestOrderedItems(@Nullable Integer limit,
                                                             @Nullable LocalDateRange dateRange) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder
                .append("select e.categoryItem as categoryItem, sum(e.quantity) as quantity ")
                .append("from OrderItem e ");

        if (dateRange != null) {
            queryBuilder.append("where e.order.date >= :startDate and e.order.date <= :endDate ");
        }

        queryBuilder.append("group by e.categoryItem ")
                .append("order by quantity desc");

        FluentValuesLoader loader = categoryItemRepository
                .fluentValuesLoader(queryBuilder.toString())
                .properties("categoryItem", "quantity")
                .maxResults(limit != null ? limit : 0);

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate())
                    .parameter("endDate", dateRange.endDate());
        }

        return loader.list().stream().collect(Collectors.toMap(
                keyValue -> keyValue.getValue("categoryItem"),
                keyValue -> keyValue.getValue("quantity")
        ));
    }
}
