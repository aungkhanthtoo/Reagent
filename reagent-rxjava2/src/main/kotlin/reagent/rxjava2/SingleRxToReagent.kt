package reagent.rxjava2

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import reagent.Task
import io.reactivex.Single as RxSingle
import io.reactivex.SingleObserver as RxSingleObserver

internal class SingleRxToReagent<I>(private val upstream: RxSingle<I>) : Task<I>() {
  override suspend fun produce() = suspendCancellableCoroutine<I> { continuation ->
    upstream.subscribe(object : RxSingleObserver<I> {
      override fun onSubscribe(d: Disposable) {
        continuation.invokeOnCompletion {
          if (continuation.isCancelled) {
            d.dispose()
          }
        }
      }

      override fun onSuccess(item: I) {
        continuation.resume(item)
      }

      override fun onError(e: Throwable) {
        continuation.resumeWithException(e)
      }
    })
  }
}
