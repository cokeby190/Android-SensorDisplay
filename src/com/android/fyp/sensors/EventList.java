package com.android.fyp.sensors;

public class EventList
{
   public State state;
   public long timestamp;

   public void addState(State state)
   {
      this.state = state;
      timestamp = 0;
   }
   
   public void addTimestamp(long timestamp) {
	   this.timestamp = timestamp;
   }
   
}
