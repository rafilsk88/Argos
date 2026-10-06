package com.argos.organizations;

import java.util.UUID;

/** Interface pública do módulo organizations: outros módulos só conversam por aqui, nunca pelas tabelas. */
public interface OrganizationApi {

    UUID createOrganization(String name);
}
