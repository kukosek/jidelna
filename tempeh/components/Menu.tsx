import React, {Component} from 'react';
import {DinnerView} from "../components/Dinner"
import {Dinner} from "../data/types"
import {CantryMenu} from "../data/types"


export interface MenuState {
}

export interface MenuViewProps {
	menu: CantryMenu
}

export class MenuView extends Component<MenuViewProps, MenuState> {
	constructor(props: MenuViewProps) {
		super(props)
		this.state = {}
	}
	render() {
		const listItems = this.props.menu.menus.map((dinner: Dinner) =>
			<DinnerView dinner={dinner} key={dinner.name.toString()} />
		);
		return (
			<div className="rounded-xl border mb-2 flex flex-wrap space-x-1">
				<div className="border rounded-xl bg-gradient-to-r from-green-300  flex pl-4 pr-4 pt-2 pb-2">
					<div className="align-middle text-center text-sm text-black mt-auto mb-auto">
						{
							new Intl.DateTimeFormat("cs-CZ", {
								month: "numeric",
								day: "2-digit"
							}).format(new Date(this.props.menu.date))}
					</div>
				</div>
				<div>
					{listItems}
				</div>
			</div>
		)
	}
}

