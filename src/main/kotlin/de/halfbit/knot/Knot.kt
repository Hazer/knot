package de.halfbit.knot

import de.halfbit.knot.dsl.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference

interface Knot<State : Any, Command : Any> {
    val state: Observable<State>
    val command: Consumer<Command>
    fun dispose()
}

@Suppress("UNCHECKED_CAST")
internal class DefaultKnot<State : Any, Command : Any>(
    initialState: State,
    commandUpdateStateTransformers: List<OnCommandUpdateStateTransformer<Command, State>>,
    commandToCommandTransformers: List<TypedCommandToCommandTransformer<Command, Command, State>>,
    eventUpdateStateTransformers: List<OnEventUpdateStateTransformer<*, State>>,
    eventToCommandTransformers: List<OnEventToCommandTransformer<*, Command, State>>,
    private val disposables: CompositeDisposable = CompositeDisposable()
) : Knot<State, Command> {

    private val stateValue = AtomicReference(initialState)
    private val withState = object : WithStateReduce<State> {
        override val state: State get() = stateValue.get()
        override fun reduce(reducer: Reducer<State>): Reducer<State> = reducer
    }

    private val _command = PublishSubject.create<Command>().toSerialized()
    private val _state: Observable<State> = Observable
        .merge(transformers(commandUpdateStateTransformers, eventUpdateStateTransformers))
        .serialize()
        .map {
            val state = it.invoke(withState)
            stateValue.set(state)
            state
        }
        .startWith(initialState)
        .distinctUntilChanged()
        .replay(1)
        .also { disposables.add(it.connect()) }

    override val state: Observable<State> = _state
    override val command: Consumer<Command> = Consumer { _command.onNext(it) }

    init {
        disposables.add(
            Observable
                .merge(
                    mutableListOf<Observable<Command>>().also { list ->
                        for (transformer in commandToCommandTransformers) {
                            list += _command
                                .ofType(transformer.type.javaObjectType)
                                .compose<Command> { transformer.transform(withState, it) }
                        }
                        for (transformer in eventToCommandTransformers) {
                            list += transformer.source
                                .compose<Command> {
                                    val transform = transformer.transform as EventToCommandTransform<*, Command, State>
                                    transform(withState, it)
                                }
                        }
                    }
                )
                .subscribe { _command.onNext(it) }
        )
    }

    private fun transformers(
        commandUpdateStateTransformers: List<OnCommandUpdateStateTransformer<Command, State>>,
        eventUpdateStateTransformers: List<OnEventUpdateStateTransformer<*, State>>
    ): List<Observable<Reducer<State>>> =
        mutableListOf<Observable<Reducer<State>>>().also { list ->
            for (transformer in commandUpdateStateTransformers) {
                list += _command
                    .ofType(transformer.type.javaObjectType)
                    .compose<Reducer<State>> { transformer.transform(withState, it) }
            }
            for (transformer in eventUpdateStateTransformers) {
                list += transformer.source
                    .compose<Reducer<State>> {
                        val transform = transformer.transform as OnEventUpdateState<*, State>
                        transform(withState, it)
                    }
            }
        }

    override fun dispose() {
        disposables.clear()
    }
}

fun <State : Any, Command : Any> tieKnot(
    block: KnotBuilder<State, Command>.() -> Unit
): Knot<State, Command> =
    KnotBuilder<State, Command>()
        .also(block)
        .build()

