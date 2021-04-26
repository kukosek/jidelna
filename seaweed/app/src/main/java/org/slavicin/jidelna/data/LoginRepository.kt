package org.slavicin.jidelna.data

class LoginRepository(val dataSource: LoginDataSource) {
    fun login(username: String, password: String) {
        // handle login_menu
        dataSource.login(username, password)
    }

}

