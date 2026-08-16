package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.cost.SavingsValuator;
import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.SortMode;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PanelModelTest
{
	private SavingsValuator valuator()
	{
		return new SavingsValuator(id -> 60); // every item = 60
	}

	@Test
	public void sectionsSortByGpWhenMostSaved()
	{
		Map<Destination, Integer> snap = new EnumMap<>(Destination.class);
		snap.put(Destination.NEXUS_VARROCK, 3);    // 5 runes * 60 * 3 = 900
		snap.put(Destination.MGLORY_EDGEVILLE, 1); // (60/6) * 1 = 10
		PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_SAVED);
		assertEquals(Transport.NEXUS, m.getSections().get(0).getTransport());
		assertEquals(4, m.getTotalCount());
	}

	@Test
	public void jewelleryBoxNestsBySubGroup()
	{
		Map<Destination, Integer> snap = new EnumMap<>(Destination.class);
		snap.put(Destination.JBOX_GLORY_EDGEVILLE, 2);
		snap.put(Destination.JBOX_DUEL_FEROX, 5);
		PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_USED);
		PanelModel.Section jbox = m.getSections().stream()
			.filter(s -> s.getTransport() == Transport.JEWELLERY_BOX)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(2, jbox.getSubGroups().size());
		assertEquals("Ring of dueling", jbox.getSubGroups().get(0).getName()); // 5 > 2
	}

	@Test
	public void rowUsesTheDisplayNameResolver()
	{
		// Stage B: the plugin passes cache-authoritative nexus names; identity is unchanged.
		Map<Destination, Integer> snap = new EnumMap<>(Destination.class);
		snap.put(Destination.NEXUS_KHARYRLL, 1);
		PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_USED,
			d -> d == Destination.NEXUS_KHARYRLL ? "Canifis (Kharyrll)" : d.getDisplayName());
		PanelModel.Row row = m.getSections().get(0).getSubGroups().get(0).getRows().get(0);
		assertEquals("Canifis (Kharyrll)", row.getDisplayName());
		assertEquals(Destination.NEXUS_KHARYRLL, row.getDestination());
	}

	@Test
	public void defaultBuildUsesEnumDisplayName()
	{
		Map<Destination, Integer> snap = new EnumMap<>(Destination.class);
		snap.put(Destination.NEXUS_VARROCK, 1);
		PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_USED); // 3-arg overload
		assertEquals("Varrock", m.getSections().get(0).getSubGroups().get(0).getRows().get(0).getDisplayName());
	}

	@Test
	public void flatTransportsHaveSingleAnonymousSubGroup()
	{
		Map<Destination, Integer> snap = new EnumMap<>(Destination.class);
		snap.put(Destination.MGLORY_EDGEVILLE, 1);
		PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_USED);
		PanelModel.Section g = m.getSections().get(0);
		assertEquals(1, g.getSubGroups().size());
		assertNull(g.getSubGroups().get(0).getName());
		assertEquals(Destination.MGLORY_EDGEVILLE, g.getSubGroups().get(0).getRows().get(0).getDestination());
	}
}
