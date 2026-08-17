package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.model.SortMode;
import com.smirnovlabs.pohteleports.model.Transport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.EnumSet;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/**
 * Renders the {@link PanelModel} as a scannable, collapsible count + gp breakdown.
 *
 * <p>Layout note: every row is {@code name (CENTER) | numbers (EAST, fixed width)}. Putting
 * the name in {@code CENTER} lets it compress and clip instead of overrunning the gp — the
 * old bug came from name and gp both sitting in preferred-width {@code WEST}/{@code EAST}
 * slots, which {@code BorderLayout} never shrinks. The name is an {@link EllipsisLabel},
 * which truncates to whatever width the layout actually gives it (DPI- and scrollbar-proof)
 * and always keeps the full name as its tooltip.
 */
public class PohTeleportPanel extends PluginPanel
{
	private static final Color GOLD = new Color(0xFFD21A);
	private static final Color TAN = new Color(0xE2CD92);
	private static final Color SUB = new Color(0x9FB6D6);
	private static final Color SUB_COUNT = new Color(0x6F7D92);
	private static final Color GREEN = new Color(0x79B34A);
	private static final Color MUTED = new Color(0x8C8C8C);
	private static final Color ZERO = new Color(0x5F5F5F);
	private static final Color DIVIDER = new Color(0x333333);
	private static final Color SEG_ON = new Color(0x2F2F2F);
	private static final Color SEG_OFF = new Color(0x242424);
	private static final Color BG = ColorScheme.DARK_GRAY_COLOR;
	private static final Color HEAD = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color HEAD_HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;
	private static final Color ORANGE = ColorScheme.BRAND_ORANGE;

	// Fixed-width number columns; the flexible name column gets whatever's left.
	private static final int RIGHT_PAD = 8;
	private static final int EAST_W = 68;   // ×count column + gp column
	private static final int COUNT_W = 20;  // fits "×14"
	private static final int GAP = 4;
	private static final int CARET_W = 14;
	private static final int HEADER_LEFT = 8;
	// Level-1 text (direct rows AND item sub-group headers) starts at HEADER_LEFT + CARET_W, so it
	// lines up exactly under the header name — otherwise "Xeric's talisman" and its "Xeric's …" rows
	// sit 2px apart, which the repeated word makes obvious. Sub-group rows indent one level deeper.
	private static final int ROW_INDENT = HEADER_LEFT + CARET_W; // 22
	private static final int SUB_LEFT = HEADER_LEFT + CARET_W;   // 22
	private static final int SUBROW_INDENT = ROW_INDENT + 12;    // 34
	private static final int SUBCOUNT_W = 26;
	private static final int ROW_H = 16;

	private static final String TIMES = "×";        // ×

	private final JPanel body = new JPanel();
	private final Consumer<SortMode> onSort;
	/** Transports the user has folded — remembered in-memory across data refreshes. */
	private final EnumSet<Transport> collapsed = EnumSet.noneOf(Transport.class);

	public PohTeleportPanel(Consumer<SortMode> onSort)
	{
		this.onSort = onSort;
		setLayout(new BorderLayout());
		setBackground(BG);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(BG);
		body.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		// NORTH (not CENTER): the list takes its natural preferred height and is never
		// stretched, so rows keep their full height instead of being clipped by a height cap.
		add(body, BorderLayout.NORTH);
	}

	public void rebuild(PanelModel model)
	{
		SwingUtilities.invokeLater(() ->
		{
			body.removeAll();
			body.add(summary(model));
			body.add(sortControl(model.getSortMode()));
			for (PanelModel.Section sec : model.getSections())
			{
				body.add(section(sec));
			}
			body.revalidate();
			body.repaint();
		});
	}

	// ---- Summary band -------------------------------------------------------

	private JComponent summary(PanelModel model)
	{
		JPanel s = new JPanel(new GridLayout(1, 2, 8, 0));
		s.setBackground(HEAD);
		s.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		s.setAlignmentX(LEFT_ALIGNMENT);
		s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		s.add(stat(QuantityFormatter.quantityToStackSize(model.getTotalCount()), "teleports", Color.WHITE));
		s.add(stat(QuantityFormatter.quantityToStackSize(model.getTotalGp()), "gp saved", GOLD));
		return s;
	}

