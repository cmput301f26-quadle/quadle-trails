// Helpers for declaring and reading route arguments in each package's graph.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

// Optional query argument: absent from the route means null.
fun optionalId(name: String): NamedNavArgument = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

fun requiredId(name: String): NamedNavArgument = navArgument(name) { type = NavType.StringType }

fun NavBackStackEntry.optionalArg(name: String): String? = arguments?.getString(name)

fun NavBackStackEntry.requireArg(name: String): String =
    requireNotNull(optionalArg(name)) { "Missing route argument '$name'" }
