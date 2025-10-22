package com.retail.dolphinpos.domain.usecases

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GetCurrentTimeUseCase @Inject constructor() {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

    operator fun invoke(): Flow<Pair<String, String>> = flow {
        while (true) {
            val now = Calendar.getInstance().time
            emit(Pair(timeFormat.format(now), dateFormat.format(now)))
            delay(1000) // Update every second
        }
    }
}