package com.company.crm.app.util.ui.loader;

import io.jmix.flowui.xml.layout.ComponentLoader;
import io.jmix.flowui.xml.layout.LayoutLoaderConfig;
import org.dom4j.Element;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static io.jmix.flowui.xml.layout.loader.MainViewLoader.MAIN_VIEW_ROOT;

@Primary
@Component
public class CrmLayoutLoaderConfig extends LayoutLoaderConfig {

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends ComponentLoader> getViewLoader(Element root) {
        if (MAIN_VIEW_ROOT.equals(root.getName())) {
            return CrmMainViewLoader.class;
        } else {
            return super.getViewLoader(root);
        }
    }
}
