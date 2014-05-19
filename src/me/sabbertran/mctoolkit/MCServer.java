package me.sabbertran.mctoolkit;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MCServer implements Runnable
{

    private String command;
    private Process p;
    private BufferedWriter processInput;
    private String line;
    private boolean serverRunning;
    private MCToolkit main;

    public MCServer(String command, MCToolkit mct)
    {
        this.command = command;
        this.main = mct;
    }

    @Override
    public void run()
    {
        try
        {
            p = Runtime.getRuntime().exec(command);
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    InputStreamReader reader = new InputStreamReader(p.getInputStream());
                    Scanner scan = new Scanner(reader);
                    String text;
                    while (scan.hasNextLine())
                    {
                        text = scan.nextLine();
                        sendMessage(text);
                    }
                    scan.close();
                    try
                    {
                        reader.close();
                    } catch (IOException ex)
                    {
                        Logger.getLogger(MCServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();

            processInput = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            serverRunning = true;
        } catch (IOException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void runMCCommand(String command)
    {
        if (serverRunning)
        {
            try
            {
                processInput.write(command + System.getProperty("line.separator"));
                processInput.flush();
                if (command.equalsIgnoreCase("stop"))
                {
                    serverRunning = false;
                }
            } catch (IOException ex)
            {
                Logger.getLogger(MCServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else
        {
            sendMessage("Minecraft Server is not running");
        }
    }

    public void sendMessage(String msg)
    {
        System.out.println(msg);

        ArrayList<BufferedWriter> removeable = new ArrayList<BufferedWriter>();
        for (BufferedWriter writer : main.getClientWriters())
        {
            try
            {
                writer.write(msg + System.getProperty("line.separator"));
                writer.flush();
            } catch (IOException ex)
            {
                removeable.add(writer);
            }
        }
        for (BufferedWriter writer : removeable)
        {
            main.getClientWriters().remove(writer);
        }
    }

    public void killThread()
    {
        
    }

    public boolean isRunning()
    {
        return serverRunning;
    }
}
