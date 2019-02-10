package de.halfbit.knot.dsl

import de.halfbit.knot.DefaultKnot
import de.halfbit.knot.Knot
import io.reactivex.Observable

@DslMarker
annotation class KnotDsl

@KnotDsl
class KnotBuilder<State : Any, Command : Any> {

    private var initialState: State? = null

    @PublishedApi
    internal val onCommandUpdateStateTransformers =
        mutableListOf<OnCommandUpdateStateTransformer<Command, State>>()

    @PublishedApi
    internal val onCommandToCommandTransformers =
        mutableListOf<TypedCommandToCommandTransformer<Command, Command, State>>()

    @PublishedApi
    internal val onEventUpdateStateTransformers =
        mutableListOf<OnEventUpdateStateTransformer<*, State>>()

    @PublishedApi
    internal val onEventToCommandTransformers =
        mutableListOf<OnEventToCommandTransformer<*, Command, State>>()

    fun build(): Knot<State, Command> = DefaultKnot(
        checkNotNull(initialState) { "state { initial } must be set" },
        onCommandUpdateStateTransformers,
        onCommandToCommandTransformers,
        onEventUpdateStateTransformers,
        onEventToCommandTransformers
    )

    @Suppress("UNCHECKED_CAST")
    inline fun <reified C : Command> on(
        onCommand: OnCommand<State, C, Command>.() -> Unit
    ): OnCommand<State, C, Command> = OnCommand(
        C::class,
        onCommandUpdateStateTransformers as MutableList<OnCommandUpdateStateTransformer<C, State>>,
        onCommandToCommandTransformers as MutableList<TypedCommandToCommandTransformer<C, Command, State>>
    ).also(onCommand)

    @Suppress("UNCHECKED_CAST")
    inline fun <Event : Any> on(
        source: Observable<Event>,
        onEvent: OnEvent<State, Event, Command>.() -> Unit
    ) = OnEvent(
        source,
        onEventUpdateStateTransformers as MutableList<OnEventUpdateStateTransformer<Event, State>>,
        onEventToCommandTransformers as MutableList<OnEventToCommandTransformer<Event, Command, State>>
    ).also(onEvent)

    fun state(state: StateBuilder<State>.() -> Unit) {
        StateBuilder<State>()
            .also(state)
            .let { initialState = it.initial }
    }
}
