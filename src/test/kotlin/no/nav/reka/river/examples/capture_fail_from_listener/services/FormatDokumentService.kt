package no.nav.reka.river.examples.capture_fail_from_listener.services

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.reka.river.Consumer
import no.nav.reka.river.Key
import no.nav.reka.river.demandValue
import no.nav.reka.river.examples.basic_consumer.BehovName
import no.nav.reka.river.examples.basic_consumer.DataFelt
import no.nav.reka.river.examples.basic_consumer.EventName
import no.nav.reka.river.interestedIn
import no.nav.reka.river.model.Behov
import no.nav.reka.river.model.Data

class FormatDokumentService(rapidsConnection: RapidsConnection) : Consumer(rapidsConnection) {
    override fun accept(): River.PacketValidation = River.PacketValidation {
        it.demandValue(Key.EVENT_NAME, EventName.DOCUMENT_RECIEVED)
        it.demandValue(Key.BEHOV,BehovName.FORMAT_DOCUMENT)
        it.interestedIn(DataFelt.RAW_DOCUMENT)
        it.interestedIn(DataFelt.RAW_DOCUMENT_FORMAT)
    }

    private fun formatDocument(rawDocument:String) {
        println("Document is now formated $rawDocument")
    }

    override fun onBehov(packet: Behov) {
        formatDocument(packet[DataFelt.RAW_DOCUMENT].asText())
        val documentFormat = packet[DataFelt.RAW_DOCUMENT_FORMAT].takeUnless { it.isMissingOrNull() }?.asText()
        if (documentFormat != "ebcdic") {
            publishBehov(
                packet.createBehov(
                    BehovName.PERSIST_DOCUMENT,
                    mapOf(DataFelt.FORMATED_DOCUMENT to "This is my formated document")
                )
            )
        }
        else {
            publishFail(packet.createFail("Unable to process files with EBCDIC charset"))
        }
    }


}