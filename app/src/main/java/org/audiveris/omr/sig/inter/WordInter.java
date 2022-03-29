//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W o r d I n t e r                                       //
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
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.StringUtil;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code WordInter} represents a text word.
 * <p>
 * The containing {@link SentenceInter} is linked by a {@link Containment} relation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "word")
public class WordInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(WordInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Word text content. */
    @XmlAttribute
    protected String value;

    /** Detected font attributes. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.Adapter.class)
    protected FontInfo fontInfo;

    /** Precise word starting point on the baseline. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    protected Point2D location;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code WordInter} object, with TEXT shape.
     *
     * @param textWord the OCR'ed text word
     */
    public WordInter (TextWord textWord)
    {
        this(textWord, Shape.TEXT);
    }

    /**
     * Creates a new {@code WordInter} object, with provided shape.
     *
     * @param textWord the OCR'ed text word
     * @param shape    specific shape (TEXT or LYRICS)
     */
    public WordInter (TextWord textWord,
                      Shape shape)
    {
        this(textWord.getGlyph(),
             textWord.getBounds(),
             shape,
             textWord.getConfidence() * Grades.intrinsicRatio,
             textWord.getValue(),
             textWord.getFontInfo(),
             textWord.getLocation());
    }

    /**
     * Creates a new {@code WordInter} object from an original WordInter,
     * with provided shape.
     *
     * @param word  the original word inter
     * @param shape specific shape (TEXT or LYRICS)
     */
    public WordInter (WordInter word,
                      Shape shape)
    {
        this(word.getGlyph(),
             word.getBounds(),
             shape,
             1.0,
             word.getValue(),
             word.getFontInfo(),
             PointUtil.rounded(word.getLocation()));
    }

    /**
     * Creates a new {@code WordInter} object, with all details.
     *
     * @param glyph    underlying glyph
     * @param bounds   bounding box
     * @param shape    specific shape (TEXT or LYRICS)
     * @param grade    quality
     * @param value    text content
     * @param fontInfo font information
     * @param location location
     */
    public WordInter (Glyph glyph,
                      Rectangle bounds,
                      Shape shape,
                      Double grade,
                      String value,
                      FontInfo fontInfo,
                      Point location)
    {
        super(glyph, bounds, shape, grade);
        this.value = value;
        this.fontInfo = fontInfo;
        this.location = location;
    }

    /**
     * Creates a new {@code WordInter} object meant for manual assignment.
     *
     * @param shape specific shape (TEXT or LYRICS)
     * @param grade inter grade
     */
    public WordInter (Shape shape,
                      Double grade)
    {
        super(null, null, shape, grade);

        this.value = "";
        this.fontInfo = null;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected WordInter ()
    {
        super(null, null, null, (Double) null);

        this.fontInfo = null;
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

        return bounds.contains(point);
    }

    //------------//
    // deriveFrom //
    //------------//


    //-----------//
    // getBounds //
    //-----------//

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (value != null) {
            sb.append(" codes[").append(StringUtil.codesOf(value, false)).append(']');
        }

        if (fontInfo != null) {
            sb.append(' ').append(fontInfo.getMnemo());
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//


    //-------------//
    // getFontInfo //
    //-------------//
    /**
     * Report the related font attributes.
     *
     * @return the fontInfo
     */
    public FontInfo getFontInfo ()
    {
        return fontInfo;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * @return the location
     */
    public Point2D getLocation ()
    {
        return location;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * @return the value
     */
    public String getValue ()
    {
        return value;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new text value.
     *
     * @param value the new value
     */
    public void setValue (String value)
    {
        this.value = value;

        setBounds(null);

        if (sig != null) {
            // Update containing sentence
            Inter sentence = getEnsemble();

            if (sentence != null) {
                sentence.invalidateCache();
            }
        }
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        // Location?
        // FontInfo?
    }

    //--------//
    // preAdd //
    //--------//


    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "WORD";
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" \"").append(value).append("\"");

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Model //
    //-------//


    //--------//
    // Editor //
    //--------//

}
