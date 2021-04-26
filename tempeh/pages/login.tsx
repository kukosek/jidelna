import {withRouter, NextRouter} from 'next/router'
import Head from 'next/head'
import React, {ChangeEvent, Component, MouseEventHandler} from 'react';
import {LoginCreds, WithRouterProps} from "../data/types"
import {login, getSettings} from "../data/client"

export interface LoginState {
	isLoading: boolean,
	credentials: LoginCreds
	errorMessage: string,
	error: boolean
}


class Home extends Component<WithRouterProps, LoginState>{
	constructor(props: WithRouterProps) {
		super(props)
		this.state = {
			isLoading: false,
			credentials: {
				username: "", password: ""
			},
			errorMessage: "",
			error: false
		}
	}

	loginSuccess() {
		this.props.router.push("/")
	}
	componentDidMount() {
		getSettings()
			.then(() => {
				this.props.router.push("/")
			})
			.catch((err: Error) => {
				if (err.message == "Unauthorized") {
					console.info("Not logged in")
				} else {
					console.error(err.message)
				}
			})
	}

	login() {
		if (!this.state.isLoading) {
			this.setState({isLoading: true})
			login(this.state.credentials)
				.then(() => {
					this.setState({isLoading: false})
					this.loginSuccess()
				})
				.catch((err: Error) => {
					this.setState({
						isLoading: false,
						error: true,
						errorMessage: err.message
					})
				})
		}
	}

	handleUsernameChange(e: InputEvent) {
		this.setState({credentials: {username: (e.target as HTMLInputElement).value, password: this.state.credentials.password}})
	}

	handlePasswordChange(e: InputEvent) {
		this.setState({credentials: {password: (e.target as HTMLInputElement).value, username: this.state.credentials.username}})
	}

	render() {
		return (
			<div className="container">
				<Head>
					<title>Jídelna: Přihlášení</title>
					<link rel="icon" href="/favicon.ico" />
				</Head>
				<main >
					<div className="flex h-screen w-screen">
						<div className="w-80 rounded-md m-auto bg-white p-5">
							<h1 className="flex-auto text-xl font-semibold">
								Jídelna Slavičín
								</h1>
							<div className="w-full flex-none text-sm font-medium text-gray-500 mt-2">
								Přihlášení
								</div>

							<input
								value={this.state.credentials.username.toString()}
								onChange={this.handleUsernameChange.bind(this)}
								placeholder="Uživatelské jméno"
								className="w-full mt-5 px-4 py-2 border rounded-lg text-gray-700 focus:outline-none focus:border-green-500" />
							<input
								value={this.state.credentials.password.toString()}
								onChange={this.handlePasswordChange.bind(this)}
								type="password" placeholder="Heslo"
								className="w-full mt-2 mb-6 px-4 py-2 border rounded-lg text-gray-700 focus:outline-none focus:border-green-500" />
							<button
								onClick={() => this.login()}
								className="py-2 mb-4 px-4 font-semibold rounded-lg shadow-md text-white bg-green-500 hover:bg-green-700 disabled:opacity-50"
							>
								<div className="flex space-x-2 content-center">
									<span>Přihlásit se</span>
									{this.state.isLoading && <div className="circle circle-white"></div>}
								</div>
							</button>



							{this.state.error && <div role="alert">
								<div className="bg-red-500 text-white font-bold rounded-t px-4 py-2">
									Chyba
							  </div>
								<div className="border border-t-0 border-red-400 rounded-b bg-red-100 px-4 py-3 text-red-700">
									<p>{this.state.errorMessage}</p>
								</div>
							</div>}
						</div>
					</div>
				</main>

			</div>
		)
	}
}

export default withRouter(Home)
