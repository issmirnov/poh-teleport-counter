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
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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

	// Mounted-amulet "Teleport menu": the destination is picked in the generic MENU_NEW (947)
	// option interface, which fires a WIDGET_CONTINUE carrying the destination as widget text
	// (no named MenuOptionClicked). When such a menu is opened we remember the amulet object so
	// the following pick can be attributed to it.
	private int mountedMenuObjectId = -1;
	private int mountedMenuUntilTick = Integer.MIN_VALUE;

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
				// A POH — your own OR a guest's — is an instanced region, so this is the
				// reliable signal. Guest houses use different map regions than your own
				// (e.g. [8302,8303] vs [8046,8047]), which a fixed region allowlist misses.
				// The region list stays as a belt-and-braces fallback.
				if (client.isInInstancedRegion())
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
			log.info("[POH-CC] option='{}' target='{}' id={} p0={} p1={} action={} inst={} regions={}",
				mi.getOption(), mi.getTarget(), mi.getId(), e.getParam0(), e.getParam1(), e.getMenuAction(),
				client.isInInstancedRegion(), Arrays.toString(client.getMapRegions()));
		}
		// A mounted amulet's "Teleport menu" opens the generic MENU_NEW option interface;
		// remember the amulet so the subsequent pick can be attributed to it.
		if ((e.getId() == PohGameIds.MOUNTED_XERICS_OBJECT || e.getId() == PohGameIds.MOUNTED_DIGSITE_OBJECT)
			&& mi.optionLower().contains("teleport") && mi.optionLower().contains("menu"))
		{
			mountedMenuObjectId = e.getId();
			mountedMenuUntilTick = client.getTickCount() + 30; // generous: reading the menu takes a few seconds
		}

		// The pick in that menu is a WIDGET_CONTINUE on MENU_NEW (947) carrying the destination
		// as the widget's text. Synthesize the equivalent direct object-click so the normal
		// mounted recognizer resolves it by name; the coord-jump then confirms the count.
		MenuInteraction toRoute = mi;
		if (e.getMenuAction() == MenuAction.WIDGET_CONTINUE
			&& client.getTickCount() <= mountedMenuUntilTick
			&& (e.getParam1() >>> 16) == InterfaceID.MENU_NEW)
		{
			Widget w = e.getWidget();
			String picked = w == null ? "" : strip(w.getText());
			mountedMenuUntilTick = Integer.MIN_VALUE; // consume the menu, matched or not
			if (!picked.isEmpty())
			{
				toRoute = new MenuInteraction(picked, mi.getTarget(), mountedMenuObjectId);
			}
			if (config.debugLogMenus())
			{
				log.info("[POH-CC] mounted-menu pick: text='{}' -> click id={} child={}",
					picked, mountedMenuObjectId, e.getParam1() & 0xFFFF);
			}
		}
		router.onMenuInteraction(toRoute); // arm/route — detection is never gated on the catalog

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
