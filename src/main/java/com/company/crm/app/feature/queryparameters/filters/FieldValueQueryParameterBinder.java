package com.company.crm.app.feature.queryparameters.filters;

import com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder;
import com.company.crm.app.util.context.AppContext;
import com.company.crm.app.util.ui.CrmUiUtils;
import com.company.crm.model.base.UuidEntity;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.core.metamodel.datatype.EnumClass;
import io.jmix.flowui.facet.UrlQueryParametersFacet;
import io.jmix.flowui.facet.urlqueryparameters.AbstractUrlQueryParametersBinder;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.navigation.UrlParamSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder.getUrlQueryParametersFacet;
import static com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder.validateId;
import static com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder.validateIds;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;

/**
 * Binder to link the field value and url query parameter.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FieldValueQueryParameterBinder extends AbstractUrlQueryParametersBinder {

    private static final Logger log = LoggerFactory.getLogger(FieldValueQueryParameterBinder.class);
    private static final String QP_PREFIX = "value-for-";

    private final SimpleUrlQueryParametersBinder delegate;

    public static Builder builder(View<?> view) {
        return new Builder(view);
    }

    private FieldValueQueryParameterBinder(Collection<ComponentValueBinder> fields, View<?> view) {
        this(fields, getUrlQueryParametersFacet(view));
    }

    private FieldValueQueryParameterBinder(Collection<ComponentValueBinder> fields, UrlQueryParametersFacet facet) {
        delegate = createDelegate(facet, fields);
    }

    private static <C extends Component & HasValue<?, String>> Collection<ComponentValueBinder> createStringBinders(Collection<C> fields) {
        validateIds(fields);
        return fields.stream()
                .map(field -> new ComponentValueBinder(field, Function.identity(), Function.identity()))
                .toList();
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        delegate.updateState(queryParameters);
    }

    @Override
    public Component getComponent() {
        return null;
    }

    private SimpleUrlQueryParametersBinder createDelegate(UrlQueryParametersFacet facet, Collection<ComponentValueBinder> binders) {
        final SimpleUrlQueryParametersBinder delegate;
        delegate = SimpleUrlQueryParametersBinder.registerBinder(facet,
                () -> {
                    QueryParameters parameters = QueryParameters.empty();
                    for (ComponentValueBinder binder : binders) {
                        String parameterKey = QP_PREFIX + binder.componentId();
                        String parameterValue = binder.serializedValue();
                        // TODO: should we add check for empty value?
                        //  if(isBlank(parameterValue)) {
                        parameters = parameters.merging(parameterKey, parameterValue);
                        // }
                    }
                    return parameters;
                },
                f -> f.getParameters().forEach((key, values) -> {
                    String componentId = substringAfter(key, QP_PREFIX);
                    if (isNotBlank(componentId)) {
                        binders.stream()
                                .filter(c -> c.componentId().equals(componentId))
                                .findFirst()
                                .ifPresent(binder ->
                                        values.stream().findFirst().ifPresent(binder::deserializeValue));
                    }
                })
        );

        addValueChangeListeners(binders, delegate);

        return delegate;
    }

    private void addValueChangeListeners(Collection<ComponentValueBinder> binders, SimpleUrlQueryParametersBinder delegate) {
        binders.forEach(binder -> {
            HasValue<?, ?> component = (HasValue<?, ?>) binder.component();
            component.addValueChangeListener(e -> delegate.fireQueryParametersChanged());
        });
    }

    private record ComponentValueBinder<V, C extends Component & HasValue<?, V>>(C component,
                                                                                 Function<V, String> serializer,
                                                                                 Function<String, V> deserializer) {

        String componentId() {
            return CrmUiUtils.getComponentId(component).orElse("");
        }

        String serializedValue() {
            Optional<V> valueOpt = component.getOptionalValue();
            if (valueOpt.isPresent()) {
                return serializer.apply(valueOpt.get());
            } else if (component.getEmptyValue() instanceof Boolean) {
                return Boolean.FALSE.toString();
            } else {
                return "";
            }
        }

        void deserializeValue(String value) {
            try {
                component.setValue(deserializer.apply(value));
            } catch (Throwable e) {
                log.warn("Error while setting deserializing value to component", e);
            }
        }
    }

    public static class Builder {

        private final View<?> view;
        private final List<ComponentValueBinder> binders = new ArrayList<>();
        private final UrlParamSerializer urlParamSerializer = AppContext.getBean(UrlParamSerializer.class);

        private Builder(View<?> view) {
            this.view = view;
        }

        public <E extends Enum<?> & EnumClass<?>, C extends Select<E>> Builder addEnumBinding(Class<E> enumType, C... component) {
            for (C select : component) {
                binders.add(new ComponentValueBinder<>(select,
                        value -> value != null ? urlParamSerializer.serialize(value) : "",
                        id -> {
                            try {
                                return urlParamSerializer.deserialize(enumType, id);
                            } catch (Throwable ignored) {
                                return null;
                            }
                        }));
            }
            return this;
        }

        public <V extends UuidEntity, C extends Select<V>> Builder addEntitySelectBinding(C select,
                                                                                          Supplier<Collection<V>> itemSupplier) {
            doAddListDataItemBinding(select, itemSupplier);
            return this;
        }

        public <V extends UuidEntity, C extends ComboBoxBase> Builder addComboboxBinding(C comboBox,
                                                                                         Supplier<Collection<V>> itemsSupplier) {
            doAddListDataItemBinding(comboBox, itemsSupplier);
            return this;
        }

        private <V extends UuidEntity, C extends Component & HasValue<?, V>> void doAddListDataItemBinding(C listDataItem,
                                                                                                           Supplier<Collection<V>> itemsSupplier) {
            binders.add(new ComponentValueBinder<>(listDataItem,
                    value -> Optional.ofNullable(value).map(entity -> entity.getId().toString()).orElse(""),
                    id -> itemsSupplier.get().stream()
                            .filter(entity -> entity.getId().toString().equals(id))
                            .findFirst().orElse(null)));
        }

        public <C extends DatePicker> Builder addDatePickerBinding(C picker) {
            binders.add(new ComponentValueBinder<>(picker,
                    value -> Optional.ofNullable(value).map(urlParamSerializer::serialize).orElse(""),
                    date -> urlParamSerializer.deserialize(LocalDate.class, date)));
            return this;
        }

        public <C extends Component & HasValue<?, Boolean>> Builder addBooleanBinding(C... component) {
            for (C checkbox : component) {
                binders.add(new ComponentValueBinder<>(checkbox, Object::toString, Boolean::valueOf));
            }
            return this;
        }

        public <C extends Component & HasValue<?, String>> Builder addStringBinding(C... component) {
            binders.addAll(createStringBinders(stream(component).toList()));
            return this;
        }

        public <V, C extends Component & HasValue<?, V>> Builder addComponentBinding(C component,
                                                                                     Function<V, String> serializer,
                                                                                     Function<String, V> deserializer) {
            validateId(component);
            binders.add(new ComponentValueBinder(component, serializer, deserializer));
            return this;
        }

        public FieldValueQueryParameterBinder build() {
            return new FieldValueQueryParameterBinder(binders, view);
        }
    }
}
