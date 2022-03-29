//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L e d g e r I n t e r                                    //
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
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code LedgerInter} represents a Ledger interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ledger")
public class LedgerInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** Default thickness of a ledger. */
    public static final double DEFAULT_THICKNESS = constants.defaultThickness.getValue();

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //----------------
    //
    /** Mean ledger thickness. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected double thickness;

    /** Median line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    protected Line2D median;

    // Transient data
    //---------------
    //
    /**
     * Index of virtual line relative to staff.
     * <p>
     * Above staff, if index is negative (-1, -2, etc)
     * <pre>
     * -2 --
     * -1 --
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * +1 --
     * +2 --
     * </pre>
     * Below staff, if index is positive (+1, +2, etc)
     */
    private Integer index;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the assignment details
     */
    public LedgerInter (Glyph glyph,
                        GradeImpacts impacts)
    {
        super(glyph, null, Shape.LEDGER, impacts);

        if (glyph != null) {
            thickness = glyph.getMeanThickness(Orientation.HORIZONTAL);
            median = glyph.getCenterLine();

            computeArea();
        }
    }

    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph the underlying glyph
     * @param grade quality
     */
    public LedgerInter (Glyph glyph,
                        Double grade)
    {
        super(glyph, null, Shape.LEDGER, grade);

        if (glyph != null) {
            thickness = glyph.getMeanThickness(Orientation.HORIZONTAL);
            median = glyph.getCenterLine();

            computeArea();
        }
    }

    /**
     * Creates a new LedgerInter object.
     *
     * @param median    horizontal median line
     * @param thickness ledger thickness
     * @param grade     quality
     */
    public LedgerInter (Line2D median,
                        double thickness,
                        Double grade)
    {
        super(null, null, Shape.LEDGER, grade);

        this.median = new Line2D.Double();
        this.median.setLine(median);
        this.thickness = thickness;

        computeArea();
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private LedgerInter ()
    {
        super(null, null, null, (Double) null);
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

    //-------//
    // added //
    //-------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        if (staff != null) {
            staff.addLedger(this);
        }
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

        if (area == null) {
            computeArea();
        }

        return area.contains(point);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (index != null) {
            sb.append(" index:").append(index);
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//


    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if ((median == null) && (glyph != null)) {
            // Case of manual ledger: Compute thickness and median parameters and area
            thickness = glyph.getMeanThickness(Orientation.HORIZONTAL);
            median = glyph.getCenterLine();

            computeArea();
        }
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report the ledger index with respect to staff.
     *
     * @return the index
     */
    public Integer getIndex ()
    {
        return index;
    }

    //----------//
    // setIndex //
    //----------//
    /**
     * Set the ledger index, with respect to staff.
     *
     * @param index the index to set
     */
    public void setIndex (int index)
    {
        this.index = index;
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report item median line.
     *
     * @return copy of the median line
     */
    public Line2D getMedian ()
    {
        return new Line2D.Double(median.getX1(), median.getY1(), median.getX2(), median.getY2());
    }

    //--------------//
    // getThickness //
    //--------------//
    /**
     * Report mean ledger thickness
     *
     * @return ledger thickness
     */
    public double getThickness ()
    {
        return thickness;
    }

    //------------//
    // deriveFrom //
    //------------//


    //--------//
    // remove //
    //--------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        if (staff != null) {
            staff.removeLedger(this);
        }

        super.remove(extensive);
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if (glyph != null) {
            return new Rectangle(bounds = glyph.getBounds());
        }

        if (area == null) {
            computeArea();
        }

        return new Rectangle(bounds = area.getBounds());
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//


    //-------------//
    // computeArea //
    //-------------//
    /**
     * Compute the ledger area.
     */
    protected final void computeArea ()
    {
        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), thickness));

        // Define precise bounds based on this path
        bounds = getArea().getBounds();
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (median != null) {
            computeArea();
        }
    }

    //------------------//
    // getDefaultLength //
    //------------------//
    /**
     * Report the default length for a ledger.
     *
     * @return default length (in interline fraction)
     */
    public static Scale.Fraction getDefaultLength ()
    {
        return constants.defaultLength;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Model //
    //-------//


    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double defaultThickness = new Constant.Double(
                "Pixels",
                3.0,
                "Default ledger thickness");

        private final Scale.Fraction defaultLength = new Scale.Fraction(
                2.0,
                "Default ledger length (WRT interline)");
    }

    //--------//
    // Editor //
    //--------//

}
