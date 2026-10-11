package app.fasting.data

import app.fasting.domain.Fast

internal fun FastEntity.toDomain(): Fast =
    Fast(id, status, startAt, durationMs, endedAt, startNotified, endNotified)

internal fun FastWithCravings.toDomain(): Fast =
    Fast(id, status, startAt, durationMs, endedAt, startNotified, endNotified, cravingsResisted)

internal fun Fast.toEntity(): FastEntity =
    FastEntity(id, status, startAt, durationMs, endedAt, startNotified, endNotified)
