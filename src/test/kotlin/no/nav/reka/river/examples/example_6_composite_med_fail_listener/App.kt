package no.nav.reka.river.examples.example_6_composite_med_fail_listener

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.reka.river.examples.example_1_basic_løser.EventName
import no.nav.reka.river.examples.example_6_composite_med_fail_listener.services.DocumentRecievedListener
import no.nav.reka.river.configuration.ListenerBuilder
import no.nav.reka.river.configuration.dsl.composition
import no.nav.reka.river.examples.example_1_basic_løser.BehovName
import no.nav.reka.river.examples.example_1_basic_løser.DataFelt
import no.nav.reka.river.examples.example_5_capture_fail_from_listener.`setup EventListener reacting to Failure`
import no.nav.reka.river.examples.example_6_composite_med_fail_listener.services.FormatDokumentService
import no.nav.reka.river.examples.example_6_composite_med_fail_listener.services.LegacyIBMFormatter
import no.nav.reka.river.examples.example_6_composite_med_fail_listener.services.PersistDocument
import no.nav.reka.river.interestedIn


fun RapidsConnection.`setup EventListener reacting to Failure`(): RapidsConnection {
    val documentListener = DocumentRecievedListener(this)


    composition("Provides document formatting capability", this) {
        eventListener(EventName.DOCUMENT_RECIEVED) {
            implementation = documentListener
        }
        løser(BehovName.FORMAT_DOCUMENT) {
            implementation = FormatDokumentService(this@`setup EventListener reacting to Failure`)
        }
        løser(BehovName.FORMAT_DOCUMENT_IBM) {
            implementation = LegacyIBMFormatter(this@`setup EventListener reacting to Failure`)

        }
        løser(BehovName.PERSIST_DOCUMENT) {
            implementation = PersistDocument(this@`setup EventListener reacting to Failure`)

        }
        failListener {
            implementation = documentListener

        }
    }.start()
    return this
}
/*
    ListenerBuilder(this)
            .eventListener(EventName.DOCUMENT_RECIEVED)
            .implementation(documentListener)
            .build()
        .failListener()
            .implementation(documentListener)
            .accept {
                it.interestedIn(DataFelt.RAW_DOCUMENT)
            }
            .build()
        .start()
    /*
    FormatDokumentService(this).start()
    LegacyIBMFormatter(this).start()
    PersistDocument(this).start()
    */
    return this

} */