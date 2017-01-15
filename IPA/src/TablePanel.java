/**
 * Apapted to show only the table grid
 * by Menzo Windhouwer
 *
 * Explicit Table Builder
 * Copyright (C) 2004  Andrew Pietsch
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id: DebugPanel.java,v 1.6 2004/11/28 06:54:23 pietschy Exp $
 */

package org.pietschy.explicit;


import com.zookitec.layout.ExplicitLayout;
import org.pietschy.explicit.style.Margin;

import java.awt.*;
import java.util.Iterator;


/**
 * An extension of {@link ScrollablePanel} that provides a debug view of the layout.  This panel
 * renders row and column padding.
 *<p>
 * Example:
 * <pre>   TableBuilder tb = new TableBuilder(<b>new TablePanel()</b>);</pre>
 */
public class TablePanel extends ScrollablePanel {
    private TableBuilder builder;

    private Color colorTopPadding;
    private Color colorBottomPadding;

    public TablePanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public TablePanel(LayoutManager layout) {
        super(layout);
    }

    public TablePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public TablePanel() {}

    public void setBuilder(TableBuilder builder, Color top, Color bottom) {
        this.builder = builder;
        colorTopPadding = top;
        colorBottomPadding = bottom;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintDebug(g);
    }

    private void paintDebug(Graphics g) {
        ExplicitLayout layout = builder.getLayout();

        Color originalColor = g.getColor();
        Insets insets = null;

        if (getBorder() != null) {
            insets = getBorder().getBorderInsets(this);
        } else {
            insets = new Insets(0, 0, 0, 0);
        }

        // now take our margin into account..
        Margin margin = builder.margin();

        insets = new Insets((int) (insets.top + margin.top().getValue(layout)),
                (int) (insets.left + margin.left().getValue(layout)),
                (int) (insets.bottom + margin.bottom().getValue(layout)),
                (int) (insets.right + margin.right().getValue(layout)));

        int rowleft = insets.left;
        int rowWidth = getWidth() - insets.left - insets.right;

        // draw the row lines...
        for (Iterator iterator = builder.rows().iterator(); iterator.hasNext();) {
            Row row = (Row) iterator.next();
            double top = row.top().getValue(layout);
            double paddingHeight = row.paddingTop().getValue(layout);

            g.setColor(colorTopPadding);
            g.fillRect(rowleft, (int) top, rowWidth, (int) paddingHeight);

            paddingHeight = row.paddingBottom().getValue(layout);
            double bottom = row.bottom().getValue(layout);

            g.setColor(colorBottomPadding);
            g.fillRect(rowleft, (int) (bottom - paddingHeight), rowWidth,
                    (int) paddingHeight);
        }

        int colTop = insets.top;
        int colHeight = getHeight() - colTop - insets.bottom;

        // draw the column lines...
        for (Iterator iterator = builder.columns().iterator(); iterator.hasNext();) {
            Column column = (Column) iterator.next();
            double left = column.left().getValue(layout);
            double paddingWidth = column.paddingLeft().getValue(layout);

            g.setColor(colorTopPadding);
            g.fillRect((int) left, colTop, (int) paddingWidth, colHeight);

            paddingWidth = column.paddingRight().getValue(layout);
            double right = column.right().getValue(layout);

            g.setColor(colorBottomPadding);
            g.fillRect((int) (right - paddingWidth), colTop, (int) paddingWidth,
                    colHeight);
        }

        g.setColor(originalColor);
    }
}
