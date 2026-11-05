// Routes for the app-shell screens every role reaches from the bottom bar.
// OWNER: Zainab (zsaghir) — App shell
// Outstanding issues: none; screens are registered in ui/shell/ShellNavigation.kt.
package com.example.trails.ui.navigation

/** Routes for the bottom-bar tabs that go to the same screen for every role. */
object ShellRoutes {
    /** The user's notifications list. */
    const val NOTIFICATIONS = "notifications"

    /** The user's messages. */
    const val MESSAGES = "messages"

    /** App settings; profile and app policy are reached from here. */
    const val SETTINGS = "settings"
}
