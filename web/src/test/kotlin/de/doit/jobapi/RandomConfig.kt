package de.doit.jobapi

import com.github.javafaker.Faker
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RandomConfig {

    @Bean
    fun jobRandomizer(): EasyRandom {
        val easyRandomParameters = EasyRandomParameters()
                .randomize(FieldPredicates.named("title"), Randomizer { Faker().job().position() })
                .randomize(FieldPredicates.named("description"), Randomizer { Faker().job().field() })
                .randomize(FieldPredicates.named("latitude"), Randomizer { Faker().address().latitude().toDouble() })
                .randomize(FieldPredicates.named("longitude"), Randomizer { Faker().address().longitude().toDouble() })
                .randomize(FieldPredicates.named("payment"), Randomizer { Faker().commerce().price().toBigDecimal() })

        return EasyRandom(easyRandomParameters)
    }

}