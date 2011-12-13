package net.citizensnpcs.questers.quests.types;

import java.util.Map;

import net.citizensnpcs.Settings;
import net.citizensnpcs.permissions.CitizensGroup;
import net.citizensnpcs.permissions.PermissionManager;
import net.citizensnpcs.questers.QuestUtils;
import net.citizensnpcs.questers.quests.Objective;
import net.citizensnpcs.questers.quests.progress.ObjectiveProgress;
import net.citizensnpcs.questers.quests.progress.QuestUpdater;
import net.citizensnpcs.utils.LocationUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDeathEvent;

import com.google.common.collect.Maps;

public class CombatQuest implements QuestUpdater {
	private static final Type[] EVENTS = new Type[] { Type.ENTITY_DEATH };
	private static final Map<Player, KillDetails> playerKills = Maps
			.newHashMap();

	@Override
	public boolean update(Event event, ObjectiveProgress progress) {
		Objective objective = progress.getObjective();
		if (event instanceof EntityDeathEvent) {
			EntityDeathEvent ev = (EntityDeathEvent) event;
			if (!(ev.getEntity() instanceof Player))
				return false;
			Player player = (Player) ev.getEntity();
			String search = objective.getString().toLowerCase();
			boolean found = false, reversed = !search.isEmpty()
					&& search.charAt(0) == '-';
			if (search.contains("*")
					|| search.contains(player.getName().toLowerCase())) {
				found = true;
			} else if (PermissionManager.hasBackend() && search.contains("g:")) {
				// Should be the last else statement, as it needs to do
				// extra processing.
				for (CitizensGroup group : PermissionManager.getGroups(player)) {
					if (search.contains("g:" + group.getName().toLowerCase())) {
						found = true;
						break;
					}
				}
			}
			if (reversed ^ found) {
				KillDetails details = playerKills.get(progress.getPlayer());
				if (details == null
						|| details.getTimes() < Settings
								.getInt("CombatExploitTimes")
						|| !LocationUtils.withinRange(player.getLocation(),
								details.getLocation(),
								Settings.getInt("CombatExploitRadius"))) {
					if (LocationUtils.withinRange(player.getLocation(), objective.getLocation(),
							objective.getAmount())) {
						progress.addAmount(1);
					}
				}
				int times = (details == null || !details.getPlayer().equals(
						player)) ? 1 : details.getTimes() + 1;
				details = new KillDetails(player, player.getLocation(), times);
				playerKills.put(progress.getPlayer(), details);
			}
		}
		return progress.getAmount() >= objective.getAmount();
	}

	@Override
	public Type[] getEventTypes() {
		return EVENTS;
	}

	@Override
	public String getStatus(ObjectiveProgress progress) {
		return QuestUtils.defaultAmountProgress(progress, "players defeated");
	}

	private static class KillDetails {
		private final Player player;
		private final Location loc;
		private final int times;

		KillDetails(Player player, Location loc, int times) {
			this.player = player;
			this.loc = loc;
			this.times = times;

		}

		public Player getPlayer() {
			return player;
		}

		public Location getLocation() {
			return loc;
		}

		public int getTimes() {
			return times;
		}
	}
}
