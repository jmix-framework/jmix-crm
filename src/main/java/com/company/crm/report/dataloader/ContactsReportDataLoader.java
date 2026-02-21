package com.company.crm.report.dataloader;

import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.report.mapper.ReportContactMapper;
import io.jmix.core.DataManager;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataLoader for Contacts section of Client360Report.
 * Loads active contacts for a client.
 */
@Component("contactsReportDataLoader")
public class ContactsReportDataLoader implements ReportDataLoader {

    private final DataManager dataManager;
    private final ReportContactMapper contactMapper;

    public ContactsReportDataLoader(DataManager dataManager, ReportContactMapper contactMapper) {
        this.dataManager = dataManager;
        this.contactMapper = contactMapper;
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

        return contacts.stream()
                .map(contactMapper::toReportMap)
                .toList();
    }
}