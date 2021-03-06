package reagent.operator

import reagent.Emitter
import reagent.Observable
import reagent.runTest
import reagent.source.observableOf
import reagent.source.test.emptyActualObservable
import reagent.source.test.toActualObservable
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

@Ignore
class ObservableIteratorTest {
  @Test fun single() = runTest {
    val items = mutableListOf<String>()
    for (item in observableOf("Hello")) {
      items.add(item)
    }
    assertEquals(listOf("Hello"), items)
  }

  @Test fun multiple() = runTest {
    val items = mutableListOf<String>()
    for (item in observableOf("Hello", "World")) {
      items.add(item)
    }
    assertEquals(listOf("Hello", "World"), items)
  }

  @Test fun empty() = runTest {
    for (item in emptyActualObservable<String>()) {
      fail()
    }
  }

  @Test fun error() = runTest {
    val exception = RuntimeException()
    try {
      for (item in exception.toActualObservable<String>()) {
        fail()
      }
      fail()
    } catch (actual: Throwable) {
      assertSame(exception, actual)
    }
  }

  @Test fun iteratorContract() = runTest {
    var called = 0
    val task = object : Observable<String>() {
      override suspend fun subscribe(emit: Emitter<String>) {
        called++
        emit("Hello")
        emit("World")
      }
    }

    val iterator = task.iterator()
    assertEquals(0, called)

    assertTrue(iterator.hasNext())
    assertEquals("Hello", iterator.next())
    assertEquals(1, called)

    assertTrue(iterator.hasNext())
    assertEquals("World", iterator.next())

    assertFalse(iterator.hasNext())
    try {
      iterator.next()
    } catch (e: IllegalStateException) {
      assertEquals("Must call hasNext() before next()", e.message)
    }
    assertEquals(1, called)
  }

  @Test fun iteratorContractNextOnly() = runTest {
    var called = 0
    val task = object : Observable<String>() {
      override suspend fun subscribe(emit: Emitter<String>) {
        called++
        emit("Hello")
        emit("World")
      }
    }

    val iterator = task.iterator()
    assertEquals(0, called)

    assertEquals("Hello", iterator.next())
    assertEquals(1, called)

    assertEquals("World", iterator.next())

    try {
      iterator.next()
    } catch (e: IllegalStateException) {
      assertEquals("Must call hasNext() before next()", e.message)
    }
    assertEquals(1, called)
  }
}
