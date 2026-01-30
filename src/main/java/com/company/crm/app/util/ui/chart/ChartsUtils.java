package com.company.crm.app.util.ui.chart;

import com.company.crm.app.ui.component.card.CrmCard;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.legend.ScrollableLegend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.Label;
import io.jmix.chartsflowui.kit.component.model.series.LineSeries;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.component.model.shared.FontStyle;
import io.jmix.chartsflowui.kit.component.model.shared.Orientation;
import io.jmix.chartsflowui.kit.component.model.toolbox.SaveAsImageFeature;
import io.jmix.chartsflowui.kit.component.model.toolbox.Toolbox;
import io.jmix.flowui.UiComponents;

import static com.company.crm.app.util.ui.CrmUiUtils.setBackgroundTransparent;

@SpringComponent
public class ChartsUtils {

    private final UiComponents uiComponents;

    public ChartsUtils(UiComponents uiComponents) {
        this.uiComponents = uiComponents;
    }

    public CrmCard createViewStatChartWrapper(Chart chart) {
        return createViewStatChartWrapper(chart, chart.getDataSet() == null);
    }

    public CrmCard createViewStatChartWrapper(Chart chart, boolean applySkeleton) {
        var flexContainer = new VerticalLayout(chart);
        flexContainer.addClassNames(LumoUtility.Flex.GROW);
        flexContainer.setPadding(false);

        CrmCard card = uiComponents.create(CrmCard.class);
        card.setWidthFull();
        card.add(flexContainer);

        if (applySkeleton) {
            SkeletonStyler.apply(chart);
        }

        return card;
    }

    public Chart createViewStatChart(String title, SeriesType seriesType) {
        Chart chart = uiComponents.create(Chart.class)
                .withLegend(new ScrollableLegend()
                        .withHeight("100")
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
        setBackgroundTransparent(chart);
    }
}
