package no.nav.jantra.river.composite

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helsearbeidsgiver.felles.rapidsrivers.composite.Transaction
import no.nav.jantra.river.IDataFelt
import no.nav.jantra.river.IKey
import no.nav.jantra.river.MessageType
import no.nav.jantra.river.model.Event
import no.nav.jantra.river.model.Fail
import no.nav.jantra.river.model.Message
import no.nav.jantra.river.model.TxMessage
import no.nav.jantra.river.redis.IRedisStore
import no.nav.jantra.river.redis.RedisKey
import org.slf4j.LoggerFactory


abstract class  Saga(val eventName: MessageType.Event)  {
    protected lateinit var redisStore: IRedisStore

    protected lateinit var rapid: RapidsConnection
    open fun onError(feil: Fail): Transaction {
        return Transaction.TERMINATE
    }
    abstract fun dispatchBehov(message: TxMessage, transaction: Transaction)
    abstract fun finalize(message: TxMessage)
    abstract fun terminate(message: TxMessage)

    fun isDataCollected(vararg keys: RedisKey): Boolean {
        return redisStore.exist(*keys) == keys.size.toLong()
    }

    internal fun setRedis(redisStore: IRedisStore) {
        this.redisStore = redisStore
    }

    internal fun setRapid(rapid: RapidsConnection) {
        this.rapid = rapid
    }

}



class SagaRunner(val redisStore: IRedisStore,val rapidsConnection: RapidsConnection, val implementation: Saga ) : MessageListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    lateinit var dataKanal: StatefullDataKanal

    init {
        implementation.setRedis(redisStore)
        implementation.setRapid(rapidsConnection)
    }

    override fun onMessage(packet: Message) {
        val txMessage = packet as TxMessage
        val transaction: Transaction = determineTransactionState(txMessage)

        when (transaction) {
            Transaction.NEW -> {
                initialTransactionState(txMessage)
                implementation.dispatchBehov(txMessage, transaction)
            }
            Transaction.IN_PROGRESS -> implementation.dispatchBehov(txMessage, transaction)
            Transaction.FINALIZE -> implementation.finalize(txMessage)
            Transaction.TERMINATE -> implementation.terminate(txMessage)
            Transaction.NOT_ACTIVE -> notActive(txMessage)
        }
    }

    private fun notActive(message: TxMessage) {
        log.error("Transaction is not active for message $message")
    }

    fun determineTransactionState(message: TxMessage): Transaction {
        // event bør ikke ha UUID men dette er ikke konsistent akkuratt nå så midlertidig blir det sånn til vi får det konsistent.
        // vi trenger også clientID for correlation
        val transactionId = message.riverId()!!
        if (message is Fail) { // Returnerer INPROGRESS eller TERMINATE
            log.error("Feilmelding er ${message.toString()}")
            return implementation.onError(message as Fail)
        }

        val eventKey = RedisKey.transactionKey(transactionId, implementation.eventName)
        val value = redisStore.get(eventKey)
        if (value.isNullOrEmpty()) {
            if (!(message is Event)) {
                log.error("TransactionID can be undefined only if the incoming message is Event.")
                return Transaction.NOT_ACTIVE
            }

            redisStore.set(eventKey, message.clientId?:transactionId)
            return Transaction.NEW
        } else {
            if (isDataCollected(transactionId)) return Transaction.FINALIZE
        }
        return Transaction.IN_PROGRESS
    }

    open fun initialTransactionState(message: TxMessage) {}



    fun withDataKanal(dataKanalSupplier: (t: SagaRunner) -> StatefullDataKanal): SagaRunner {
        dataKanal = dataKanalSupplier.invoke(this)
        return this
    }

    open fun isDataCollected(uuid: String): Boolean = dataKanal.isAllDataCollected(RedisKey.clientKey(uuid))

}




class SagaBuilder(val eventName: MessageType.Event,
                  val implementation:MessageListener,
                  private val redisStore: IRedisStore,
                  val rapidsConnection: RapidsConnection) {

        fun event(eventName: MessageType.Event, block: SagaEventListener.() -> Unit) {
                SagaEventListener(eventName).apply { block }
        }


/*
        fun start() {
            val eventListener = StatefullEventKanal(redisStore,eventName,dataFelter.map { it }.toTypedArray() ,implementation
            , rapidsConnection )
            val dataKanal = StatefullDataKanal(this.dataFelter.map { it }.toTypedArray(),eventName,implementation,rapidsConnection,redisStore)
            val failListener = DelegatingFailKanal(eventName,implementation,rapidsConnection)
            eventListener.start()
            dataKanal.start()
            failListener.start()
        }

 */
    }


class SagaEventListener(eventName: MessageType.Event) {
    private lateinit var dataFelter: List<IKey>

    fun capture(dataFelter: List<IDataFelt>) {
        this.dataFelter = dataFelter
    }

    fun build() {
      //  StatefullEventKanal
    }

}

class SagaDataListener(eventName: MessageType.Event, dataFelter: List<IKey> = listOf()) {
    private lateinit var dataFelter: List<IKey>

    fun dataFelter(dataFelter: List<IDataFelt>) {
        this.dataFelter = dataFelter

    }

}