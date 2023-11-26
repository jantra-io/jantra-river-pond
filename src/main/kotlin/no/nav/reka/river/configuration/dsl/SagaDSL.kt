package no.nav.reka.river.configuration.dsl

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.reka.river.IDataFelt
import no.nav.reka.river.IEventListener
import no.nav.reka.river.IKey
import no.nav.reka.river.Key
import no.nav.reka.river.MessageType
import no.nav.reka.river.bridge.BehovRiver
import no.nav.reka.river.bridge.DataRiver
import no.nav.reka.river.bridge.EventRiver
import no.nav.reka.river.bridge.FailRiver
import no.nav.reka.river.composite.MessageListener
import no.nav.reka.river.composite.Saga
import no.nav.reka.river.composite.SagaEventListener
import no.nav.reka.river.composite.SagaRunner
import no.nav.reka.river.composite.StatefullDataKanal
import no.nav.reka.river.composite.StatefullEventKanal
import no.nav.reka.river.demandValue
import no.nav.reka.river.plus
import no.nav.reka.river.redis.RedisStore


class SagaBuilder(private val rapid: RapidsConnection,
                  private val redisStore: RedisStore,
                  val capture: MutableList<IKey> = mutableListOf(),
                  val løser: MutableList<BehovRiver> = mutableListOf()) {
    lateinit var eventName: MessageType.Event
    private lateinit var sagaRunner: SagaRunner
    private lateinit var eventListener: StatefullEventKanal
    lateinit var dataListener: StatefullDataKanal

    fun implementation(implementation: Saga) {
        sagaRunner = SagaRunner(redisStore, implementation )
    }

    @DSLTopology
    fun capture(vararg datafelter:IDataFelt) {
        capture.addAll(datafelter)
    }
    @DSLTopology
    fun eventListener(eventName: MessageType.Event ,block: SagaEventListenerBuilder.()-> Unit) {
        eventListener = SagaEventListenerBuilder(eventName,redisStore,rapid,sagaRunner).apply(block).build()
    }

    fun dataListener(vararg datafelter: IKey) {
        dataListener = SagaDataListenerBuilder(eventListener.event,redisStore,rapid,sagaRunner).capture(*datafelter).start()
    }
    fun løser(behov: MessageType.Behov, block: LøserBuilder.() -> Unit) {
        løser.add(LøserBuilder(behov,eventListener.event, rapid).apply (block ).build())
    }

    fun start() {

        eventListener.start()
        dataListener.start()
        løser.forEach{
            it.start()
        }
    }
}

class SagaDataListenerBuilder( private val eventName: MessageType.Event,
                               private val redisStore: RedisStore,
                               private val rapid: RapidsConnection,
                               private val messageListener: SagaRunner) {

    lateinit var datafelter: Array<IKey>

    fun capture(vararg datafelter: IKey) : SagaDataListenerBuilder {
        this.datafelter = datafelter.map { it }.toTypedArray()
        return this
    }

    fun start() : StatefullDataKanal {
        return StatefullDataKanal(datafelter,eventName,messageListener,rapid,redisStore)
    }

}

@DSLTopology
class SagaEventListenerBuilder(private val eventName: MessageType.Event,
                               private val redisStore: RedisStore,
                               private val rapid: RapidsConnection,
                               private val sagaRunner: SagaRunner
                            ) {
    @DSLTopology
    private lateinit var accept: River.PacketValidation
    private lateinit var capture: Array<IDataFelt>

    @DSLTopology()
    fun accepts(jsonMessage: (JsonMessage) -> Unit)  {
        accept = River.PacketValidation {
            jsonMessage.invoke(it)
        }
    }

    fun capture(vararg datafelter: IDataFelt) {
        if (::accept.isInitialized) {
            accept += {
                it.interestedIn(*datafelter.map { it.str }.toTypedArray())
            }
        }
        else accept = River.PacketValidation{
                it.interestedIn(*datafelter.map { it.str }.toTypedArray())
            }
        this.capture = datafelter.map { it }.toTypedArray()
    }


    internal fun build() : StatefullEventKanal {

        return StatefullEventKanal(redisStore,eventName,capture,sagaRunner,rapid)
    }

}

class SagaDSL {
}