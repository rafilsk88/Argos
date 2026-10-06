package com.argos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do monólito modular. UserDetailsServiceAutoConfiguration é excluída porque a
 * autenticação é feita por JWT próprio; sem a exclusão, o Spring cria um usuário com senha aleatória no log.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ArgosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArgosApplication.class, args);
    }
}
