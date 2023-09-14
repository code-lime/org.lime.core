package org.lime.system.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.lime.system.execute.Func1;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class NearUtils {
    public static Stream<? extends Player> getPlayerList(Location location, double distance) {
        World world = location.getWorld();
        return Bukkit
                .getOnlinePlayers()
                .stream()
                .filter(other -> other.getWorld() == world && other.getLocation().distance(location) <= distance);
    }
    public static Player getNearPlayer(List<Player> list, Location location) {
        return getNearPlayer(list, location, null, null);
    }
    public static Player getNearPlayer(List<Player> list, Location location, Double minDistance, Func1<Player, Boolean> filter) {
        Player result = null;
        double lastDistance = minDistance == null ? Double.MAX_VALUE : minDistance;
        for(Player p : list) {
            if (p.getWorld() != location.getWorld()) continue;
            double distance = location.distance(p.getLocation());
            if(distance < lastDistance) {
                if (filter != null && !filter.invoke(p)) continue;
                lastDistance = distance;
                result = p;
            }
        }
        return result;
    }

    public static Player getNearPlayer(Map<Player, Location> map, Location location) {
        return getNearPlayer(map, location, null, null);
    }
    public static Player getNearPlayer(Map<Player, Location> map, Location location, Double minDistance, Func1<Player, Boolean> filter) {
        if (location == null) return null;
        Player result = null;
        double lastDistance = minDistance == null ? Double.MAX_VALUE : minDistance;
        for(Map.Entry<Player, Location> kv : map.entrySet()) {
            Location l = kv.getValue();
            if (l.getWorld() != location.getWorld()) continue;
            double distance = location.distance(l);
            if(distance < lastDistance) {
                Player p = kv.getKey();
                if (filter != null && !filter.invoke(p)) continue;
                lastDistance = distance;
                result = p;
            }
        }
        return result;
    }
}
