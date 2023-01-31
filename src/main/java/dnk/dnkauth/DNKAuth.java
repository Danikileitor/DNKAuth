package dnk.dnkauth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dnk.dnkauth.mc.WorldAuthSession;
import dnk.dnkauth.mc.commands.RegisterCommandsEventListener;
import dnk.dnkauth.mc.player.InterceptedEvents;
import dnk.dnkauth.mc.player.PlayerLoginEventListener;
import dnk.dnkauth.mc.player.PlayerLogoutEventListener;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("dnkauth")
public class DNKAuth {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public static DNKAuth instance;

    private WorldAuthSession session;

    public WorldAuthSession getSession() {
        return session;
    }

    public DNKAuth() {
        instance = this;

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RegisterCommandsEventListener());
        MinecraftForge.EVENT_BUS.register(new PlayerLoginEventListener());
        MinecraftForge.EVENT_BUS.register(new PlayerLogoutEventListener());
        MinecraftForge.EVENT_BUS.register(new InterceptedEvents());

        session = null;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("Starting authentication middleware");

        session = new WorldAuthSession(5);
    }
}
