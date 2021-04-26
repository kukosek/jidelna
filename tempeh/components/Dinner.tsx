import React, {Component} from 'react';
import {Dinner, isStatusOrdered} from "../data/types"


export interface DinnerState {
	isLoading: boolean
}

export interface DinnerViewProps {
	dinner: Dinner
}

export class DinnerView extends Component<DinnerViewProps, DinnerState> {
	constructor(props: DinnerViewProps) {
		super(props)
		this.state = {isLoading: false}
	}
	render() {
		return (
			<article className="p-2 flex flex-wrap space-x-2">
				<div className="flex-shrink">
					{this.state.isLoading && <div className="circle circle-blue"></div>}
					{!this.state.isLoading &&
						<input checked={isStatusOrdered(this.props.dinner.status)} type="checkbox" className="rounded" />}
				</div>
				<div className="flex-auto">
					<span className="text-sm text-black mb-0.5">
						{this.props.dinner.name}
					</span>
				</div>
			</article>
		)
	}
}

