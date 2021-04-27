import {withRouter} from 'next/router'
import Head from 'next/head'
import List from "../components/List"
import {getMenu, logout} from "../data/client"
import {CantryMenu, WithRouterProps} from "../data/types"
import React, {Component} from 'react';
import {MenuView} from '../components/Menu'


interface HomeState {
	menus: CantryMenu[],
	isLoading: boolean
}

class Home extends Component<WithRouterProps, HomeState> {
	constructor(props: WithRouterProps) {
		super(props)
		this.state = {
			menus: [],
			isLoading: false
		}
	}
	logout() {
		logout()
			.then(() => {
				this.props.router.push("/login")
			})
			.catch((err) => {
				if (err.message == "401")
					this.props.router.push("/login")
			})
	}
	componentDidMount() {
		const cachedMenus: CantryMenu[] = JSON.parse(localStorage.getItem("menus"))
		if (cachedMenus) {
			this.setState({menus: cachedMenus})
		}
		this.setState({isLoading: true})
		getMenu()
			.then((menus) => {
				this.setState({menus: menus})
				this.setState({isLoading: false})
				localStorage.setItem("menus", JSON.stringify(menus))
			})
			.catch((err: Error) => {
				this.setState({isLoading: false})
				if (err.message == "401")
					this.props.router.push("/login")
			})
	}
	render() {
		const listItems = this.state.menus.map((menu: CantryMenu) =>
			<MenuView menu={menu} key={menu.date.toString()} />
		);
		return (
			<div className="container">
				<Head>
					<title>Jídelna Slavičín</title>
					<link rel="icon" href="/favicon.ico" />
				</Head>
				<div className="flex h-screen w-screen m-auto">
					<div className="w-auto rounded-md m-auto bg-white p-5">
						<div className="flex p-0 mb-4 items-center flex-wrap space-x-4">
							<div className="flex-auto text-3xl font-semibold">
								Jídelna Slavičín
							</div>
							{this.state.isLoading &&
								<div className="flex-shrink circle circle-green"></div>}
							<button
								onClick={() => this.logout()}
								className="py-2 px-4 font-semibold rounded-lg shadow-md text-white bg-green-500 hover:bg-green-700 disabled:opacity-50"
							>
								Odhlásit se
							</button>
						</div>
						{listItems}
					</div>
				</div>
			</div>
		)
	}
}
export default withRouter(Home)
