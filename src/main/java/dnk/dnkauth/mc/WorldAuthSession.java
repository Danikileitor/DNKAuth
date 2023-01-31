package dnk.dnkauth.mc;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dnk.dnkauth.DNKAuth;
import dnk.dnkauth.auth.Authenticator512;
import dnk.dnkauth.files.AuthStore;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Manages authentication during a server session. Users are unauthenticated when they join; they need to create an account or log in
 * to be able to play.
 * 
 * @author Joe Desmond
 */
public class WorldAuthSession {
	private final int maxLoginTries;
	private final List<String> authenticatedUsers;
	private final Map<String, UserState> unauthenticatedUsers;
	
	private final AuthStore authStore;
	
	public WorldAuthSession(final int _maxLoginTries) {
		maxLoginTries = _maxLoginTries;
		authenticatedUsers = new ArrayList<String>();
		unauthenticatedUsers = new HashMap<String, UserState>();
		
		authStore = new AuthStore(new Authenticator512());
	}
	
	public boolean isAuthenticated(final Player player) {
		final String username = getUsername(player);
		return authenticatedUsers.contains(username);
	}
	
	public void reset(final Player player) {
		final String username = getUsername(player);
		final UserState userState = unauthenticatedUsers.get(username);
		
		if (userState == null) {
			return;
		}
		
		player.moveTo(userState.initialPosition);
	}
	
	public void urgeToAuthenticate(final Player player) {
		final String username = getUsername(player);
		final String message;
		
		if (authStore.userExists(username)) {
			message = "You need to log in, do /login";
		} else {
			message = "You need to set a password, do /createaccount";
		}
		
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.DARK_GRAY));
	}
	
	public void tryResetPassword(final ServerPlayer player, final String newPassword) {
		final String username = getUsername(player);
		
		if (unauthenticatedUsers.containsKey(username)) {
			player.sendSystemMessage(Component.literal("Need to log in first").withStyle(ChatFormatting.DARK_RED));
		} else if (authenticatedUsers.contains(username)) {
			try {
				authStore.changePassword(username, newPassword);
				
				player.sendSystemMessage(Component.literal("Successfully changed password").withStyle(ChatFormatting.GREEN));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				DNKAuth.LOGGER.error("DNK Auth cannot function without SHA512 or SHA256. Please uninstall the plugin or install a native implementation of at least one of these algorithms.");
				System.exit(-1);
			}
		}
	}
	
	public void tryRemoveUser(final CommandSourceStack executor, final String username) {
		try {
			authStore.removeUser(username);
			
			executor.sendSuccess(Component.literal("Successfully removed \"" + username + "\""), false);
		} catch (IllegalArgumentException e) {
			executor.sendFailure(Component.literal("User does not have an account").withStyle(ChatFormatting.DARK_RED));
		}
	}
	
	public void onPlayerConnected(final ServerPlayer player) {
		final String username = getUsername(player);
		final UserState userState = new UserState(player, username);
		
		unauthenticatedUsers.put(username, userState);
	}
	
	private void kickPlayer(final ServerPlayer player, final String reason) {
		player.connection.disconnect(Component.literal(reason));
	}
	
	public boolean onLoginAttempt(final ServerPlayer player, final String password) {
		final String username = getUsername(player);
		
		if (authenticatedUsers.contains(username)) {
			player.sendSystemMessage(Component.literal("Already logged in").withStyle(ChatFormatting.YELLOW));
			return true;
		}
		
		try {
			final boolean wasSuccessful = authStore.login(username, password);
			
			if (wasSuccessful) {
				unauthenticatedUsers.remove(username);
				authenticatedUsers.add(username);
				
				player.sendSystemMessage(Component.literal("Successfully logged in").withStyle(ChatFormatting.GREEN));
				return true;
			}
			
			
			final UserState userState = unauthenticatedUsers.get(username);
			userState.onFailedLoginAttempt();
			
			if (userState.loginTries == maxLoginTries) {
				kickPlayer(player, "Too many failed login attempts (" + maxLoginTries + ")");
				unauthenticatedUsers.remove(username);
				return false;
			}
			
			player.sendSystemMessage(Component.literal("Bad password (max " + maxLoginTries + " attempts)").withStyle(ChatFormatting.DARK_RED));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			DNKAuth.LOGGER.error("DNK Auth cannot function without SHA512 or SHA256. Please uninstall the plugin or install a native implementation of at least one of these algorithms.");
			System.exit(-1);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			
			player.sendSystemMessage(Component.literal("Before doing /login, you need to create a password with /createaccount").withStyle(ChatFormatting.YELLOW));
		}
		
		return false;
	}
	
	public void onCreateAccount(final ServerPlayer player, final String password) {
		final String username = getUsername(player);
		
		if (authStore.userExists(username)) {
			player.sendSystemMessage(Component.literal("Account already exists, try /login").withStyle(ChatFormatting.DARK_RED));
			return;
		}
		
		try {
			authStore.createUser(username, password);
			
			unauthenticatedUsers.remove(username);
			authenticatedUsers.add(username);
			
			player.sendSystemMessage(Component.literal("Successfully created account").withStyle(ChatFormatting.GREEN));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			DNKAuth.LOGGER.error("DNK Auth cannot function without SHA512 or SHA256. Please uninstall the plugin or install a native implementation of at least one of these algorithms.");
			System.exit(-1);
		}
	}
	
	public void onPlayerDisconnected(final ServerPlayer player) {
		final String username = getUsername(player);
		
		authenticatedUsers.remove(username);
		unauthenticatedUsers.remove(username);
	}
	
	private static final String getUsername(final Player player) {
		return player.getGameProfile().getName();
	}
}
