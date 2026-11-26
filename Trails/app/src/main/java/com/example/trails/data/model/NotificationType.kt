package com.example.trails.data.model

enum class NotificationType {
    RESERVE_CONFIRM,
    RESERVE_CANCEL,
    LOTTERY_SINGLE,
    LOTTERY_MULTI,
    LOTTERY_LOST,

    /** A message an owner sent to everyone on a lottery's waitlist (US 02.10.01). */
    OWNER_MESSAGE
}