package com.android.fyp.sensors;

public class EventState {
	
	private State state_name;
	
	//initializing state
	public EventState(State state) {
		
		state_name = state;
	}
	
	public State getState() {
		return state_name;
	}
	
	public void changeState(State newstate) {
		state_name = newstate;
	}
	
}
