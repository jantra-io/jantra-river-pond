package no.nav.jantra.river.examples.example_3_event_triger_2_behov_emitting_event.services

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.jantra.river.basic.Løser
import no.nav.jantra.river.Key
import no.nav.jantra.river.MessageType
import no.nav.jantra.river.demandValue
import no.nav.jantra.river.examples.example_1_basic_løser.BehovName
import no.nav.jantra.river.examples.example_1_basic_løser.DataFelt
import no.nav.jantra.river.examples.example_1_basic_løser.EventName
import no.nav.jantra.river.interestedIn
import no.nav.jantra.river.model.Behov
import no.nav.jantra.river.publish

class FormatDokumentService(rapidsConnection: RapidsConnection) : Løser(rapidsConnection) {

    override val event: MessageType.Event = EventName.DOCUMENT_RECIEVED
    override fun accept(): River.PacketValidation = River.PacketValidation {
        it.demandValue(Key.BEHOV,BehovName.FORMAT_DOCUMENT)
        it.interestedIn(DataFelt.RAW_DOCUMENT)
    }

    private fun formatDocument(rawDocument:String) {
        println("Document is now formated $rawDocument")
    }

    override fun onBehov(packet: Behov) {
        formatDocument(packet[DataFelt.RAW_DOCUMENT].asText())
        rapidsConnection.publish(packet.createBehov(BehovName.PERSIST_DOCUMENT, mapOf(DataFelt.FORMATED_DOCUMENT to "This is my formated document")))
    }


}