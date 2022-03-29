//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d S y l l a b l e R e l a t i o n                           //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordSyllableRelation} represents a support relation between a chord
 * and a lyric item (of syllable kind).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-syllable")
public class ChordSyllableRelation
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ChordSyllableRelation.class);

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final HeadChordInter chord = (HeadChordInter) e.getEdgeSource();
        final LyricItemInter item = (LyricItemInter) e.getEdgeTarget();
        final boolean above = item.getCenter().y < chord.getCenter().y;
        final LyricLineInter line = (LyricLineInter) item.getEnsemble();
        final Part chordPart = chord.getPart();
        final Part linePart = line.getPart();
        final Staff chordStaff = above ? chord.getTopStaff() : chord.getBottomStaff();

        if (linePart != chordPart) {
            if (linePart != null) {
                linePart.removeLyric(line);
            }

            chordPart.addLyric(line);
            line.setStaff(chordStaff);

            for (Inter inter : line.getMembers()) {
                LyricItemInter it = (LyricItemInter) inter;
                it.setPart(null);
                it.setStaff(chordStaff);
            }

            // Re-numbering of lyric lines
            chordPart.sortLyricLines();
            chord.getSig().getSystem().numberLyricLines();
        }

        item.checkAbnormal();
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        // Just one chord can be linked to a given syllable.
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        // A chord can be linked to several syllables (from different lyric verses).
        return false;
    }

    //---------//
    // preLink //
    //---------//


    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final LyricItemInter item = (LyricItemInter) e.getEdgeTarget();

        if (!item.isRemoved()) {
            item.checkAbnormal();
        }
    }
}
