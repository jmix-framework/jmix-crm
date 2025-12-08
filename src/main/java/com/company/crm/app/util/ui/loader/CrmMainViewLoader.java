package com.company.crm.app.util.ui.loader;

import com.company.crm.model.settings.CrmSettings;
import io.jmix.appsettings.AppSettings;
import io.jmix.flowui.xml.layout.loader.MainViewLoader;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CrmMainViewLoader extends MainViewLoader {
    @Override
    protected Element getAppLayoutElement() {
        AppSettings appSettings = applicationContext.getBean(AppSettings.class);
        Boolean touchOptimized = appSettings.load(CrmSettings.class).getNavigationBarTouchOptimized();
        Element appLayoutElement = super.getAppLayoutElement();
        appLayoutElement.element("navigationBar")
                .addAttribute("touchOptimized", touchOptimized.toString());
        return appLayoutElement;
    }
}
