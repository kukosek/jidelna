import {UserSettings, CantryMenu, LoginCreds} from "./types"

const api = "https://jidelna.techbrick.cz/api"

export async function login(credentials: LoginCreds) {
	return fetch(
		api + '/login',
		{
			method: 'POST',
			body: JSON.stringify(credentials),
			credentials: 'include'
		}
	).then(response => {
		if (!response.ok) {
			throw new Error(response.statusText)
		}
	})
}

export async function logout() {
	return fetch(
		api + '/logout',
		{
			credentials: 'include'
		}
	).then(response => {
		if (!response.ok) {
			throw new Error(response.statusText)
		}
	})
}

export async function getMenu(): Promise<CantryMenu[]> {

	return fetch(api + '/menu',
		{
			credentials: 'include'
		}
	)
		.then(response => {
			if (!response.ok) {
				throw new Error(response.statusText)
			}
			return response.json() as Promise<CantryMenu[]>
		})
}

export async function getSettings(): Promise<UserSettings> {
	return fetch(api + "/settings", {credentials: "include"})
		.then(response => {
			if (!response.ok) {
				throw new Error(response.statusText)
			}
			return response.json() as Promise<UserSettings>
		})
}
