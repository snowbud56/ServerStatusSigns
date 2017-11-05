package com.snowbud56;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;

import com.snowbud56.Utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerStatusSigns extends JavaPlugin implements Listener {

    private ArrayList<StatusSign> signs;
    private static ServerStatusSigns instance;

    @Override
    public void onEnable() {
        instance = this;
        this.signs = new ArrayList<>();
        signs.clear();

        saveDefaultConfig();
        for (String str : getConfig().getKeys(false)) {
            ConfigurationSection s = getConfig().getConfigurationSection(str);

            ConfigurationSection l = s.getConfigurationSection("loc");
            World w = Bukkit.getServer().getWorld(l.getString("world"));
            double x = l.getDouble("x"), y = l.getDouble("y"), z = l.getDouble("z");
            Location loc = new Location(w, x, y, z);

            if (loc.getBlock() == null) {
                getConfig().set(str, null);
            } else {
                signs.add(new StatusSign(loc, s.getString("name"), s.getString("ip"), s.getInt("port")));
                getLogger().info("StatusSign " + s.getString("name") + " updated.");
            }
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (StatusSign s : signs) {
                Material block = s.getLocation().getBlock().getType();
                if (!block.equals(Material.SIGN) && !block.equals(Material.SIGN_POST) && !block.equals(Material.WALL_SIGN)) {
                    //Bukkit.broadcastMessage(s.getName() + " isn't a sign!");
                    for (String str : ServerStatusSigns.getInstance().getConfig().getKeys(false)) {
                        ConfigurationSection sign = ServerStatusSigns.getInstance().getConfig().getConfigurationSection(str);
                        if (sign.getString("name").equals(s.getName())) {
                            ServerStatusSigns.getInstance().getConfig().set(str, null);
                            //Bukkit.broadcastMessage(s.getName() + " removed");
                            signs.remove(s);
                        }
                    }
                    ServerStatusSigns.getInstance().saveConfig();
                }
                s.update();
                //Bukkit.broadcastMessage("Updating " + s.getName());
            }
        }, 0, 20);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    public static ServerStatusSigns getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = e.getClickedBlock();

        if (block.getType() != Material.SIGN && block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) return;

        for (StatusSign s : signs) {
            if (s.getLocation().equals(block.getLocation())) {
                try {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);

                    out.writeUTF("Connect");
                    out.writeUTF(s.getName());

                    e.getPlayer().sendPluginMessage(this, "BungeeCord", b.toByteArray());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create status signs.");
            return true;
        }

        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("statussigns")) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "/statussigns <ip> <port> <name>");
                return true;
            }

            String ip = args[0];
            int port;
            String name = args[2];

            try {
                port = Integer.valueOf(args[1]);
            } catch (Exception e) {
                p.sendMessage(ChatColor.RED + "Port is not a number.");
                return true;
            }

            Block block = p.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 10);
            if (block == null) {
                p.sendMessage(ChatColor.RED + "You are not looking at a sign!");
                return true;
            }

            if (block.getType() != Material.SIGN && block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
                p.sendMessage(ChatColor.RED + "You are not looking at a sign!");
                return true;
            }

            StatusSign statusSign = new StatusSign(block.getLocation(), name, ip, port);
            signs.add(statusSign);
            save(statusSign);
            Sign sign = (Sign) statusSign.getLocation().getBlock().getState();
            sign.setLine(0, "Updating...");
            sign.setLine(1, statusSign.getName());
            sign.update();
        }

        if (commandLabel.equalsIgnoreCase("signs")) {
            p.sendMessage(ChatUtils.format("&cSigns:"));
            for (StatusSign sign : signs) {
                p.sendMessage(ChatUtils.format("&7- &c" + sign.getName() + "&7 at &c(" + sign.getLocation().getX() + ", " + sign.getLocation().getY() + ", " + sign.getLocation().getZ() + ")&7 with ip:port of &c" + sign.getIP() + ":" + sign.getPort()));
            }
        }

        return true;
    }

    private void save(StatusSign sign) {
        int size = getConfig().getKeys(false).size() + 1;
        getConfig().set(size + ".loc.world", sign.getLocation().getWorld().getName());
        getConfig().set(size + ".loc.x", sign.getLocation().getX());
        getConfig().set(size + ".loc.y", sign.getLocation().getY());
        getConfig().set(size + ".loc.z", sign.getLocation().getZ());
        getConfig().set(size + ".name", sign.getName());
        getConfig().set(size + ".ip", sign.getIP());
        getConfig().set(size + ".port", sign.getPort());
        saveConfig();
    }
}