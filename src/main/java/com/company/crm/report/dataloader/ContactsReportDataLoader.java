package com.company.crm.report.dataloader;

import com.company.crm.ai.report.run.AiReportExecutionService;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.report.mapper.ReportContactMapper;
import io.jmix.aitools.ExcludeFromAi;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.company.crm.report.dataloader.ContactsReportDataLoader.BEAN_NAME;

/**
 * DataLoader for Contacts section of Client360Report.
 * Loads active contacts for a client.
 */
@Component(BEAN_NAME)
public class ContactsReportDataLoader implements ReportDataLoader {

    public static final String BEAN_NAME = "contactsReportDataLoader";

    private final DataManager dataManager;
    private final ReportContactMapper contactMapper;
    private final Metadata metadata;

    public ContactsReportDataLoader(DataManager dataManager, ReportContactMapper contactMapper, Metadata metadata) {
        this.dataManager = dataManager;
        this.contactMapper = contactMapper;
        this.metadata = metadata;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        if (client == null) {
            return List.of();
        }

        List<Contact> contacts = dataManager.load(Contact.class)
                .query("SELECT c FROM Contact c WHERE c.client.id = :clientId " +
                        "AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE) " +
                        "ORDER BY c.startDate DESC")
                .parameter("clientId", client.getId())
                .fetchPlanProperties("person", "position", "phone", "email", "startDate", "endDate")
                .list();

        boolean aiRun = Boolean.TRUE.equals(params.get(AiReportExecutionService.AI_RUN_PARAMETER));
        return contacts.stream()
                .map(contactMapper::toReportMap)
                .map(fields -> aiRun ? withoutExcludedFromAi(fields) : fields)
                .toList();
    }

    /**
     * The CRM assistant reads the report output, so attributes marked {@link ExcludeFromAi} (phone, email)
     * are left out of an AI run; the report a user runs in the application keeps them.
     */
    private Map<String, Object> withoutExcludedFromAi(Map<String, Object> fields) {
        Map<String, Object> allowed = new HashMap<>(fields);
        metadata.getClass(Contact.class).getProperties().stream()
                .filter(property -> property.getAnnotations().containsKey(ExcludeFromAi.class.getName()))
                .map(MetaProperty::getName)
                .forEach(allowed::remove);
        return allowed;
    }
}