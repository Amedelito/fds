package me.amets.mimicperms;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MimicPermsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final String PATH_PERMITIDOS = "permitidos";

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("Mimic") == null) {
            getLogger().severe("No se encontró Mimic, desactivando MimicPerms.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        getCommand("mimic").setExecutor(this);
        getCommand("mimic").setTabCompleter(this);
    }

    // ---------------------------------------------------------------- comandos

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ayuda(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "dar" -> {
                if (!esAdmin(sender)) return denegado(sender);
                if (args.length < 2) { sender.sendMessage("§cUso: /mimic dar <jugador>"); return true; }
                darPermiso(sender, args[1]);
            }
            case "revocar" -> {
                if (!esAdmin(sender)) return denegado(sender);
                if (args.length < 2) { sender.sendMessage("§cUso: /mimic revocar <jugador>"); return true; }
                revocarPermiso(sender, args[1]);
            }
            case "permitidos" -> {
                if (!esAdmin(sender)) return denegado(sender);
                listarPermitidos(sender);
            }
            case "lista" -> {
                if (!autorizado(sender)) return denegado(sender);
                listarCriaturas(sender);
            }
            case "quitar" -> {
                if (!autorizado(sender)) return denegado(sender);
                // Cualquiera quita su propia transformación; el admin puede indicar otro jugador
                if (args.length >= 2 && esAdmin(sender)) {
                    Player objetivo = Bukkit.getPlayerExact(args[1]);
                    if (objetivo == null) { sender.sendMessage("§cEse jugador no está conectado."); return true; }
                    despachar(sender, objetivo, "transformar quitar " + objetivo.getName());
                } else {
                    if (!(sender instanceof Player p)) { sender.sendMessage("§cUsa /mimic quitar <jugador> desde consola."); return true; }
                    despachar(sender, p, "transformar quitar yo");
                }
            }
            default -> {
                // Cualquier otra cosa es el nombre de una criatura
                if (!autorizado(sender)) return denegado(sender);
                if (!(sender instanceof Player p)) { sender.sendMessage("§cEste comando solo lo usa un jugador."); return true; }
                if (!criaturas().contains(sub)) {
                    sender.sendMessage("§cNo existe la criatura §e" + sub + "§c. Usa §e/mimic lista");
                    return true;
                }
                despachar(sender, p, "transformar yo " + sub);
            }
        }
        return true;
    }

    /**
     * Ejecuta un comando de Mimic como si lo hiciera el jugador, dándole el permiso
     * mimic.use solo durante la ejecución. Así no hace falta darle permisos de forma permanente.
     */
    private void despachar(CommandSender sender, Player ejecutor, String comando) {
        // 'sender' se mantiene por claridad de la llamada; el permiso se presta al ejecutor
        PermissionAttachment att = ejecutor.addAttachment(this);
        att.setPermission("mimic.use", true);
        try {
            Bukkit.dispatchCommand(ejecutor, comando);
        } finally {
            ejecutor.removeAttachment(att);
        }
    }

    // ------------------------------------------------------------ permisos

    private void darPermiso(CommandSender sender, String nombre) {
        OfflinePlayer jugador = resolver(nombre);
        if (jugador == null) {
            sender.sendMessage("§cNo conozco a §e" + nombre + "§c. Tiene que haber entrado al servidor al menos una vez.");
            return;
        }
        getConfig().set(PATH_PERMITIDOS + "." + jugador.getUniqueId(), jugador.getName());
        saveConfig();
        sender.sendMessage("§aAhora §e" + jugador.getName() + "§a puede usar /mimic.");
        if (jugador.isOnline() && jugador.getPlayer() != null) {
            jugador.getPlayer().sendMessage("§aYa puedes usar §e/mimic§a.");
        }
    }

    private void revocarPermiso(CommandSender sender, String nombre) {
        OfflinePlayer jugador = resolver(nombre);
        if (jugador == null || !getConfig().contains(PATH_PERMITIDOS + "." + jugador.getUniqueId())) {
            sender.sendMessage("§c" + nombre + " no tiene permiso concedido.");
            return;
        }
        getConfig().set(PATH_PERMITIDOS + "." + jugador.getUniqueId(), null);
        saveConfig();
        sender.sendMessage("§aPermiso de §e" + jugador.getName() + "§a revocado.");
    }

    private void listarPermitidos(CommandSender sender) {
        ConfigurationSection sec = getConfig().getConfigurationSection(PATH_PERMITIDOS);
        if (sec == null || sec.getKeys(false).isEmpty()) {
            sender.sendMessage("§7Nadie tiene permiso concedido.");
            return;
        }
        List<String> nombres = new ArrayList<>();
        for (String uuid : sec.getKeys(false)) nombres.add(sec.getString(uuid, uuid));
        sender.sendMessage("§6Con permiso de /mimic: §e" + String.join("§7, §e", nombres));
    }

    private void listarCriaturas(CommandSender sender) {
        sender.sendMessage("§6Criaturas: §e" + String.join("§7, §e", criaturas()));
    }

    private OfflinePlayer resolver(String nombre) {
        Player online = Bukkit.getPlayerExact(nombre);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(nombre);
    }

    /** OP o jugador con permiso concedido. */
    private boolean autorizado(CommandSender sender) {
        if (!(sender instanceof Player p)) return true;
        if (p.isOp()) return true;
        return getConfig().contains(PATH_PERMITIDOS + "." + p.getUniqueId());
    }

    /** Solo OP o consola. */
    private boolean esAdmin(CommandSender sender) {
        return !(sender instanceof Player p) || p.isOp();
    }

    private boolean denegado(CommandSender sender) {
        sender.sendMessage("§cNo tienes permiso para usar esto.");
        return true;
    }

    private void ayuda(CommandSender sender) {
        sender.sendMessage("§6/mimic <criatura> §7- transformarte");
        sender.sendMessage("§6/mimic quitar §7- volver a tu forma");
        sender.sendMessage("§6/mimic lista §7- ver criaturas");
        if (esAdmin(sender)) {
            sender.sendMessage("§6/mimic dar <jugador> §7- permitir usarlo siempre");
            sender.sendMessage("§6/mimic revocar <jugador> §7- quitar ese permiso");
            sender.sendMessage("§6/mimic permitidos §7- ver quién tiene permiso");
        }
    }

    // ---------------------------------------------------------- utilidades

    private Set<String> criaturas() {
        JavaPlugin mimic = (JavaPlugin) Bukkit.getPluginManager().getPlugin("Mimic");
        ConfigurationSection sec = mimic.getConfig().getConfigurationSection("criaturas");
        return sec == null ? Set.of() : sec.getKeys(false);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opciones = new ArrayList<>(criaturas());
            opciones.add("quitar");
            opciones.add("lista");
            if (esAdmin(sender)) {
                opciones.add("dar");
                opciones.add("revocar");
                opciones.add("permitidos");
            }
            return opciones.stream().filter(o -> o.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && esAdmin(sender)
                && (args[0].equalsIgnoreCase("dar") || args[0].equalsIgnoreCase("revocar")
                    || args[0].equalsIgnoreCase("quitar"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
