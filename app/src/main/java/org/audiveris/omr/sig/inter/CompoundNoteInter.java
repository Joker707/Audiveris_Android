//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                C o m p o u n d N o t e I n t e r                               //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code CompoundNoteInter} represents a head combined with a stem.
 * <p>
 * Instances of this class are meant to be temporary, not put in SIG, just to ease manual insertion
 * of quarter and half notes.
 * <p>
 * When such compound is dropped, it gets replaced by head + stem + head-stem relation.
 * <p>
 * TODO:
 *
 * getNeededLedgerLines
 * getNeededLedgerAdditions (?)
 *
 * Tracker
 *
 * @author Hervé Bitteur
 */
public class CompoundNoteInter
        extends AbstractInter
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(CompoundNoteInter.class);

    //~ Instance fields ----------------------------------------------------------------------------


    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadInter} object.
     *
     * @param glyph  the underlying glyph if any
     * @param bounds the object bounds
     * @param shape  the underlying shape
     * @param grade  quality grade
     */
    public CompoundNoteInter (Glyph glyph,
                              Rectangle bounds,
                              Shape shape,
                              Double grade)
    {
        super(glyph, bounds, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // deriveFrom //
    //------------//


    //---------------//
    // getHeadCenter //
    //---------------//
    /**
     * Report the head center (which is vertically shifted from compound center).
     *
     * @return head center
     */
    public Point2D getHeadCenter ()
    {
        return getCenter2D();
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        return getHeadCenter();
    }

    //------------//
    // getTracker //
    //------------//


    //----------//
    // getModel //
    //----------//


    //--------//
    // preAdd //
    //--------//


    //-----------//
    // isQuarter //
    //-----------//


    //------//
    // isUp //
    //------//

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Model //
    //-------//

}
