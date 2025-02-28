package net.jojosolos.isstreamerlive.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.jojosolos.isstreamerlive.IsStreamerLive;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CheckStreamerInfo
{
    public CheckStreamerInfo(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("whoami")
                .executes(commandContext -> checkName(commandContext.getSource())
                )
        );

    }

    private int checkName(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayer();
        assert player != null;

        boolean infoEmpty = player.getPersistentData().getString(IsStreamerLive.MODID + "streamername").isEmpty();

        if (infoEmpty)
        {
            source.sendFailure(Component.literal("streamer name hasn't been set"));
            return -1;
        }
        else
        {
            String e = player.getPersistentData().get(IsStreamerLive.MODID + "streamername").toString();
            source.sendSuccess(() -> Component.literal("streamer name is set and its " + e), true);
            return 1;
        }
    }
}
