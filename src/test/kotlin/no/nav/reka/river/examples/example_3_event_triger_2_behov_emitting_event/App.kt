package no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event.services.DocumentRecievedListener
import no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event.services.FormatDokumentService
import no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event.services.PersistDocument

import no.nav.reka.river.redis.RedisStore



fun RapidsConnection.`setup EventTriggering 2 Behov And Emitting Event`(redisStore: RedisStore): RapidsConnection {
    DocumentRecievedListener(this).start()
    FormatDokumentService(this).start()
    PersistDocument(this).start()
    return this
}
