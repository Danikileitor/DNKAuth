package dnk.dnkauth.mc.player;

import java.util.List;

import com.mojang.brigadier.context.CommandContextBuilder;
import dnk.dnkauth.DNKAuth;
import dnk.dnkauth.mc.WorldAuthSession;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InterceptedEvents {
	private final List<String> allowedCommands = List.of("/login", "/createaccount");
	
	private void authCheck(final Event event, final Player player) {
		final WorldAuthSession session = DNKAuth.instance.getSession();
		
		if (!session.isAuthenticated(player)) {
			if (event.isCancelable()) {
				event.setCanceled(true);
			}
			
			session.urgeToAuthenticate(player);
			session.reset(player);
		}
	}
	
	@SubscribeEvent
	public void onCommandExecuted(final CommandEvent event) {
		final CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
		final CommandSourceStack source = context.getSource();
		final Entity executor = source.getEntity();
		
		if(!(executor instanceof ServerPlayer)) {
			return;
		}
		
		final ServerPlayer player = (ServerPlayer) executor;
		final String fullCommand = event.getParseResults().getReader().getString();
		
		for (final String allowed : allowedCommands) {
			if (fullCommand.startsWith(allowed)) {
				return;
			}
		}
		
		authCheck(event, player);		
	}
	
	@SubscribeEvent
	public void onPlayerHurt(final LivingHurtEvent event) {
		final Entity entity = event.getEntity();
		
		if (entity instanceof Player) {
			authCheck(event, (Player) entity);
		}
	}
	
	@SubscribeEvent
	public void onPlayerStartTracking(final PlayerEvent.StartTracking event) {
		authCheck(event, event.getEntity());
	}

	@SubscribeEvent
	public void onPlayerInteract(final PlayerInteractEvent event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerRightClickItem(final PlayerInteractEvent.RightClickItem event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerRightClickEmpty(final PlayerInteractEvent.RightClickEmpty event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerRightClickEntitySpecific(final PlayerInteractEvent.EntityInteractSpecific event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerRightClickEntity(final PlayerInteractEvent.EntityInteract event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
		authCheck(event, event.getEntity());
	}
	
	@SubscribeEvent
	public void onPlayerLeftClickEmpty(final PlayerInteractEvent.LeftClickEmpty event) {
		authCheck(event, event.getEntity());
	}
}
