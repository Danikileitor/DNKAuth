package dnk.dnkauth.mc.player;

import dnk.dnkauth.DNKAuth;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLogoutEventListener {
	
	@SubscribeEvent
	public void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
		final Player player = event.getEntity();
		
		if (!(player instanceof ServerPlayer)) {
			return;
		}
		
		final ServerPlayer serverPlayer = (ServerPlayer) player;
		
		DNKAuth.instance.getSession().onPlayerDisconnected(serverPlayer);
	}
}
