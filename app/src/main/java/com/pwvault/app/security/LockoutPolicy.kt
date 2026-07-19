package com.pwvault.app.security

/** 30s * 2^6 = 32min already exceeds [SecurityPolicy.MAX_LOCKOUT_DURATION] — no need to go higher. */
private const val MAX_DOUBLING_EXPONENT = 6

/**
 * Shared wrong-attempt counter for Master Password + PIN unlock (biometric failures don't count —
 * the OS already rate-limits those at the HAL level). See
 * docs/plans/feature-04-autolock-lockout-plan.md.
 */
class LockoutPolicy(
    private val store: LockoutStore,
) {
    /** `null` if not currently locked out. */
    fun currentLockoutUntilMillis(): Long? {
        val lockedUntil = store.getLockedUntilMillis()
        return if (lockedUntil > System.currentTimeMillis()) lockedUntil else null
    }

    fun recordSuccess() {
        store.save(attemptCount = 0, lockedUntilMillis = 0L)
    }

    /** Records a wrong Master Password/PIN attempt, locking out once the threshold is reached. */
    fun recordFailure() {
        val attemptCount = store.getAttemptCount() + 1
        if (attemptCount < SecurityPolicy.MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            store.save(attemptCount, lockedUntilMillis = 0L)
            return
        }
        val exponent = (attemptCount - SecurityPolicy.MAX_ATTEMPTS_BEFORE_LOCKOUT).coerceAtMost(MAX_DOUBLING_EXPONENT)
        val duration =
            (SecurityPolicy.INITIAL_LOCKOUT_DURATION * (1 shl exponent))
                .coerceAtMost(SecurityPolicy.MAX_LOCKOUT_DURATION)
        val lockedUntilMillis = System.currentTimeMillis() + duration.inWholeMilliseconds
        store.save(attemptCount, lockedUntilMillis)
    }
}
