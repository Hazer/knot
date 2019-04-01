package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentStateMutation {

    object CountUpCommand
    data class CountUpChange(val value: Int)

    data class State(val counter: Int = 0) {
        override fun toString(): String = "State: $counter"
    }

    private lateinit var knot: Knot<State, CountUpCommand, CountUpChange>

    @Test
    fun `Serialize concurrent state updates`() {

        val latch = CountDownLatch(2 * COUNT)
        val countUpEmitter1 = Observable
            .create<CountUpCommand> { emitter ->
                for (i in 1..COUNT) {
                    Thread.sleep(10)
                    emitter.onNext(CountUpCommand)
                    latch.countDown()
                }
            }
            .subscribeOn(Schedulers.newThread())

        val countUpEmitter2 = Observable
            .create<Unit> { emitter ->
                for (i in 1..COUNT) {
                    Thread.sleep(10)
                    emitter.onNext(Unit)
                    latch.countDown()
                }
            }
            .subscribeOn(Schedulers.newThread())

        knot = knot {
            state {
                initial = State()
                reduce { change, state ->
                    effect(state.copy(counter = state.counter + change.value))
                }
            }

            on<CountUpCommand> { countUpEmitter1.map { CountUpChange(1) } }
            onEvent { countUpEmitter2.map { CountUpChange(100) } }
        }

        latch.await(3, TimeUnit.SECONDS)

        val observer = knot.state.test()
        observer.assertValues(
            State(COUNT * 100 + COUNT)
        )
    }

}

private const val COUNT = 30