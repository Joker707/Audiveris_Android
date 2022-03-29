//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       W e d g e I n t e r                                      //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.ChordWedgeRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code WedgeInter} represents a wedge (crescendo or diminuendo).
 * <p>
 * <img alt="Wedge image"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Music_hairpins.svg/296px-Music_hairpins.svg.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "wedge")
public class WedgeInter
        extends AbstractDirectionInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(WedgeInter.class);

    /** Default thickness of a wedge line. */
    public static final double DEFAULT_THICKNESS = constants.defaultThickness.getValue();

    /** Location of handle below, with respect to wedge line length. */
    private static final double HANDLE_RATIO = constants.handleRatio.getValue();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Top line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D l1;

    /** Bottom line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D l2;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new WedgeInter object.
     *
     * @param l1      precise first line
     * @param l2      precise second line
     * @param bounds  bounding box
     * @param shape   CRESCENDO or DIMINUENDO
     * @param impacts assignments details
     */
    public WedgeInter (Line2D l1,
                       Line2D l2,
                       Rectangle bounds,
                       Shape shape,
                       GradeImpacts impacts)
    {
        super(null, bounds, shape, impacts);
        this.l1 = l1;
        this.l2 = l2;
    }

    /**
     * Creates a new {@code WedgeInter} object, meant for manual assignment.
     *
     * @param glyph underlying glyph, if any
     * @param shape CRESCENDO or DIMINUENDO
     * @param grade assigned grade
     */
    public WedgeInter (Glyph glyph,
                       Shape shape,
                       Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);

        if (glyph != null) {
            setGlyph(glyph);
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private WedgeInter ()
    {
        super(null, null, null, 0.0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        if (l1.ptLineDistSq(point) <= ((DEFAULT_THICKNESS * DEFAULT_THICKNESS) / 4)) {
            return true;
        }

        if (l2.ptLineDistSq(point) <= ((DEFAULT_THICKNESS * DEFAULT_THICKNESS) / 4)) {
            return true;
        }

        return false;
    }

    //------------//
    // deriveFrom //
    //------------//


    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        Rectangle box = super.getBounds();

        if (box != null) {
            return new Rectangle(box);
        }

        box = l1.getBounds().union(l2.getBounds());
        box.grow(0, (int) Math.ceil(DEFAULT_THICKNESS / 2.0));

        return new Rectangle(bounds = box);
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle b)
    {
        super.setBounds(b);

        if (b != null) {
            final double dy = DEFAULT_THICKNESS / 2.0;

            if (shape == Shape.CRESCENDO) {
                l1 = new Line2D.Double(b.x, b.y + b.height / 2.0,
                                       b.x + b.width, b.y + 0.5 + dy);
            } else {
                l1 = new Line2D.Double(b.x, b.y + 0.5 + dy,
                                       b.x + b.width, b.y + b.height / 2.0);
            }

            if (shape == Shape.CRESCENDO) {
                l2 = new Line2D.Double(b.x, b.y + b.height / 2.0,
                                       b.x + b.width, b.y + b.height - 0.5 - dy);
            } else {
                l2 = new Line2D.Double(b.x, b.y + b.height - 0.5 - dy,
                                       b.x + b.width, b.y + b.height / 2.0);
            }
        }
    }

    //-----------//
    // getEditor //
    //-----------//


    //----------//
    // getLine1 //
    //----------//
    /**
     * Report wedge top line.
     *
     * @return top line
     */
    public Line2D getLine1 ()
    {
        return l1;
    }

    //----------//
    // getLine2 //
    //----------//
    /**
     * Report wedge bottom line.
     *
     * @return bottom line
     */
    public Line2D getLine2 ()
    {
        return l2;
    }

    //-----------//
    // getSpread //
    //-----------//
    /**
     * Report vertical gap between ending points or provided side.
     *
     * @param side provided horizontal side
     * @return vertical gap in pixels
     */
    public double getSpread (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return l2.getY1() - l1.getY1();
        } else {
            return l2.getY2() - l1.getY2();
        }
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if (glyph != null) {
            setBounds(glyph.getBounds());
        }
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();
        final double xMargin = scale.toPixels(constants.stackAbscissaMargin);
        final Line2D topLine = getLine1();
        final List<Link> links = new ArrayList<>();

        for (HorizontalSide side : HorizontalSide.values()) {
            final Point2D location = (side == HorizontalSide.LEFT)
                    ? new Point2D.Double(topLine.getX1() + xMargin, topLine.getY1())
                    : new Point2D.Double(topLine.getX2() - xMargin, topLine.getY2());
            final MeasureStack stack = system.getStackAt(location);

            if (stack == null) {
                continue;
            }

            final AbstractChordInter chordAbove = stack.getStandardChordAbove(location, null);

            if (chordAbove != null) {
                links.add(new Link(chordAbove, new ChordWedgeRelation(side), false));
            } else {
                final AbstractChordInter chordBelow = stack.getStandardChordBelow(location, null);

                if (chordBelow != null) {
                    links.add(new Link(chordBelow, new ChordWedgeRelation(side), false));
                } else {
                    logger.info("No chord for {} {}", this, side);
                }
            }
        }

        return links;
    }

    //---------------//
    // searchUnlinks //
    //---------------//

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{
            "s1",
            "s2",
            "closedDy",
            "openDy",
            "openBias"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1, 1};

        public Impacts (double s1,
                        double s2,
                        double closedDy,
                        double openDy,
                        double openBias)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, s1);
            setImpact(1, s2);
            setImpact(2, closedDy);
            setImpact(3, openDy);
            setImpact(4, openBias);
        }
    }

    //-------//
    // Model //
    //-------//


    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction stackAbscissaMargin = new Scale.Fraction(
                1.0,
                "Margin beyond stack abscissa limits");

        private final Constant.Double defaultThickness = new Constant.Double(
                "pixels",
                3.0,
                "Default wedge line thickness");

        private final Constant.Ratio handleRatio = new Constant.Ratio(
                0.1,
                "Ratio of handle below with respect to line length");
    }

    //--------//
    // Editor //
    //--------//

}
