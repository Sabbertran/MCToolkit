package me.sabbertran.mctoolkit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MCToolkit
{
    private String command;
    private MCServer srv;
    private EventTimer evTimer;
    private Thread serverThread;
    private ServerSocket socket;
    private Socket client;
    private ArrayList<BufferedWriter> clientWriters = new ArrayList<BufferedWriter>();
    private File config;
    private Properties config_prop;
    private int port;
    private String password;
    private String mcjar;
    private int ram;
    private String sql_host;
    private String sql_port;
    private String sql_database;
    private String sql_user;
    private String sql_password;
    private static String sql_url;
    private static Connection con = null;

    public MCToolkit()
    {

        config = new File("./toolkit_config.properties");
        if (!config.exists())
        {
            InputStream configIn = getClass().getResourceAsStream("toolkit_config.properties");
            copy(configIn, config);
            try
            {
                configIn.close();
            } catch (IOException ex)
            {
                Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        config_prop = new Properties();
        try
        {
            InputStream config_propIn = new BufferedInputStream(new FileInputStream("toolkit_config.properties"));
            config_prop.load(config_propIn);

            this.port = Integer.parseInt(config_prop.getProperty("Toolkit_Port"));
            this.password = config_prop.getProperty("Toolkit_Password");
            this.mcjar = config_prop.getProperty("Minecraft_Server_Jar");
            this.ram = Integer.parseInt(config_prop.getProperty("Allocated_RAM"));
            this.sql_host = config_prop.getProperty("SQL_Adress");
            this.sql_port = config_prop.getProperty("SQL_Port");
            this.sql_database = config_prop.getProperty("SQL_Database");
            this.sql_user = config_prop.getProperty("SQL_Username");
            this.sql_password = config_prop.getProperty("SQL_Password");

            config_propIn.close();
        } catch (IOException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        File dir = new File("./toolkit_sessions");
        if (!dir.exists())
        {
            dir.mkdirs();
        }

        try
        {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            this.sql_url = "jdbc:mysql://" + sql_host + ":" + sql_port + "/" + sql_database + "?user=" + this.sql_user + "&password=" + this.sql_password;
            this.con = DriverManager.getConnection(sql_url);
        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
        }

        evTimer = new EventTimer(this);

        command = "java -Xms" + ram + "M -Xmx" + ram + "M -XX:MaxPermSize=128M -jar " + mcjar + " nogui";
        srv = new MCServer(command, this);
        serverThread = new Thread(srv);
        serverThread.start();

        //Command line reader
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                BufferedReader cmdline = new BufferedReader(new InputStreamReader(System.in));
                String text = "";
                while (true)
                {
                    try
                    {
                        text = cmdline.readLine();
                    } catch (IOException ex)
                    {
                        Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    runCommand(null, text);
                }
            }
        }).start();

        //Socket reader
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    socket = new ServerSocket(port);
                    while (true)
                    {
                        client = socket.accept();
                        handleClient(client);
                    }
                } catch (IOException ex)
                {
                    Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    private void closeToolkit()
    {
        System.exit(0);
    }

    public void runCommand(Socket c, String cmd)
    {
        String[] cmdsplit = cmd.split(" ");
        if (cmd != null)
        {
            switch (cmdsplit[0])
            {
                case ".end":
                    srv.runMCCommand("say Stopping Toolkit...");
                    srv.runMCCommand("save-all");
                    srv.runMCCommand("stop");
                    closeToolkit();
                    break;
                case ".start":
                    boolean skipcheck = false;
                    if (cmdsplit.length == 2 && cmdsplit[1].equals("-i"))
                    {
                        skipcheck = true;
                    }
                    if (!srv.isRunning() || skipcheck)
                    {
                        srv = new MCServer(command, this);
                        serverThread = new Thread(srv);
                        serverThread.start();
                    } else
                    {
                        srv.sendMessage("Sever is already running!");
                    }
                    break;
                case ".event":
                    switch (cmdsplit[1])
                    {
                        case "add":
                            DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                            Date time = null;
                            try
                            {
                                time = format.parse(cmdsplit[3]);
                            } catch (ParseException ex)
                            {
                                Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            ArrayList<String> cmds = new ArrayList<String>();
                            int period;

                            switch (cmdsplit[2])
                            {
                                case "once":
                                    for (int i = 4; i < cmdsplit.length; i++)
                                    {
                                        cmds.add(cmdsplit[i]);
                                    }
                                    evTimer.addOneTimeEvent(cmds, time);
                                    break;
                                case "repeating":
                                    for (int i = 5; i < cmdsplit.length; i++)
                                    {
                                        cmds.add(cmdsplit[i]);
                                    }
                                    period = Integer.parseInt(cmdsplit[4]);
                                    evTimer.addRepeatingEvent(cmds, time, period);
                                    break;
                            }
                            break;
                        case "get":
                            if (c != null)
                            {
                                for (Map.Entry<Integer, Event> entry : evTimer.getAllEvents().entrySet())
                                {
                                    String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(entry.getValue().getTime());
                                    String type = entry.getValue().getType();
                                    String commands = "";
                                    int id = entry.getValue().getID();
                                    for (String command : entry.getValue().getCMDs())
                                    {
                                        commands = commands + command + " ";
                                    }

                                    String send = "";
                                    switch (type)
                                    {
                                        case "once":
                                            send = ".event " + id + " " + type + " " + date + " " + commands + " ";
                                            break;
                                        case "repeating":
                                            int periode = entry.getValue().getPeriod();
                                            send = ".event " + id + " " + type + " " + date + " " + periode + " " + commands + " ";
                                            break;
                                    }
                                    sendToClient(c, send);
                                }
                            }
                            break;
                    }
                    break;
                default:
                    srv.runMCCommand(cmd);
                    break;
            }
        }
    }

    public void handleClient(final Socket c)
    {
        final String password = this.password;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                boolean running = true;

                String state = "auth";
                String pcname = "No PC Name found";
                String username = "No Username found";
                try
                {
                    Scanner scan = new Scanner(new InputStreamReader(c.getInputStream()));

                    String text;
                    while (running)
                    {
                        if (scan.hasNextLine())
                        {
                            text = scan.nextLine();
                            String[] t = text.split(":");
                            if (t[0].equals(".login"))
                            {
                                if (state.equals("auth"))
                                {
                                    String sha1pw = toSHA1(t[2]);
                                    if (t.length == 5)
                                    {
                                        pcname = t[3];
                                        username = t[4];
                                    }
                                    try
                                    {
                                        Statement st = con.createStatement();
                                        ResultSet rs = st.executeQuery("SELECT tk_acce FROM wh_mc_member WHERE u_name='" + t[1] + "' AND u_pass='" + sha1pw + "' AND tk_acce='true'");
                                        st.close();
                                        if (rs.next())
                                        {
                                            addClientWriter(c);
                                            state = "valid";
                                            sendToClient(c, "Successfully logged in");
                                            createSessionLog(true, client.getInetAddress().toString(), pcname, username, t[1], t[2]);
                                        } else
                                        {
                                            sendToClient(c, "Wrong password");
                                            createSessionLog(false, client.getInetAddress().toString(), pcname, username, t[1], t[2]);
                                        }
                                    } catch (SQLException ex)
                                    {
                                        Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                } else
                                {
                                    sendToClient(c, "Already logged in");
                                }
                            } else if (state.equals("valid"))
                            {
                                runCommand(c, text);
                            } else
                            {
                                sendToClient(c, "You are not logged in. Please log in to send commands");
                            }
                        }
                    }
                } catch (IOException ex)
                {
                    running = false;
                }
            }
        }).start();
    }

    private void sendToClient(Socket c, String text)
    {
        try
        {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
            bw.write(text + System.getProperty("line.separator"));
            bw.flush();

        } catch (IOException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addClientWriter(Socket c)
    {
        BufferedWriter bw = null;
        try
        {
            bw = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
            clientWriters.add(bw);
        } catch (IOException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createSessionLog(boolean success, String IP, String pcname, String pcuser, String username, String pw)
    {
        File folder = new File("./toolkit_sessions");
        File[] files = folder.listFiles();
        if (files.length >= 50)
        {
            File firstModified = files[0];
            for (int i = 1; i < files.length; i++)
            {
                if (firstModified.lastModified() > files[i].lastModified())
                {
                    firstModified = files[i];
                }
            }
            firstModified.delete();

        }

        Date d = new Date();
        String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(new Date());
        File newFile = new File("./toolkit_sessions/" + date + ".session");
        try
        {
            PrintWriter writer = new PrintWriter(newFile);
            writer.write("Success: " + success + System.getProperty("line.separator"));
            writer.write("IP: " + IP + System.getProperty("line.separator"));
            writer.write("Username: " + username + System.getProperty("line.separator"));
            writer.write("Used Password: " + pw + System.getProperty("line.separator"));
            writer.write("Computer Name: " + pcname + System.getProperty("line.separator"));
            writer.write("Computer User: " + pcuser + System.getProperty("line.separator"));
            writer.flush();
            writer.close();

        } catch (IOException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<BufferedWriter> getClientWriters()
    {
        return clientWriters;
    }

    public MCServer getCurrentServer()
    {
        return srv;
    }

    private void copy(InputStream in, File file)
    {
        if (in != null && file != null)
        {
            try
            {
                OutputStream out = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private String toSHA1(String convert)
    {
        byte[] byteconvert = convert.getBytes();
        MessageDigest md = null;
        try
        {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        byte[] finalbyte = md.digest(byteconvert);

        String result = "";
        for (int i = 0; i < finalbyte.length; i++)
        {
            result +=
                    Integer.toString((finalbyte[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static void main(String[] args)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            sql_url = "jdbc:mysql://everlanche.com:3306/d019ae82?user=d019ae82&password=LPsHuJh4dNUw3X2F";
            con = DriverManager.getConnection(sql_url);
        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex)
        {
            Logger.getLogger(MCToolkit.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error");
        }
        MCToolkit tk = new MCToolkit();
    }
}
