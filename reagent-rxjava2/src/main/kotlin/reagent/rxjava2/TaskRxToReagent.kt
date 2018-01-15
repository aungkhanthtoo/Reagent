package reagent.rxjava2

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import reagent.Task
import io.reactivex.Completable as RxCompletable
import io.reactivex.CompletableObserver as RxCompletableObserver

internal class TaskRxToReagent(private val upstream: RxCompletable) : Task() {
  override suspend fun run() = suspendCancellableCoroutine<Unit> { continuation ->
    upstream.subscribe(object : RxCompletableObserver {
      override fun onSubscribe(d: Disposable) {
        continuation.invokeOnCompletion {
          if (continuation.isCancelled) {
            d.dispose()
          }
        }
      }

      override fun onComplete() {
        continuation.resume(Unit)
      }

      override fun onError(e: Throwable) {
        continuation.resumeWithException(e)
      }
    })
  }
}
