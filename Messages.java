package erydev.eryStaffDefender.lang;

import erydev.eryStaffDefender.EryStaffDefender;
import erydev.eryStaffDefender.utils.HexUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Messages {

    private final EryStaffDefender plugin;
    private FileConfiguration lang;

    public Messages(EryStaffDefender plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        saveDefaults();
        String code = plugin.getConfig().getString("language", "en");
        File file = new File(new File(plugin.getDataFolder(), "lang"), code + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file '" + code + ".yml' not found, falling back to en.yml");
            file = new File(new File(plugin.getDataFolder(), "lang"), "en.yml");
        }
        lang = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource("lang/" + code + ".yml");
        if (bundled == null) {
            bundled = plugin.getResource("lang/en.yml");
        }
        if (bundled != null) {
            lang.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
    }

    private void saveDefaults() {
        for (String name : new String[]{"en", "ru", "uk", "de", "es", "fr"}) {
            File file = new File(new File(plugin.getDataFolder(), "lang"), name + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + name + ".yml", false);
            }
        }
    }

    public String color(String input) {
        return HexUtils.colorize(input);
    }

    public String raw(String path) {
        return lang.getString(path, "");
    }

    public java.util.List<String> rawList(String path) {
        return lang.getStringList("messages." + path);
    }

    public String prefix() {
        return raw("prefix");
    }

    public String get(String path) {
        return color(prefix() + raw("messages." + path));
    }

    public String get(String path, String key, String value) {
        return color(prefix() + raw("messages." + path).replace(key, value));
    }

    public String plain(String path) {
        return color(raw("messages." + path));
    }

    public String title(String base, String field) {
        return color(raw("titles." + base + "." + field));
    }

    public void send(CommandSender target, String path) {
        target.sendMessage(get(path));
    }

    public void send(CommandSender target, String path, String key, String value) {
        target.sendMessage(get(path, key, value));
    }
}
