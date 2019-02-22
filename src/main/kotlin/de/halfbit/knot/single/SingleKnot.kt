package de.halfbit.knot.single

import de.halfbit.knot.Knot
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference

fun <State : Any, Command : Any> singleKnot(
    init: KnotBuilder<State, Command>.() -> Unit
): Knot<State, Command> =
    KnotBuilder<State, Command>()
        .also(init)
        .build()

internal class SingleKnot<State : Any, Command : Any>(
    initialState: State,
    commandToStateTransformers: List<CommandToStateTransformer<Command, State>>,
    eventToCommandTransformers: List<SourcedEventToCommandTransformer<Any, Command>>,
    private val disposables: CompositeDisposable = CompositeDisposable()
) : Knot<State, Command> {

    private val stateValue = AtomicReference(initialState)
    private val _command = PublishSubject.create<Command>().toSerialized()
    override val state: Observable<State> = Observable
        .merge(composeToState(commandToStateTransformers))
        .serialize()
        .startWith(initialState)
        .distinctUntilChanged()
        .replay(1)
        .also { disposables.add(it.connect()) }

    init {
        disposables.add(
            Observable
                .merge(composeToCommand(eventToCommandTransformers))
                .subscribe { _command.onNext(it) }
        )
    }

    private fun composeToCommand(
        eventToCommandTransformers: List<SourcedEventToCommandTransformer<Any, Command>>
    ): List<Observable<Command>> = mutableListOf<Observable<Command>>().also { list ->
        for (transformer in eventToCommandTransformers) {
            list += transformer.source.compose(transformer.transformer)
        }
    }

    private fun composeToState(
        commandToStateTransformers: List<CommandToStateTransformer<Command, State>>
    ): List<Observable<State>> = mutableListOf<Observable<State>>().also { list ->
        for (transformer in commandToStateTransformers) {
            list += _command.compose(transformer)
        }
    }

    override val currentState: State get() = stateValue.get()
    override val command: Consumer<Command> = Consumer { _command.onNext(it) }
    override fun dispose() = disposables.dispose()
}
