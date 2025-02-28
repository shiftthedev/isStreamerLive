package net.jojosolos.isstreamerlive.events;

import net.jojosolos.isstreamerlive.IsStreamerLive;
import net.jojosolos.isstreamerlive.commands.CheckStreamerInfo;
import net.jojosolos.isstreamerlive.commands.SetStreamer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static net.jojosolos.isstreamerlive.IsStreamerLive.LOGGER;

@Mod.EventBusSubscriber(modid = IsStreamerLive.MODID)
public class ModEvents
{
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event)
    {
        new SetStreamer(event.getDispatcher());
        new CheckStreamerInfo(event.getDispatcher());
    }


    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event)
    {
        if (event.isWasDeath())
        {
            event.getEntity().getPersistentData().putString(IsStreamerLive.MODID + "streamername",
                    event.getOriginal().getPersistentData().getString(IsStreamerLive.MODID + "streamername"));
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer)
        {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            if (player.getServer() != null)
            {
                // if the player that joined hasnt set up their streamer name, automatically set it to their minecraft name
                if (!player.getPersistentData().contains(IsStreamerLive.MODID + "streamername"))
                {
                    player.getPersistentData().putString(IsStreamerLive.MODID + "streamername", player.getDisplayName().getString());
                }

                CompletableFuture.runAsync(() -> {
                    isChannelLive(player);
                    player.refreshDisplayName();
                    player.refreshTabListName();
                });
            }
        }
    }


    // This will format the player's name in the tab list based on whether they are live or not.
    @SubscribeEvent
    public static void onTablistFormat(PlayerEvent.TabListNameFormat event)
    {
        if (event.getEntity().hasCustomName() && event.getEntity().isCustomNameVisible())
        {
            event.setDisplayName(event.getEntity().getCustomName());
        }
    }

    // This will format the player's name in the chat based on whether they are live or not.
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event)
    {
        if (event.getEntity().hasCustomName() && event.getEntity().isCustomNameVisible())
        {
            event.setDisplayname(event.getEntity().getCustomName());
        }
    }

    private static int tick = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        // It is going to check if the player is streaming every 6000 ticks ~ 5 minutes
        if (tick != 6000)
        {
            tick++;
            return;
        }

        tick = 0;

        CompletableFuture.runAsync(() -> {
            event.getServer().getPlayerList().getPlayers().forEach(player -> {
                isChannelLive(player);
                player.refreshDisplayName();
                player.refreshTabListName();
            });
        });
    }


    public static void isChannelLive(Entity entity)
    {
        String channelName = entity.getPersistentData().getString(IsStreamerLive.MODID + "streamername");
        try
        {
            URL url = new URL("https://www.twitch.tv/" + channelName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                {
                    content.append(inputLine);
                }

                in.close();

                String contents = content.toString();
                if (contents.contains("isLiveBroadcast"))
                {
                    entity.setCustomName(Component.literal("● ").withStyle(ChatFormatting.RED).append(entity.getName().getString()));
                    entity.setCustomNameVisible(true);
                }
                else
                {
                    entity.setCustomNameVisible(false);
                }
            }
            else
            {
                LOGGER.info("Error: Unable to connect to Twitch");
            }
            con.disconnect();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
