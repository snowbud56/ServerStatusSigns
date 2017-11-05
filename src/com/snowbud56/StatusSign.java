package com.snowbud56;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;

public class StatusSign {

    private Location location;
    private Sign sign;
    private String name, ip;
    private int port;

    public StatusSign(Location location, String name, String ip, int port) {
        this.location = location;
        if (location.getBlock().getState() instanceof Sign) this.sign = (Sign) location.getBlock().getState();
        else {
            for (String str : ServerStatusSigns.getInstance().getConfig().getKeys(false)) {
            ConfigurationSection s = ServerStatusSigns.getInstance().getConfig().getConfigurationSection(str);
            ConfigurationSection l = s.getConfigurationSection("loc");
            World w = Bukkit.getServer().getWorld(l.getString("world"));
            double x = l.getDouble("x"), y = l.getDouble("y"), z = l.getDouble("z");
            Location loc = new Location(w, x, y, z);
            if (loc == location) {
                ServerStatusSigns.getInstance().getConfig().set(str, null);
            }
        }
            ServerStatusSigns.getInstance().saveConfig();
        }
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getIP() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void update() {
        //Bukkit.broadcastMessage("Recieved " + name + " request");
        sign.setLine(0, "Updating...");
        sign.setLine(1, name);
        sign.update();
        //Bukkit.broadcastMessage(name + " updated.");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 50);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.write(0xFE);
            StringBuilder str = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) if (b != 0 && b > 16 && b != 255 && b != 23 && b != 24) str.append((char) b);
            String[] data = str.toString().split("§");
            String motd = data[0];
            int onlinePlayers = Integer.valueOf(data[1]);
            int maxPlayers = Integer.valueOf(data[2]);
            sign.setLine(0, ChatColor.GREEN + "[Online]");
            sign.setLine(1, name);
            sign.setLine(2, onlinePlayers + "/" + maxPlayers);
            if (motd.contains("whitelisted")) sign.setLine(3, ChatColor.DARK_RED + "Whitelisted");
            else sign.setLine(3, "");
            socket.close();
        } catch (Exception e) {
            sign.setLine(0, ChatColor.DARK_RED + "[Offline]");
            sign.setLine(1, name);
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
            return;
        }
        sign.update();
    }
}