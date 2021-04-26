import {NextRouter} from 'next/router'
export interface WithRouterProps {
	router: NextRouter
}
export interface Dinner {
	type: string,
	menuNumber: number,
	name: string,
	allergens: number[],
	status: string
}
export interface CantryMenu {
	date: string,
	menus: Dinner[]
}

export interface LoginCreds {username: String, password: String}

export interface AllergensConfig {
	loveList: number[],
	blackList: number[]
}

export interface AutoorderConfig {
	randomOrders: boolean,
	prefferedMenuNumber: number,
	allergens: AllergensConfig,
	orderUncomplying: boolean
}

export interface AutoorderRequestConfig {
	orderAll: boolean,
	orderDaysInAdvance: number
}

export interface AutoorderSetting {
	enable: boolean,
	config: AutoorderConfig,
	requestConfig: AutoorderRequestConfig
}

export interface UserSettings {
	autoorder: AutoorderSetting
}
export enum Status {
	ORDERED = "ordered",
	ORDERED_CLOSED = "ordered closed",
	ORDERING = "ordering",
	CANCELLING_ORDER = "cancelling order",
	AVAILABLE = "available",
	AUTOORDER = "autoordered",
	UNAVAILABLE = "unavailable"
}

export function isStatusOrdered(status: string): boolean {
	switch (status) {
		case Status.ORDERED:
			return true
		case Status.ORDERED_CLOSED:
			return true
		case Status.ORDERING:
			return true
		case Status.CANCELLING_ORDER:
			return false
		case Status.AUTOORDER:
			return true
		default:
			return false
	}
}
