package io.groovin.mcpsample.util

suspend fun<T> suspendRetry(
    retryCount: Int = 2,
    func: suspend () -> T
): T {
    var attempts = 0
    while (attempts < retryCount) {
        try {
            return func()
        } catch (e: Throwable) {
            attempts++
            if (attempts >= retryCount) {
                throw e
            }
        }
    }
    throw RuntimeException("retry error")
}