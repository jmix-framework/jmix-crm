package com.company.crm.app.util.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.Map;

public final class AppContext {

    public static Object getBean(String name) {
        return context.getBean(name);
    }

    /**
     * Получить экземпляр бина по переданному классу.
     *
     * @throws NoSuchBeanDefinitionException   если не был найден экземпляр Бина
     * @throws NoUniqueBeanDefinitionException если было найдено более одного экземпляра Бина
     * @throws BeansException                  если такого Бина не существует
     */
    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    /**
     * Безопасно получить экземпляр бина по переданному классу.
     * В случае возникновения ошибки возвращает null.
     */
    @Nullable
    public static <T> T getBeanSafely(Class<T> clazz) {
        try {
            return context.getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return context.getBeansOfType(type);
    }

    private static ApplicationContext context;

    static void setContext(ApplicationContext context) {
        AppContext.context = context;
    }
}
