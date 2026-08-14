package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.model.SortMode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/** Renders the {@link PanelModel} as a nested count + gp breakdown with a sort toggle. */
public class PohTeleportPanel extends PluginPanel
{
	private static final Color GOLD = new Color(0xFFD21A);
	private static final Color TAN = new Color(0xE2CD92);
	private static final Color SUB = new Color(0x9FB6D6);
	private static final Color GREEN = new Color(0x79B34A);

	private final JPanel body = new JPanel();

	public PohTeleportPanel(Consumer<SortMode> onSort)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
		tools.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel sortLabel = new JLabel("Sort:");
		sortLabel.setForeground(Color.LIGHT_GRAY);
		tools.add(sortLabel);
		tools.add(sortButton("Most used", SortMode.MOST_USED, onSort));
		tools.add(sortButton("Most saved", SortMode.MOST_SAVED, onSort));

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		add(tools, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
	}

	private JButton sortButton(String text, SortMode mode, Consumer<SortMode> onSort)
	{
		JButton b = new JButton(text);
		b.addActionListener(a -> onSort.accept(mode));
		return b;
	}

	public void rebuild(PanelModel model)
	{
		SwingUtilities.invokeLater(() ->
		{
			body.removeAll();
			body.add(label("Total: " + QuantityFormatter.quantityToStackSize(model.getTotalCount()) + " teleports", Color.WHITE, 0));
			body.add(label("Saved: " + QuantityFormatter.quantityToStackSize(model.getTotalGp()) + " gp", GOLD, 0));
			body.add(Box.createVerticalStrut(6));

			for (PanelModel.Section sec : model.getSections())
			{
				body.add(twoCol(sec.getTransport().getDisplayName() + "  ×" + sec.getCount(),
					QuantityFormatter.quantityToStackSize(sec.getGp()) + " gp", TAN, GREEN, 0));
				for (PanelModel.SubGroup g : sec.getSubGroups())
				{
					int rowIndent = 1;
					if (g.getName() != null)
					{
						body.add(twoCol(g.getName() + " · " + g.getCount(), "", SUB, SUB, 1));
						rowIndent = 2;
					}
					for (PanelModel.Row r : g.getRows())
					{
						body.add(twoCol(r.getDestination().getDisplayName() + "  " + r.getCount(),
							QuantityFormatter.quantityToStackSize(r.getGp()), Color.LIGHT_GRAY, GREEN, rowIndent));
					}
				}
				body.add(Box.createVerticalStrut(4));
			}
			body.revalidate();
			body.repaint();
		});
	}

	private JComponent label(String text, Color color, int indent)
	{
		JLabel l = new JLabel(text);
		l.setForeground(color);
		l.setBorder(BorderFactory.createEmptyBorder(1, indent * 10, 1, 0));
		l.setAlignmentX(LEFT_ALIGNMENT);
		return l;
	}

	private JComponent twoCol(String left, String right, Color leftColor, Color rightColor, int indent)
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setBorder(BorderFactory.createEmptyBorder(1, indent * 10, 1, 0));
		p.setAlignmentX(LEFT_ALIGNMENT);
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		JLabel l = new JLabel(left);
		l.setForeground(leftColor);
		JLabel r = new JLabel(right);
		r.setForeground(rightColor);
		p.add(l, BorderLayout.WEST);
		p.add(r, BorderLayout.EAST);
		return p;
	}
}
