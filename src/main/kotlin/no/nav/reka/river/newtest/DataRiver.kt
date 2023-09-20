package no.nav.reka.river.newtest

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.reka.river.model.Data
import no.nav.reka.river.model.Event
import no.nav.reka.river.model.Fail
import no.nav.reka.river.test.IDataListener
import no.nav.reka.river.test.IEventListener

class DataRiver (val rapidsConnection: RapidsConnection, val eventListener: IDataListener, private val riverValidation: River.PacketValidation) : River.PacketListener{

    fun start() {
        configureAsDataListener(
            River(rapidsConnection).apply {
                validate(riverValidation)
            }
        ).register(this)
    }

    private fun configureAsDataListener(river: River): River {
        return river.validate {
            Data.packetValidator.validate(it)
        }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        eventListener.onData(Data.create(packet))
    }


}