package com.android.fyp.sensors;

import java.util.EnumSet;
import java.util.HashMap;

public class EventState {
	
	private static State currentState;
	private static HashMap stateMap;
	
	static {
		
		stateMap = new HashMap<State, EnumSet<State>>();
		
		stateMap.put(State.ACC, EnumSet.of(State.CONST, State.DEC));
		stateMap.put(State.DEC, EnumSet.of(State.CONST, State.STOP, State.ACC));
		stateMap.put(State.CONST, EnumSet.of(State.DEC, State.ACC));
		stateMap.put(State.STOP, EnumSet.of(State.ACC));
		
		currentState = State.STOP;
	}
	
	public static boolean checkTransit(State expected) {
		EnumSet<State> check = (EnumSet<State>) stateMap.get(currentState);
		if(check.contains(expected))
			return true;
		else
			return false;
	}
	
	public static void setCurrent(State current) {
		if(checkTransit(current))
			currentState = current;
	}
	
	public static State getState() {
		return currentState;
	}
}
