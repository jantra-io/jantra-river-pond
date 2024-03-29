package no.nav.jantra.river.configuration

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.jantra.river.Key
import no.nav.jantra.river.MessageType
import no.nav.jantra.river.IDataListener
import no.nav.jantra.river.IEventListener
import no.nav.jantra.river.IFailListener
import no.nav.jantra.river.ValidatedMessage
import no.nav.jantra.river.bridge.DataRiver
import no.nav.jantra.river.bridge.EventRiver
import no.nav.jantra.river.bridge.FailRiver
import no.nav.jantra.river.demandValue
import no.nav.jantra.river.plus


class ListenerBuilder(val rapid:RapidsConnection) {

    lateinit var event: MessageType.Event
    private lateinit var eventRiver: EventRiver
    private lateinit var dataRiver: DataRiver
    private lateinit var failRiver: FailRiver



    fun eventListener(event: MessageType.Event) : EventListenerBuilder {
        this.event = event
        return EventListenerBuilder(this)
    }

    fun failListener() : FailListenerBuilder {
        return FailListenerBuilder(this)
    }

    fun dataListener() : DataListenerBuilder {
        return DataListenerBuilder(this)
    }

    fun start() {
        if (::eventRiver.isInitialized) eventRiver.start()
        if (::dataRiver.isInitialized)  dataRiver.start()
        if (::failRiver.isInitialized)  failRiver.start()
    }

    class EventListenerBuilder(private val listenerBuilder: ListenerBuilder) {
        lateinit var listener: IEventListener
        lateinit var accepts: River.PacketValidation

        fun implementation(listener: IEventListener) : EventListenerBuilder {
            this.listener = listener
            return this
        }

        fun accept(accepts: River.PacketValidation) : EventListenerBuilder {
            this.accepts = accepts
            return this
        }

        fun build() : ListenerBuilder {
            val inlineValidator = if (!::accepts.isInitialized  && listener is ValidatedMessage) (listener as ValidatedMessage).accept() else River.PacketValidation{}
            val validation = if (::accepts.isInitialized) accepts else inlineValidator + River.PacketValidation{it.demandValue(Key.EVENT_NAME,listenerBuilder.event)}
            listenerBuilder.eventRiver = EventRiver(listenerBuilder.rapid,listener, validation)
            return listenerBuilder
        }

    }

    class FailListenerBuilder(private val listenerBuilder: ListenerBuilder) {
        lateinit var accepts: River.PacketValidation
        lateinit var listener: IFailListener

        fun implementation(listener: IFailListener) : FailListenerBuilder {
            this.listener = listener
            return this
        }

        fun accept(accepts: River.PacketValidation) : FailListenerBuilder {
            this.accepts = accepts
            return this
        }

        fun build() : ListenerBuilder {
            if (!this::accepts.isInitialized) accepts = River.PacketValidation {
                it.demandValue(Key.EVENT_NAME.str,listenerBuilder.event.value)
            }
            listenerBuilder.failRiver = FailRiver(listenerBuilder.rapid,listener,accepts)
            return listenerBuilder
        }

    }

    class DataListenerBuilder(private val listenerBuilder: ListenerBuilder) {
        lateinit var accepts: River.PacketValidation
        lateinit var listener: IDataListener

        fun implementation(listener: IDataListener) : DataListenerBuilder {
            this.listener = listener
            return this
        }

        fun accept(validation: River.PacketValidation) : DataListenerBuilder {
            this.accepts = validation
            return this
        }

        fun build() : ListenerBuilder {
            if (!this::accepts.isInitialized) accepts = River.PacketValidation {
                it.demandValue(Key.EVENT_NAME.str,listenerBuilder.event.value)
            }
            listenerBuilder.dataRiver = DataRiver(listenerBuilder.rapid,listener,accepts)
            return listenerBuilder
        }

    }

}









