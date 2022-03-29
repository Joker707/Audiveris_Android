//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D i s t a n c e T a b l e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.image;

import static org.audiveris.omr.image.ChamferDistance.VALUE_TARGET;
import static org.audiveris.omr.image.ChamferDistance.VALUE_UNKNOWN;

import org.audiveris.omr.util.Table;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Interface {@code DistanceTable}
 *
 * @author Hervé Bitteur
 */
public interface DistanceTable
        extends Table
{
    //~ Methods ------------------------------------------------------------------------------------



    /**
     * Report the normalizing value by which each raw distance data should be divided.
     *
     * @return the normalizing value
     */
    int getNormalizer ();

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Integer //
    //---------//
    public static class Integer
            extends Abstract
    {

        private final Table.Integer table;

        public Integer (int width,
                        int height,
                        int normalizer)
        {
            super(normalizer);
            table = new Table.Integer(width, height);
        }

        protected Integer (Table.Integer table,
                           int normalizer)
        {
            super(normalizer);
            this.table = table;
        }

        @Override
        public DistanceTable.Integer getCopy (Rectangle roi)
        {
            return new DistanceTable.Integer(table.getCopy(roi), normalizer);
        }

        @Override
        public DistanceTable.Integer getView (Rectangle roi)
        {
            return new DistanceTable.Integer(table.getView(roi), normalizer);
        }

        @Override
        public void dump (String title)
        {
            table.dump(title);
        }

        @Override
        protected final Table getTable ()
        {
            return table;
        }
    }

    //-------//
    // Short //
    //-------//
    public static class Short
            extends Abstract
    {

        private final Table.Short table;

        public Short (int width,
                      int height,
                      int normalizer)
        {
            super(normalizer);
            table = new Table.Short(width, height);
        }

        protected Short (Table.Short table,
                         int normalizer)
        {
            super(normalizer);
            this.table = table;
        }

        @Override
        public DistanceTable.Short getCopy (Rectangle roi)
        {
            return new DistanceTable.Short(table.getCopy(roi), normalizer);
        }

        @Override
        public DistanceTable.Short getView (Rectangle roi)
        {
            return new DistanceTable.Short(table.getView(roi), normalizer);
        }

        @Override
        public void dump (String title)
        {
            table.dump(title);
        }

        @Override
        protected final Table getTable ()
        {
            return table;
        }
    }

    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
            implements DistanceTable
    {

        protected final int normalizer;

        public Abstract (int normalizer)
        {
            this.normalizer = normalizer;
        }

        @Override
        public void fill (int val)
        {
            getTable().fill(val);
        }

        @Override
        public int getHeight ()
        {
            return getTable().getHeight();
        }

        //----------//
        // getImage //
        //----------//


        //---------------//
        // getNormalizer //
        //---------------//
        @Override
        public int getNormalizer ()
        {
            return normalizer;
        }

        @Override
        public int getValue (int index)
        {
            return getTable().getValue(index);
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            return getTable().getValue(x, y);
        }

        @Override
        public int getWidth ()
        {
            return getTable().getWidth();
        }

        @Override
        public void setValue (int index,
                              int value)
        {
            getTable().setValue(index, value);
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            getTable().setValue(x, y, val);
        }

        //----------//
        // getTable //
        //----------//
        protected abstract Table getTable ();

        //--------//
        // getLut //
        //--------//

    }
}
