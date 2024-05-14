package net.jojosolos.isstreamerlive.events;

import net.jojosolos.isstreamerlive.IsStreamerLive;
import net.jojosolos.isstreamerlive.commands.CheckStreamerInfo;
import net.jojosolos.isstreamerlive.commands.SetStreamer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkInstance;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Mod.EventBusSubscriber(modid = IsStreamerLive.MODID)
public class ModEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new SetStreamer(event.getDispatcher());
        new CheckStreamerInfo(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event) {
        if(event.isWasDeath()) {
            event.getEntity().getPersistentData().putString(IsStreamerLive.MODID + "streamername",
                    event.getOriginal().getPersistentData().getString(IsStreamerLive.MODID + "streamername"));
        }
    }

//    @SubscribeEvent
//    public static void onPlayerJoin(EntityJoinLevelEvent event) throws IOException {
//        // will reload to see if someone is live? I guess
//        if(!event.getLevel().isClientSide && event.getEntity() instanceof Player) {
//            event.getEntity().sendSystemMessage(Component.literal( event.getEntity().getName().getString() + " has joined").withStyle(ChatFormatting.BOLD));
//            checkLive2(event.getEntity());
//        }
//    }
    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getServer() != null) {
                ForgeEventFactory.getPlayerDisplayName(player, Component.literal(player.getDisplayName().getString()));
//                ForgeEventFactory.getPlayerDisplayName(player, player.getGameProfile().getName());
//                player.getServer().getPlayerList().sendPacketToAllPlayers(new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME));
//                player.sendMessage(new KeybindTextComponent("Display name updated."));
            }
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if(event.getEntity() instanceof Player) {
            if(isChannelLive(event.getEntity()))
                event.setDisplayname(Component.literal("[● LIVE] ").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD)
                    .append(event.getUsername().getString()));
            else
                event.setDisplayname(Component.literal(event.getUsername().getString()).setStyle(Style.EMPTY));
        }
    }

    public static boolean isChannelLive(Entity entity) {
        String channelName = entity.getPersistentData().getString(IsStreamerLive.MODID + "streamername");
        try {
            URL url = new URL("https://www.twitch.tv/" + channelName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                String contents = content.toString();
                return contents.contains("isLiveBroadcast");
            } else {
                LOGGER.info("Error: Unable to connect to Twitch");
            }
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
