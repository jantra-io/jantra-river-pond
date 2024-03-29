package no.nav.jantra.river.examples.example_6_composite_med_fail_listener.services

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.jantra.river.*
import no.nav.jantra.river.examples.example_1_basic_løser.BehovName
import no.nav.jantra.river.examples.example_1_basic_løser.DataFelt
import no.nav.jantra.river.model.Data
import no.nav.jantra.river.model.Event
import no.nav.jantra.river.model.Fail
import no.nav.jantra.river.bridge.CompositeListener
import no.nav.jantra.river.examples.example_1_basic_løser.EventName

class DocumentRecievedListener(val rapid: RapidsConnection) : CompositeListener,ValidatedMessage {

    val event:MessageType.Event get() = EventName.DOCUMENT_RECIEVED
    override fun accept(): River.PacketValidation  {
        return River.PacketValidation {
            it.demandValue(Key.EVENT_NAME,event)
            it.interestedIn(DataFelt.RAW_DOCUMENT)
        }
    }

    override fun onData(data: Data) {
        TODO("Not yet implemented")
    }

    override fun onEvent(event: Event) {
        this.rapid.publish(event.createBehov(BehovName.FORMAT_DOCUMENT, mapOf(DataFelt.RAW_DOCUMENT to event[DataFelt.RAW_DOCUMENT],
            DataFelt.RAW_DOCUMENT_FORMAT to "ebcdic")))
    }

    override fun onFail(fail: Fail) {
        this.rapid.publish(fail.createBehov(BehovName.FORMAT_DOCUMENT_IBM,mapOf(DataFelt.RAW_DOCUMENT to fail[DataFelt.RAW_DOCUMENT])))
    }

}