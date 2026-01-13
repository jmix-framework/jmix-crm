package com.company.crm.model.base;

import io.jmix.core.FetchPlan;
import io.jmix.core.FluentLoader;
import io.jmix.core.FluentValueLoader;
import io.jmix.core.FluentValuesLoader;
import io.jmix.core.repository.JmixDataRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface UuidEntityRepository<T extends UuidEntity> extends JmixDataRepository<T, UUID> {

    default List<T> listByQuery(String query, Object... params) {
        return fluentLoader().query(query, params).list();
    }

    default Optional<T> optionalByQuery(String query, Object... params) {
        return queryLoader(query, params).maxResults(1).optional();
    }

    default T oneByQuery(String query, Object... params) {
        return queryLoader(query, params).maxResults(1).one();
    }

    default <V> Optional<V> loadOptionalValue(String queryString, Class<V> valueClass) {
        return fluentValueLoader(queryString, valueClass).optional();
    }

    default List<T> findAll(long offset, long limit, @Nullable FetchPlan fetchPlan) {
        return findAll(OffsetLimitPageRequest.of(offset, limit), fetchPlan).getContent();
    }

    default List<T> findAll(long offset, long limit) {
        return findAll(offset, limit, null);
    }

    default List<T> findAll(long limit, @Nullable FetchPlan fetchPlan) {
        return findAll(0, limit, fetchPlan);
    }

    default List<T> findAll(long limit) {
        return findAll(limit, null);
    }

    // ----- utils -----

    default FluentLoader.ByQuery<T> queryLoader(String query, Object... params) {
        return fluentLoader().query(query, params);
    }

    default FluentLoader<T> fluentLoader() {
        return getDataManager().load(getEntityClass());
    }

    default FluentValuesLoader fluentValuesLoader(String query) {
        return getDataManager().loadValues(query);
    }

    default <V> FluentValueLoader<V> fluentValueLoader(String query, Class<V> valueClass) {
        return getDataManager().loadValue(query, valueClass);
    }

    default Class<T> getEntityClass() {
        Type[] interfaces = getClass().getInterfaces();
        for (Type t : interfaces) {
            if (t instanceof Class<?> clazz) {
                Type genericInterface = clazz.getGenericInterfaces()[0];
                //noinspection unchecked
                return (Class<T>) ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
            }
        }

        throw new IllegalStateException("Can not resolve entity class from repository interface");
    }
}