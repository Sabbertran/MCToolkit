package me.sabbertran.mctoolkit;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

public class Event extends TimerTask
{

    private MCToolkit main;
    private EventTimer timer;
    private int id;
    private ArrayList<String> cmds;
    private Date time;
    private int period = -1;
    private String type;

    public Event(MCToolkit main, EventTimer timer, int id, ArrayList<String> cmds, Date startTime, int period)
    {
        this.main = main;
        this.timer = timer;
        this.id = id;
        this.cmds = cmds;
        this.time = startTime;
        this.period = period;
        this.type = "repeating";
    }

    public Event(MCToolkit main, EventTimer timer, int id, ArrayList<String> cmds, Date time)
    {
        this.main = main;
        this.timer = timer;
        this.id = id;
        this.cmds = cmds;
        this.time = time;
        this.type = "once";
    }

    @Override
    public void run()
    {
        for (String cmd : this.cmds)
        {
            main.runCommand(null, cmd);
        }
    }

    public ArrayList<String> getCMDs()
    {
        return cmds;
    }

    public Date getTime()
    {
        return time;
    }

    public int getPeriod()
    {
        return period;
    }

    public String getType()
    {
        return type;
    }

    public int getID()
    {
        return id;
    }
}
