package no.nav.jantra.river.examples.example_7_simple_saga.services

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
import kotlin.jvm.Throws

class FormatDokumentService(rapidsConnection: RapidsConnection) : Løser(rapidsConnection) {

    override val event: MessageType.Event = EventName.DOCUMENT_RECIEVED
    private val ILLEGAL_CHARACTER = "%"
    override fun accept(): River.PacketValidation = River.PacketValidation {
        it.demandValue(Key.EVENT_NAME, EventName.DOCUMENT_RECIEVED)
        it.demandValue(Key.BEHOV,BehovName.FORMAT_DOCUMENT)
        it.interestedIn(DataFelt.RAW_DOCUMENT)
    }

    @Throws(Exception::class)
    private fun formatDocument(rawDocument:String) {
        takeIf { !rawDocument.contains(ILLEGAL_CHARACTER)} ?: throw Exception("Illegal character")
        println("Document is now formated $rawDocument")
    }

    override fun onBehov(behov: Behov) {
        runCatching {
            formatDocument(behov[DataFelt.RAW_DOCUMENT].asText())
        }.onSuccess {
            rapidsConnection.publish(behov.createData(mapOf(
                DataFelt.FORMATED_DOCUMENT to "this is my formated document"
            )))
        }.onFailure {
            rapidsConnection.publish(behov.createFail("Illegal character encountered"))
        }



    }


}