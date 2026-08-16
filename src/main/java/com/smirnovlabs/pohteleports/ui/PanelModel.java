package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.cost.SavingsValuator;
import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.SortMode;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pure view-model for the panel: turns the store snapshot into a sorted,
 * nested tree (transport -&gt; item sub-group -&gt; destination rows). No Swing,
 * no RuneLite — fully unit-testable.
 */
public final class PanelModel
{
	public static final class Row
	{
		private final Destination destination;
		private final String displayName;
		private final int count;
		private final long gp;

		Row(Destination destination, String displayName, int count, long gp)
		{
			this.destination = destination;
			this.displayName = displayName;
			this.count = count;
			this.gp = gp;
		}

		public Destination getDestination()
		{
			return destination;
		}

		/** Label to show — cache-authoritative for nexus, the enum name otherwise. */
		public String getDisplayName()
		{
			return displayName;
		}

		public int getCount()
		{
			return count;
		}

		public long getGp()
		{
			return gp;
		}
	}

	public static final class SubGroup
	{
		private final String name; // null = flat (no item sub-level)
		private final int count;
		private final long gp;
		private final List<Row> rows;

		SubGroup(String name, int count, long gp, List<Row> rows)
		{
			this.name = name;
			this.count = count;
			this.gp = gp;
			this.rows = rows;
		}

		public String getName()
		{
			return name;
		}

		public int getCount()
		{
			return count;
		}

		public long getGp()
		{
			return gp;
		}

		public List<Row> getRows()
		{
			return rows;
		}
	}

	public static final class Section
	{
		private final Transport transport;
		private final int count;
		private final long gp;
		private final List<SubGroup> subGroups;

		Section(Transport transport, int count, long gp, List<SubGroup> subGroups)
		{
			this.transport = transport;
			this.count = count;
			this.gp = gp;
			this.subGroups = subGroups;
		}

		public Transport getTransport()
		{
			return transport;
		}

		public int getCount()
		{
			return count;
		}

		public long getGp()
		{
			return gp;
		}

		public List<SubGroup> getSubGroups()
		{
			return subGroups;
		}
	}

	private final int totalCount;
	private final long totalGp;
	private final List<Section> sections;

	private PanelModel(int totalCount, long totalGp, List<Section> sections)
	{
		this.totalCount = totalCount;
		this.totalGp = totalGp;
		this.sections = sections;
	}

	public int getTotalCount()
	{
		return totalCount;
	}

	public long getTotalGp()
	{
		return totalGp;
	}

	public List<Section> getSections()
	{
		return sections;
	}

	public static PanelModel build(Map<Destination, Integer> snapshot, SavingsValuator valuator, SortMode sort)
	{
		return build(snapshot, valuator, sort, Destination::getDisplayName);
	}

	/**
	 * @param displayNameFn resolves each destination's shown label — the plugin passes the
	 *                      cache-authoritative nexus name (falling back to the enum name).
	 */
	public static PanelModel build(Map<Destination, Integer> snapshot, SavingsValuator valuator, SortMode sort,
		Function<Destination, String> displayNameFn)
	{
		// Compare on [count, gp], descending, keyed by the chosen sort mode.
		Comparator<long[]> byMode = (a, b) -> sort == SortMode.MOST_SAVED
			? Long.compare(b[1], a[1])
			: Long.compare(b[0], a[0]);

		int totalCount = 0;
		long totalGp = 0;
		List<Section> sections = new ArrayList<>();

		for (Transport t : Transport.values())
		{
			Map<String, List<Row>> bySub = new LinkedHashMap<>(); // subGroup name ("" = flat) -> rows
			int secCount = 0;
			long secGp = 0;
			for (Map.Entry<Destination, Integer> e : snapshot.entrySet())
			{
				Destination d = e.getKey();
				int c = e.getValue();
				if (d.getTransport() != t || c == 0)
				{
					continue;
				}
				long gp = valuator.gpTotal(d, c);
				String sub = d.getSubGroup() == null ? "" : d.getSubGroup();
				bySub.computeIfAbsent(sub, k -> new ArrayList<>()).add(new Row(d, displayNameFn.apply(d), c, gp));
				secCount += c;
				secGp += gp;
			}
			if (secCount == 0)
			{
				continue;
			}
			List<SubGroup> groups = new ArrayList<>();
			for (Map.Entry<String, List<Row>> g : bySub.entrySet())
			{
				List<Row> rows = g.getValue();
				rows.sort((r1, r2) -> byMode.compare(new long[]{r1.count, r1.gp}, new long[]{r2.count, r2.gp}));
				int gc = 0;
				long gg = 0;
				for (Row r : rows)
				{
					gc += r.count;
					gg += r.gp;
				}
				groups.add(new SubGroup(g.getKey().isEmpty() ? null : g.getKey(), gc, gg, rows));
			}
			groups.sort((s1, s2) -> byMode.compare(new long[]{s1.count, s1.gp}, new long[]{s2.count, s2.gp}));
			sections.add(new Section(t, secCount, secGp, groups));
			totalCount += secCount;
			totalGp += secGp;
		}
		sections.sort((s1, s2) -> byMode.compare(new long[]{s1.count, s1.gp}, new long[]{s2.count, s2.gp}));
		return new PanelModel(totalCount, totalGp, sections);
	}
}
