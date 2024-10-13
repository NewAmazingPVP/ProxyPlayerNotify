package ppn.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import ppn.velocity.VelocityPlayerNotify;

public class ToggleMessages implements SimpleCommand {

    private final VelocityPlayerNotify plugin;

    public ToggleMessages(VelocityPlayerNotify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player) {
            Player player = (Player) source;
            if (plugin.getMessageToggles().contains(player.getUniqueId())) {
                plugin.getMessageToggles().remove(player.getUniqueId());
                player.sendMessage(Component.text("Message notifications toggled on"));
            } else {
                plugin.getMessageToggles().add(player.getUniqueId());
                player.sendMessage(Component.text("Message notifications toggled off"));
            }
        } else {
            source.sendMessage(Component.text("Only players can use this command."));
        }
    }
}