	private JComponent stat(String value, String caption, Color valueColor)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(HEAD);
		JLabel v = new JLabel(value);
		v.setForeground(valueColor);
		v.setFont(v.getFont().deriveFont(Font.BOLD, 15f));
		v.setAlignmentX(LEFT_ALIGNMENT);
		JLabel k = new JLabel(caption);
		k.setForeground(MUTED);
		k.setFont(k.getFont().deriveFont(10f));
		k.setAlignmentX(LEFT_ALIGNMENT);
		p.add(v);
		p.add(k);
		return p;
	}

	// ---- Segmented sort toggle ---------------------------------------------

	private JComponent sortControl(SortMode active)
	{
		JPanel bar = new JPanel(new GridLayout(1, 2, 1, 0));
		bar.setBackground(DIVIDER); // the 1px grid gap reads as a divider between segments
		bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
		bar.setAlignmentX(LEFT_ALIGNMENT);
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		bar.add(seg("Most used", SortMode.MOST_USED, active));
		bar.add(seg("Most saved", SortMode.MOST_SAVED, active));
		return bar;
	}

	private JLabel seg(String text, SortMode mode, SortMode active)
	{
		boolean on = mode == active;
		JLabel l = new JLabel(text, SwingConstants.CENTER);
		l.setOpaque(true);
		l.setForeground(on ? ORANGE : MUTED);
		l.setBackground(on ? SEG_ON : SEG_OFF);
		l.setFont(l.getFont().deriveFont(on ? Font.BOLD : Font.PLAIN, 11f));
		l.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		l.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				onSort.accept(mode);
			}
		});
		return l;
	}

	// ---- Transport section (collapsible) -----------------------------------

	private JComponent section(PanelModel.Section sec)
	{
		boolean isCollapsed = collapsed.contains(sec.getTransport());

		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setBackground(BG);
		container.setAlignmentX(LEFT_ALIGNMENT);
		container.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(BG);
		content.setAlignmentX(LEFT_ALIGNMENT);
		content.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0)); // keep the last row off the next divider
		for (PanelModel.SubGroup g : sec.getSubGroups())
		{
			int indent = ROW_INDENT;
			if (g.getName() != null)
			{
				content.add(subHeader(g));
				indent = SUBROW_INDENT;
			}
			for (PanelModel.Row r : g.getRows())
			{
				content.add(row(r.getDestination().getDisplayName(), r.getCount(), r.getGp(), indent));
			}
		}
		content.setVisible(!isCollapsed);

		container.add(sectionHeader(sec, isCollapsed, content));
		container.add(content);
		return container;
	}

	private JComponent sectionHeader(PanelModel.Section sec, boolean isCollapsed, JComponent content)
	{
		final Transport t = sec.getTransport();
		final JPanel h = new JPanel(new BorderLayout(GAP, 0));
		h.setBackground(BG);
		h.setBorder(BorderFactory.createEmptyBorder(4, HEADER_LEFT, 4, RIGHT_PAD));
		h.setAlignmentX(LEFT_ALIGNMENT);
		// No height cap. A cap below the header's natural preferred height makes BoxLayout hand the
		// leftover space out differently depending on whether the collapsible content sibling is
		// showing — which made the header text hop vertically on every toggle. Uncapped in NORTH,
		// the header simply takes its own height and stays put.
		h.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		final Caret caret = new Caret(!isCollapsed);

		EllipsisLabel name = new EllipsisLabel();
		name.setForeground(TAN);
		name.setFont(name.getFont().deriveFont(Font.BOLD));
		name.setFullText(t.getDisplayName());

		JPanel west = new JPanel(new BorderLayout(0, 0));
		west.setOpaque(false);
		west.add(caret, BorderLayout.WEST);
		west.add(name, BorderLayout.CENTER);

		h.add(west, BorderLayout.CENTER);
		h.add(numbers(sec.getCount(), sec.getGp(), GREEN, true), BorderLayout.EAST);

		h.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				boolean willCollapse = content.isVisible();
				content.setVisible(!willCollapse);
				caret.setOpen(!willCollapse);
				if (willCollapse)
				{
					collapsed.add(t);
				}
				else
				{
					collapsed.remove(t);
				}
				body.revalidate();
				body.repaint();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				h.setBackground(HEAD_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				h.setBackground(BG);
			}
		});
		return h;
	}

	// ---- Item sub-group header (jewellery box: "Ring of dueling ×2") --------

	private JComponent subHeader(PanelModel.SubGroup g)
	{
		JPanel p = new JPanel(new BorderLayout(GAP, 0));
		p.setBackground(BG);
		p.setBorder(BorderFactory.createEmptyBorder(3, SUB_LEFT, 1, RIGHT_PAD));
		p.setAlignmentX(LEFT_ALIGNMENT);

		EllipsisLabel name = new EllipsisLabel();
		name.setForeground(SUB);
		name.setFont(name.getFont().deriveFont(11f));
		name.setFullText(g.getName());

		JLabel cnt = new JLabel(TIMES + g.getCount(), SwingConstants.RIGHT);
		cnt.setForeground(SUB_COUNT);
		cnt.setFont(cnt.getFont().deriveFont(11f));
		cnt.setPreferredSize(new Dimension(SUBCOUNT_W, ROW_H));

		p.add(name, BorderLayout.CENTER);
		p.add(cnt, BorderLayout.EAST);
		return p;
	}

	// ---- Destination row ----------------------------------------------------

	private JComponent row(String destName, int count, long gp, int indent)
	{
		JPanel p = new JPanel(new BorderLayout(GAP, 0));
		p.setBackground(BG);
		p.setBorder(BorderFactory.createEmptyBorder(1, indent, 1, RIGHT_PAD));
		p.setAlignmentX(LEFT_ALIGNMENT);

		EllipsisLabel name = new EllipsisLabel();
		name.setForeground(Color.LIGHT_GRAY);
		name.setFullText(destName);

		p.add(name, BorderLayout.CENTER);
		p.add(numbers(count, gp, gp == 0 ? ZERO : GREEN, false), BorderLayout.EAST);
		return p;
	}

	/** Fixed-width [ ×count | gp ] block so the gp column always lines up. */
	private JComponent numbers(int count, long gp, Color gpColor, boolean alwaysCount)
	{
		JPanel e = new JPanel(new BorderLayout(GAP, 0));
		e.setOpaque(false);
		e.setPreferredSize(new Dimension(EAST_W, ROW_H));

		JLabel c = new JLabel((alwaysCount || count > 1) ? TIMES + count : "", SwingConstants.RIGHT);
		c.setForeground(MUTED);
		c.setFont(c.getFont().deriveFont(11f));
		c.setPreferredSize(new Dimension(COUNT_W, ROW_H));

		JLabel g = new JLabel(QuantityFormatter.quantityToStackSize(gp), SwingConstants.RIGHT);
		g.setForeground(gpColor);

		e.add(c, BorderLayout.WEST);
		e.add(g, BorderLayout.CENTER);
		return e;
	}

	/**
	 * A disclosure caret painted as one triangle rotated about its own centroid — pointing down
	 * when open, right when collapsed. Because it rotates a single shape in place (rather than
	 * swapping two different font glyphs, which have different centroids), toggling can't shift it
	 * by even a sub-pixel. Fixed size, so it never nudges the header name beside it either.
	 */
	private static final class Caret extends JComponent
	{
		private boolean open;

		Caret(boolean open)
		{
			this.open = open;
			Dimension d = new Dimension(CARET_W, ROW_H);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
		}

		void setOpen(boolean value)
		{
			if (value != open)
			{
				open = value;
				repaint();
			}
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(MUTED);
			g2.translate(getWidth() / 2.0, getHeight() / 2.0);
			if (!open)
			{
				g2.rotate(-Math.PI / 2.0); // same triangle, turned to point right
			}
			// Down-pointing triangle with its centroid at the origin, so rotation pivots in place.
			double halfBase = 4.0;
			double height = 6.0;
			Path2D.Double tri = new Path2D.Double();
			tri.moveTo(-halfBase, -height / 3.0);
			tri.lineTo(halfBase, -height / 3.0);
			tri.lineTo(0.0, 2.0 * height / 3.0);
			tri.closePath();
			g2.fill(tri);
			g2.dispose();
		}
	}

	/**
	 * A {@link JLabel} that truncates its text with an ellipsis to whatever width the layout
	 * gives it, re-fitting on every resize, and keeps the untruncated text as the tooltip.
	 * Its minimum width is zero so a long name never forces the row wider than the panel.
	 */
	private static final class EllipsisLabel extends JLabel
	{
		private String full = "";

		void setFullText(String s)
		{
			full = s == null ? "" : s;
			setToolTipText(full);
			reflow();
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			super.setBounds(x, y, width, height);
			reflow();
		}

		@Override
		public Dimension getMinimumSize()
		{
			Dimension d = super.getMinimumSize();
			return new Dimension(0, d.height);
		}

		private void reflow()
		{
			int avail = getWidth();
			FontMetrics fm = getFontMetrics(getFont());
			if (avail <= 0 || fm.stringWidth(full) <= avail)
			{
				super.setText(full);
				return;
			}
			int ellipsis = fm.stringWidth("…");
			StringBuilder sb = new StringBuilder();
			int w = 0;
			for (int i = 0; i < full.length(); i++)
			{
				int cw = fm.charWidth(full.charAt(i));
				if (w + cw + ellipsis > avail)
				{
					break;
				}
				sb.append(full.charAt(i));
				w += cw;
			}
			super.setText(sb.append('…').toString());
		}
	}
}
