package com.company.crm.app.util.ui.component.chart;

import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.Unit;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.shared.FontStyle;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class ChartsUtils {
    private static final Logger log = LoggerFactory.getLogger(ChartsUtils.class);
    private final UiAsyncTasks uiAsyncTasks;
    private final UiComponents uiComponents;

    public ChartsUtils(UiAsyncTasks uiAsyncTasks, UiComponents uiComponents) {
        this.uiAsyncTasks = uiAsyncTasks;
        this.uiComponents = uiComponents;
    }

    public CompletableFuture<?> initializeChartsAsync(Map<Chart, Supplier<DataSet>> chartsLoaders) {
        return uiAsyncTasks.supplierConfigurer(() -> {
                    var chartDataSetters = new ArrayList<Runnable>();
                    chartsLoaders.forEach((chart, dataSetLoader) -> {
                        DataSet dataSet = dataSetLoader.get();
                        chartDataSetters.add(() -> {
                            chart.withDataSet(dataSet);
                            SkeletonStyler.remove(chart);
                        });
                    });
                    return chartDataSetters;
                })
                .withTimeout(5, TimeUnit.SECONDS)
                .withResultHandler(r -> r.forEach(Runnable::run))
                .withExceptionHandler(e -> {
                    log.error(e.getMessage(), e);
                    SkeletonStyler.remove(chartsLoaders.keySet());
                })
                .supplyAsync();
    }

    public Chart createDefaulListViewTopChart(String title) {
        Chart chart = uiComponents.create(Chart.class)
                .withTooltip(new Tooltip())
                .withSeries(new PieSeries()
                        .withAnimation(true))
                .withTitle(new Title()
                        .withText(title)
                        .withBottom("0%")
                        .withRight(title.length() >= 10 ? "0" : "10%")
                        .withTextAlign(Title.TextAlign.CENTER)
                        .withTextVerticalAlign(Title.TextVerticalAlign.BOTTOM)
                        .withTextStyle(new Title.TextStyle()
                                .withFontSize(12)
                                .withFontStyle(FontStyle.NORMAL)))
                .withGrid(new Grid()
                        .withWidth("100%")
                        .withShow(false));

        applyDefaultChartSettings(chart);

        return chart;
    }

    private void applyDefaultChartSettings(Chart chart) {
        chart.setWidthFull();
        chart.setHeight(12, Unit.EM);
        chart.setMinWidth(20, Unit.EM);
    }
}
