package no.nav.reka.river.examples

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event.services.DocumentRecievedListener
import no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event.services.FormatDokumentService
import no.nav.reka.river.examples.example_3_event_triger_2_behov_emitting_event.services.PersistDocument
import no.nav.reka.river.examples.services.*

import no.nav.reka.river.redis.RedisStore



fun RapidsConnection.buildApp(redisStore: RedisStore): RapidsConnection {
    RetrieveFullNameService(this)
    ApplicationStartedListener(this)
    DocumentRecievedListener(this)
    FormatDokumentService(this)
    PersistDocument(this)
    return this
}
