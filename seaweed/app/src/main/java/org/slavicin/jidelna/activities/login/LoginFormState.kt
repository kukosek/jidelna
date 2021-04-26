package org.slavicin.jidelna.activities.login

/**
 * Data validation state of the login_menu form.
 */
data class LoginFormState(
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)