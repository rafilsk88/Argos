package com.argos.organizations;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrganizationService implements OrganizationApi {

    private final OrganizationRepository repository;

    OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UUID createOrganization(String name) {
        return repository.save(new Organization(name.trim())).getId();
    }
}
