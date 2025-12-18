package com.company.crm.app.service.user;

import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final ClientRepository clientRepository;

    public UserService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<User> loadAccountManagers() {
        return clientRepository.findAllByAccountManagerNotNull().stream()
                .map(Client::getAccountManager)
                .distinct()
                .toList();
    }
}
