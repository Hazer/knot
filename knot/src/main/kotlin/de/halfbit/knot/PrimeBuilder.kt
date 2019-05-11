package de.halfbit.knot

import kotlin.reflect.KClass

fun <State : Any, Change : Any, Action : Any> prime(
    block: PrimeBuilder<State, Change, Action>.() -> Unit
): Prime<State, Change, Action> =
    PrimeBuilder<State, Change, Action>()
        .also(block)
        .build()

@KnotDsl
class PrimeBuilder<State : Any, Change : Any, Action : Any>
internal constructor() {
    private val reducers = mutableMapOf<KClass<out Change>, Reducer<State, Change, Action>>()
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()
    private val stateInterceptors = mutableListOf<Interceptor<State>>()
    private val changeInterceptors = mutableListOf<Interceptor<Change>>()
    private val actionInterceptors = mutableListOf<Interceptor<Action>>()

    /** A section for [Change] related declarations. */
    fun changes(block: ChangesBuilder<State, Change, Action>.() -> Unit) {
        ChangesBuilder(reducers, changeInterceptors).also(block)
    }

    /** A section for [Action] related declarations. */
    fun actions(block: ActionsBuilder<Change, Action>.() -> Unit) {
        ActionsBuilder(actionTransformers, actionInterceptors).also(block)
    }

    /** A section for Event related declarations. */
    fun events(block: EventsBuilder<Change>.() -> Unit) {
        EventsBuilder(eventTransformers).also(block)
    }

    /** A section for declaring interceptors for [State], [Change] or [Action]. */
    fun intercept(block: InterceptBuilder<State, Change, Action>.() -> Unit) {
        InterceptBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    /** A section for declaring watchers for [State], [Change] or [Action]. */
    fun watch(block: WatchBuilder<State, Change, Action>.() -> Unit) {
        WatchBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    internal fun build(): Prime<State, Change, Action> = DefaultPrime(
        reducers = reducers,
        eventTransformers = eventTransformers,
        actionTransformers = actionTransformers,
        stateInterceptors = stateInterceptors,
        changeInterceptors = changeInterceptors,
        actionInterceptors = actionInterceptors
    )

    @KnotDsl
    class ChangesBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val reducers: MutableMap<KClass<out Change>, Reducer<State, Change, Action>>,
        private val changeInterceptors: MutableList<Interceptor<Change>>
    ) {

        /**
         * Mandatory reduce function which receives the current [State] and a [Change]
         * and must return [Effect] with a new [State] and an optional [Action].
         *
         * New *State* and *Action* can be joined together using overloaded [State.plus()]
         * operator. For returning *State* without action call *.only* on the state.
         *
         * Example:
         * ```
         *  changes {
         *      reduce { change ->
         *          when (change) {
         *              is Change.Load -> copy(value = "loading") + Action.Load
         *              is Change.Load.Success -> copy(value = change.payload).only
         *              is Change.Load.Failure -> copy(value = "failed").only
         *          }
         *      }
         *  }
         * ```
         */
        fun reduce(changeType: KClass<out Change>, reduce: Reducer<State, Change, Action>) {
            reducers[changeType] = reduce
        }

        inline fun <reified C : Change> reduce(noinline reduce: Reducer<State, C, Action>) {
            @Suppress("UNCHECKED_CAST")
            reduce(C::class, reduce as Reducer<State, Change, Action>)
        }

        fun intercept(interceptor: Interceptor<Change>) {
            changeInterceptors += interceptor
        }

        fun watch(watcher: Watcher<Change>) {
            changeInterceptors += WatchingInterceptor(watcher)
        }

        /** Turns [State] into an [Effect] without [Action]. */
        val State.only: Effect<State, Action> get() = Effect(this)

        /** Combines [State] and [Action] into [Effect]. */
        operator fun State.plus(action: Action) = Effect(this, action)
    }
}
