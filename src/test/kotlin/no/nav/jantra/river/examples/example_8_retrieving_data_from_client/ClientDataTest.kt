package no.nav.jantra.river.examples.example_8_retrieving_data_from_client

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.jantra.river.EndToEndTest
import no.nav.jantra.river.examples.example_8_retrieving_data_from_client.frontend.ApplicationRecievedProducer
import no.nav.jantra.river.redis.RedisStore
import no.nav.jantra.river.pause
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ClientDataTest() : EndToEndTest() {

    @Test
    fun `client can recieve data via redis`() {
        val dokumentRef = ApplicationRecievedProducer(this.rapid, this.redisPoller).publish("My application form")
        pause()
        Assertions.assertFalse(dokumentRef.isNullOrBlank())
    }



    @Test
    fun `client can recieve fail messages via redis`() {
        val dokumentRef = ApplicationRecievedProducer(this.rapid, this.redisPoller).publish("My application% form")
        Thread.sleep(5000)
        Assertions.assertFalse(dokumentRef.isNullOrBlank())
    }


    override val appBuilder: (rapidConnection: RapidsConnection, redisStore: RedisStore) -> RapidsConnection
        get() = { rapid: RapidsConnection, redisStore: RedisStore -> rapid.`client retrieving data`(redisStore)}

}