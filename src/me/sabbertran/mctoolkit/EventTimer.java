package me.sabbertran.mctoolkit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;

public class EventTimer
{

    private MCToolkit main;
    private Timer timer;
    private HashMap<Integer, Event> events = new HashMap<Integer, Event>();

    public EventTimer(MCToolkit main)
    {
        this.main = main;
        timer = new Timer();
    }

    public void addRepeatingEvent(ArrayList<String> cmds, Date startTime, long periodinseconds)
    {
        int id = events.size() + 1;
        Event ev = new Event(main, this, id, cmds, startTime, (int) periodinseconds);
        timer.scheduleAtFixedRate(ev, startTime, (periodinseconds * 1000));
        events.put(id, ev);
    }

    public void addOneTimeEvent(ArrayList<String> cmds, Date time)
    {
        int id = events.size() + 1;
        Event ev = new Event(main, this, id, cmds, time);
        timer.schedule(ev, time);
        events.put(id, ev);
    }

    public HashMap<Integer, Event> getAllEvents()
    {
        return events;
    }  

    public Event getEvent(int id)
    {
        return events.get(id);
    }

    public void removeEvent(int id)
    {
        events.remove(id);
    }
}
