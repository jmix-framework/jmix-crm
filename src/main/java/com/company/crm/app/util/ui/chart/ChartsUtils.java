package com.company.crm.app.util.ui.chart;

import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.Unit;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.Label;
import io.jmix.chartsflowui.kit.component.model.series.LineSeries;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.component.model.shared.FontStyle;
import io.jmix.chartsflowui.kit.component.model.shared.Orientation;
import io.jmix.chartsflowui.kit.component.model.toolbox.MagicTypeFeature;
import io.jmix.chartsflowui.kit.component.model.toolbox.SaveAsImageFeature;
import io.jmix.chartsflowui.kit.component.model.toolbox.Toolbox;
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

    public Chart createViewStatPieChart(String title) {
        return createViewStatChart(title, SeriesType.PIE);
    }

    public Chart createViewStatChart(String title, SeriesType seriesType) {
        Chart chart = uiComponents.create(Chart.class)
                .withLegend(new Legend()
                        .withTop("20")
                        .withLeft("0")
                        .withOrientation(Orientation.VERTICAL))
                .withTooltip(new Tooltip()
                        .withShow(true))
                .withToolbox(new Toolbox()
                        .withShow(true)
                        .withFeatures(
                                new SaveAsImageFeature()
                                        .withType(SaveAsImageFeature.SaveType.PNG)))
                .withTitle(new Title()
                        .withText(title)
                        .withTextStyle(new Title.TextStyle()
                                .withFontSize(12)
                                .withFontStyle(FontStyle.NORMAL)))
                .withGrid(new Grid()
                        .withWidth("100%")
                        .withShow(false));

        switch (seriesType) {
            case BAR -> chart.withSeries(
                            new BarSeries()
                                    .withAnimation(true)
                                    .withLabel(new Label().withShow(false))
                                    .withYAxisIndex(0)
                                    .withXAxisIndex(0))
                    .withXAxis(new XAxis().withType(AxisType.CATEGORY).withInterval(1))
                    .withYAxis(new YAxis().withType(AxisType.VALUE).withInterval(1));

            case LINE -> chart.withSeries(
                            new LineSeries()
                                    .withAnimation(true)
                                    .withLabel(new Label().withShow(false))
                                    .withYAxisIndex(0)
                                    .withXAxisIndex(0))
                    .withXAxis(new XAxis().withType(AxisType.CATEGORY).withInterval(1))
                    .withYAxis(new YAxis().withType(AxisType.VALUE).withInterval(1));

            case PIE -> chart.withSeries(
                    new PieSeries()
                            .withLabel(new Label().withShow(false))
                            .withAnimation(true));

            default -> throw new IllegalArgumentException("SeriesType not supported");
        }

        applyDefaultChartSettings(chart);

        return chart;
    }

    private void applyDefaultChartSettings(Chart chart) {
        chart.setWidthFull();
        chart.setHeight(12, Unit.EM);
        chart.setMinWidth(20, Unit.EM);
    }
}
