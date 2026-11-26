package com.example.trails.ui.testing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trails.BuildConfig
import com.example.trails.R
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Role
import com.example.trails.ui.profile.IdentityGate

/** Release launches directly; debug waits for an explicit testing role. */
@Composable
fun TestingRoleGate(
    enabled: Boolean = BuildConfig.TESTING_TOOLS,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val controller: TestingRoleViewModel = viewModel {
        TestingRoleViewModel(createSavedStateHandle())
    }
    val state by controller.state.collectAsState()
    if (state.ready) {
        // The real device identity goes through Welcome / Welcome back first (US 01.09.01).
        if (state.usesDevice) IdentityGate { content() } else content()
    } else {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                TestingRoleDialog(state, controller::selectRole, controller::selectThisDevice)
            }
        }
    }
}

/**
 * The "Choose your testing role" dialog: three sample profiles plus this device's real sign-in.
 *
 * @param state loading and error state.
 * @param onSelect called with the sample role picked.
 * @param onThisDevice called when "This device · Firebase sign-in" is picked.
 */
@Composable
fun TestingRoleDialog(state: TestingRoleState, onSelect: (Role) -> Unit, onThisDevice: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.testing_role_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.testing_role_body))
                listOf(
                    Role.ENTRANT to R.string.testing_role_camper,
                    Role.OWNER to R.string.testing_role_owner,
                    Role.ADMIN to R.string.testing_role_admin
                ).forEach { (role, label) ->
                    OutlinedButton(
                        onClick = { onSelect(role) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().testTag("testing_role_${role.name}")
                    ) { Text(stringResource(label)) }
                }
                OutlinedButton(
                    onClick = onThisDevice,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().testTag("testing_role_DEVICE")
                ) { Text(stringResource(R.string.testing_role_this_device)) }
                if (state.loading) CircularProgressIndicator()
                if (state.failed) {
                    Text(stringResource(R.string.testing_role_error), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {}
    )
}

/** A fresh Activity discards navigation and ViewModels belonging to the previous user. */
@Composable
fun rememberTestingRoleRestart(): (() -> Unit)? {
    if (!BuildConfig.TESTING_TOOLS || !CurrentUser.canUseSampleUsers) return null
    val activity = LocalContext.current.findActivity() ?: return null
    return {
        activity.startActivity(Intent(activity, activity.javaClass))
        activity.finish()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
