package com.flip7.game.testutils;

import com.flip7.game.repository.DeckRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class DeterministicDeckConfig {

    @Bean
    @Primary
    DeterministicDeckService deterministicDeckService(DeckRepository deckRepository) {
        return new DeterministicDeckService(deckRepository);
    }
}
