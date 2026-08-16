package com.smirnovlabs.pohteleports;

import com.google.inject.Provides;
import com.smirnovlabs.pohteleports.cost.SavingsValuator;
import com.smirnovlabs.pohteleports.detect.CacheView;
import com.smirnovlabs.pohteleports.detect.ClientCacheView;
import com.smirnovlabs.pohteleports.detect.DetectionRouter;
import com.smirnovlabs.pohteleports.detect.GameStateView;
import com.smirnovlabs.pohteleports.detect.JewelleryBoxRecognizer;
import com.smirnovlabs.pohteleports.detect.MenuInteraction;
import com.smirnovlabs.pohteleports.detect.MountedAmuletRecognizer;
import com.smirnovlabs.pohteleports.detect.NexusCatalog;
import com.smirnovlabs.pohteleports.detect.NexusRecognizer;
import com.smirnovlabs.pohteleports.detect.PohGameIds;
import com.smirnovlabs.pohteleports.detect.TeleportRecognizer;
import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import com.smirnovlabs.pohteleports.store.TeleportSavingsStore;
import com.smirnovlabs.pohteleports.ui.PanelModel;
import com.smirnovlabs.pohteleports.ui.PohTeleportPanel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "PoH Teleport Counter",
	description = "Counts your free Player-Owned House teleports and the runes/charges they save",
	tags = {"poh", "teleport", "nexus", "jewellery", "construction"}
)
public class PohTeleportCounterPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ConfigManager configManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private PohTeleportConfig config;

	private final TeleportSavingsStore store = new TeleportSavingsStore();
	private final NexusCatalog nexusCatalog = new NexusCatalog();
	private SavingsValuator valuator;
	private CacheView cacheView;
	private DetectionRouter router;
	private PohTeleportPanel panel;
	private NavigationButton navButton;
	private boolean catalogWarned;

	@Provides
	PohTeleportConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(PohTeleportConfig.class);
	}

	@Override
	protected void startUp()
	{
		store.load(configManager);
		valuator = new SavingsValuator(itemManager::getItemPrice);
		cacheView = new ClientCacheView(client);

		GameStateView state = new GameStateView()
		{
			@Override
			public int getVarbit(int id)
			{
				return id < 0 ? 0 : client.getVarbitValue(id);
			}

			@Override
			public boolean isInPoh()
			{
				// Until POH regions are captured (live QA), leave the gate open so
				// object-scoped transports can be verified; once populated it tightens.
				if (PohGameIds.POH_REGIONS.length == 0)
				{
					return true;
				}
				int[] regions = client.getMapRegions();
				if (regions == null)
				{
					return false;
				}
				for (int r : PohGameIds.POH_REGIONS)
				{
					for (int cur : regions)
					{
						if (cur == r)
						{
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public int currentTick()
			{
				return client.getTickCount();
			}

			@Override
			public int[] playerPos()
			{
				Player p = client.getLocalPlayer();
				if (p == null)
				{
					return null;
				}
				WorldPoint wp = p.getWorldLocation();
				return wp == null ? null : new int[]{wp.getX(), wp.getY(), wp.getPlane()};
			}
		};

		// Order matters: object-scoped and armed recognizers before name-only ones.
		// Mounted glory must precede the jewellery box (they share names like "Edgeville").
		List<TeleportRecognizer> recognizers = Arrays.asList(
			new MountedAmuletRecognizer(PohGameIds.MOUNTED_XERICS_OBJECT, PohGameIds.MOUNTED_XERICS_DEFAULT_VARBIT,
				Transport.MOUNTED_XERICS, byName(Transport.MOUNTED_XERICS), varbitDefault(Transport.MOUNTED_XERICS)),
			new MountedAmuletRecognizer(PohGameIds.MOUNTED_DIGSITE_OBJECT, PohGameIds.MOUNTED_DIGSITE_DEFAULT_VARBIT,
				Transport.MOUNTED_DIGSITE, byName(Transport.MOUNTED_DIGSITE), varbitDefault(Transport.MOUNTED_DIGSITE)),
			new MountedAmuletRecognizer(PohGameIds.MOUNTED_GLORY_OBJECT, -1, Transport.MOUNTED_GLORY,
				byName(Transport.MOUNTED_GLORY), varbitDefault(Transport.MOUNTED_GLORY)),
			new NexusRecognizer(PohGameIds.NEXUS_OBJECT, varbitDefault(Transport.NEXUS), byName(Transport.NEXUS), nexusCatalog),
			new JewelleryBoxRecognizer(byName(Transport.JEWELLERY_BOX)));

		router = new DetectionRouter(recognizers, state, ev ->
		{
			store.record(ev);
			store.persist(configManager);
			refresh();
		}, msg ->
		{
			if (config.debugLogMenus())
			{
				log.info("[POH-CC] {}", msg);
			}
		});

		panel = new PohTeleportPanel(mode ->
		{
			configManager.setConfiguration(TeleportSavingsStore.GROUP, "sortMode", mode.name());
			refresh();
		});
		navButton = NavigationButton.builder()
			.tooltip("PoH Teleport Counter")
			.icon(ImageUtil.loadImageResource(getClass(), "/poh_teleport_counter_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		refresh();
	}

	@Override
	protected void shutDown()
	{
		store.persist(configManager);
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		MenuInteraction mi = new MenuInteraction(strip(e.getMenuOption()), strip(e.getMenuTarget()), e.getId());
		if (config.debugLogMenus() && !mi.optionLower().equals("walk here"))
		{
			log.info("[POH-CC] option='{}' target='{}' id={} regions={}",
				mi.getOption(), mi.getTarget(), mi.getId(), Arrays.toString(client.getMapRegions()));
		}
		router.onMenuInteraction(mi); // arm/route FIRST — detection is never gated on the catalog

		// Catalog is best-effort: load it AFTER routing, and never let it disturb counting.
		try
		{
			boolean wasLoaded = nexusCatalog.isLoaded();
			nexusCatalog.ensureLoaded(cacheView); // lazy: builds once the cache is up, then a cheap boolean
			if (!wasLoaded && nexusCatalog.isLoaded())
			{
				// One-shot (fires once per session, no debug flag needed). If this logs
				// e.g. "45 names (461=Kharyrll, 855=Civitas illa Fortis)" the researched
				// cache ids are correct. Absence after a few clicks => ids need adjusting.
				log.info("[POH-CC] nexus catalog loaded: {} names (461={}, 855={})",
					nexusCatalog.size(), nexusCatalog.name(461), nexusCatalog.name(855));
			}
		}
		catch (RuntimeException ex)
		{
			if (!catalogWarned)
			{
				catalogWarned = true;
				log.warn("[POH-CC] nexus catalog load failed (counting unaffected)", ex);
			}
		}
	}

	private static String strip(String s)
	{
		return s == null ? "" : Text.removeTags(s);
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		// Confirms/expires an armed teleport by the player's coordinate jump.
		router.onGameTick();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (config.debugLogVarbits())
		{
			log.info("[POH-CC] varbit={} varp={} value={}", e.getVarbitId(), e.getVarpId(), e.getValue());
		}
	}

	private void refresh()
	{
		// ItemManager (via the valuator) must be read on the client thread; the panel
		// then marshals its own Swing update onto the EDT.
		clientThread.invokeLater(() ->
			panel.rebuild(PanelModel.build(store.snapshot(), valuator, config.sortMode())));
	}

	/** Lowercased display-name -&gt; Destination for one transport (excludes the unknown bucket). */
	private static Map<String, Destination> byName(Transport t)
	{
		Map<String, Destination> m = new HashMap<>();
		for (Destination d : Destination.values())
		{
			if (d.getTransport() == t && !d.getId().endsWith(":unknown"))
			{
				m.put(d.getDisplayName().toLowerCase(Locale.ROOT), d);
			}
		}
		return m;
	}

	/**
	 * Varbit value -&gt; Destination for a transport's configured default. Empty until the
	 * real varbit encodings are captured in live QA; empty is safe (falls back to the
	 * transport's Unknown bucket).
	 */
	private static Map<Integer, Destination> varbitDefault(Transport t)
	{
		return Collections.emptyMap();
	}
}
