package net.jojosolos.isstreamerlive.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.jojosolos.isstreamerlive.IsStreamerLive;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetStreamer
{
    public SetStreamer(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("iam")
                .then(Commands.argument("streamer name", StringArgumentType.string())
                        .executes(commandContext -> setStreamer(commandContext.getSource(), StringArgumentType.getString(commandContext, "streamer name"))
                        )
                )
        );

    }

    private int setStreamer(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayer();
        assert player != null;
        player.getPersistentData().putString(IsStreamerLive.MODID + "streamername", name);

        source.sendSuccess(() -> Component.literal("Successfully set your streamer name to " + name), true);
        return 1;
    }
}
