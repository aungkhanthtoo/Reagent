package reagent.tester

import com.google.common.truth.Subject
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import reagent.Observable
import reagent.Task
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.DeprecationLevel.ERROR
import kotlin.test.assertEquals
import kotlin.test.fail

class RecordingRule : TestRule {
  private val recorders = mutableListOf<Recorder<*, *>>()

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        try {
          base.evaluate()
          recorders.forEach { it.assertExhausted() }
        } finally {
          recorders.clear()
        }
      }
    }
  }

  fun <I, S : Subject<S, I>> observable(asserter: (I) -> S) = ObservableRecorder(asserter).also { recorders.add(it) }
  fun <I, S : Subject<S, I>> one(asserter: (I) -> S) = TaskRecorder(asserter).also { recorders.add(it) }
}

abstract class Recorder<I, S : Subject<S, I>>(
  private val asserter: (I) -> S
) {
  private val events = LinkedBlockingDeque<Any>()

  protected fun event(event: Any) {
    events.addLast(event)
  }

  open fun assertItem(): S {
    val item = events.pollFirst(1, SECONDS) ?: fail("Timed out waiting for complete.")
    if (item is Item<*>) {
      @Suppress("UNCHECKED_CAST")
      return asserter(item.item as I)
    }
    fail("Expected item but was $item")
  }

  open fun assertComplete() {
    val item = events.pollFirst(1, SECONDS) ?: fail("Timed out waiting for complete.")
    assertEquals(Complete, item)
  }

  fun assertError(): ThrowableSubject {
    val item = events.pollFirst(1, SECONDS) ?: fail("Timed out waiting for complete.")
    if (item is Error) {
      return assertThat(item.t)
    }
    fail("Expected error but was $item")
  }

  fun assertExhausted() {
    assertThat(events).isEmpty()
  }
}

class ObservableRecorder<I, S : Subject<S, I>>(
  asserter: (I) -> S
) : Recorder<I, S>(asserter), Observable.Observer<I> {
  override fun onNext(item: I) = event(Item(item))
  override fun onComplete() = event(Complete)
  override fun onError(t: Throwable) = event(Error(t))
}

class TaskRecorder<I, S : Subject<S, I>>(
  asserter: (I) -> S
) : Recorder<I, S>(asserter), Task.Observer<I> {
  override fun onItem(item: I) = event(Item(item))
  override fun onError(t: Throwable) = event(Error(t))

  @Deprecated("Task does not have complete events.", level = ERROR)
  override fun assertComplete() = fail()
}
