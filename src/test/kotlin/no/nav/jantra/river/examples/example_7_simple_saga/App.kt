package no.nav.jantra.river.examples.example_7_simple_saga

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.jantra.river.Key
import no.nav.jantra.river.bridge.DataRiver
import no.nav.jantra.river.bridge.EventRiver
import no.nav.jantra.river.bridge.FailRiver
import no.nav.jantra.river.composite.DelegatingFailKanal
import no.nav.jantra.river.composite.SagaRunner
import no.nav.jantra.river.composite.StatefullDataKanal
import no.nav.jantra.river.composite.StatefullEventKanal
import no.nav.jantra.river.configuration.dsl.topology
import no.nav.jantra.river.demandValue
import no.nav.jantra.river.examples.example_1_basic_løser.BehovName
import no.nav.jantra.river.examples.example_1_basic_løser.DataFelt
import no.nav.jantra.river.examples.example_1_basic_løser.EventName
import no.nav.jantra.river.examples.example_7_simple_saga.services.FormatDokumentService
import no.nav.jantra.river.examples.example_7_simple_saga.services.LegacyIBMFormatter
import no.nav.jantra.river.examples.example_7_simple_saga.services.PersistDocument
import no.nav.jantra.river.interestedIn

import no.nav.jantra.river.redis.RedisStore



fun RapidsConnection.buildSagaScenario(redisStore: RedisStore): RapidsConnection {
    val formattingService = DocumentFormatingSaga(EventName.DOCUMENT_RECIEVED)
    val sagaRunner = SagaRunner(redisStore,this, formattingService )
    val dataListener =  StatefullDataKanal(EventName.DOCUMENT_RECIEVED,
                                        arrayOf(DataFelt.FORMATED_DOCUMENT,DataFelt.FORMATED_DOCUMENT_IBM,DataFelt.DOCUMENT_REFERECE),
                                        sagaRunner,
                                        redisStore,
                                        this@buildSagaScenario)
    sagaRunner.dataKanal = dataListener
    DataRiver(this,dataListener, dataListener.accept()).start()
    val eventListener = StatefullEventKanal(EventName.DOCUMENT_RECIEVED, redisStore, arrayOf(DataFelt.RAW_DOCUMENT),sagaRunner)
    EventRiver(this, eventListener, eventListener.accept()).start()
    val failKanal = DelegatingFailKanal(EventName.DOCUMENT_RECIEVED,sagaRunner,this)
    FailRiver(this,failKanal, {  }).start()


    FormatDokumentService(this).start()
    LegacyIBMFormatter(this).start()
    PersistDocument(this).start()
    return this
}

fun RapidsConnection.buildSagaViaDSL(redisStore: RedisStore): RapidsConnection {

    topology(this) {
        saga("My saga",redisStore) {
            implementation(DocumentFormatingSaga(EventName.DOCUMENT_RECIEVED))
            eventListener(EventName.DOCUMENT_RECIEVED) {
                this.capture(DataFelt.RAW_DOCUMENT)
            }
            dataListener(
                        DataFelt.FORMATED_DOCUMENT,
                        DataFelt.FORMATED_DOCUMENT_IBM,
                        DataFelt.DOCUMENT_REFERECE
            )
            
            løser(BehovName.FORMAT_DOCUMENT) {
                implementation = FormatDokumentService(this@buildSagaViaDSL)
                accepts {
                     it.demandValue(Key.EVENT_NAME, EventName.DOCUMENT_RECIEVED)
                     it.demandValue(Key.BEHOV,BehovName.FORMAT_DOCUMENT)
                     it.interestedIn(DataFelt.RAW_DOCUMENT)
                }
            }
            løser(BehovName.FORMAT_DOCUMENT_IBM) {
                implementation = LegacyIBMFormatter(this@buildSagaViaDSL)
                accepts {
                      it.demandValue(Key.EVENT_NAME, EventName.DOCUMENT_RECIEVED)
                      it.demandValue(Key.BEHOV, BehovName.FORMAT_DOCUMENT_IBM)
                      it.interestedIn(DataFelt.RAW_DOCUMENT)
                      it.interestedIn(DataFelt.RAW_DOCUMENT_FORMAT)
                }
            }
            løser(BehovName.PERSIST_DOCUMENT) {
                implementation = PersistDocument(this@buildSagaViaDSL)
                accepts {
                    it.demandValue(Key.BEHOV,BehovName.PERSIST_DOCUMENT)
                    it.interestedIn(DataFelt.FORMATED_DOCUMENT)
                    it.interestedIn(DataFelt.FORMATED_DOCUMENT_IBM)
                }
            }
            failListener{}
        }
    }.start()
    return this
}
