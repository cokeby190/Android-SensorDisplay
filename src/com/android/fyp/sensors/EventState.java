package com.android.fyp.sensors;

import java.util.EnumSet;
import java.util.HashMap;

import android.text.format.Time;

public class EventState {
	
	private static State currentState;
	private static State currentDir;
	private static HashMap stateMap;
	private static HashMap dirMap;
	
	private static long start_ts;
	//private Time stop_ts;
	
	static {
		
		stateMap = new HashMap<State, EnumSet<State>>();
		dirMap = new HashMap<State, EnumSet<State>>();
		
		stateMap.put(State.ACC, EnumSet.of(State.CONST, State.DEC));
		stateMap.put(State.DEC, EnumSet.of(State.CONST, State.STOP, State.ACC));
		stateMap.put(State.CONST, EnumSet.of(State.DEC, State.ACC));
		stateMap.put(State.STOP, EnumSet.of(State.ACC));
		
		dirMap.put(State.LEFT, EnumSet.of(State.STRAIGHT));
		dirMap.put(State.RIGHT, EnumSet.of(State.STRAIGHT));
		dirMap.put(State.STRAIGHT, EnumSet.of(State.LEFT, State.RIGHT));
		
		currentState = State.STOP;
		currentDir = State.STRAIGHT;
	}
		
	public static boolean checkTransit(State expected) {
		EnumSet<State> check = (EnumSet<State>) stateMap.get(currentState);
		if(check.contains(expected))
			return true;
		else
			return false;
	}
	
	public static boolean checkDir(State expected) {
		EnumSet<State> check = (EnumSet<State>) dirMap.get(currentDir);
		if(check.contains(expected))
			return true;
		else
			return false;
	}
	
	public static void setCurrent(State current, long curr_time) {
		
		currentState = current;
		start_ts = curr_time;
		
	}
	
	public static void setDir(State direction) {
		currentDir = direction;
	}
	
	public static State getState() {
		return currentState;
	}
	
	public static State getDir() {
		return currentDir;
	}
	
	public static long getStartTs() {
		return start_ts;
	}
}
