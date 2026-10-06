package com.argos.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Clock injetável: permite testes determinísticos de expiração e bloqueio. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
