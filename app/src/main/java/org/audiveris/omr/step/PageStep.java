//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a g e S t e p                                        //
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
package org.audiveris.omr.step;

import org.audiveris.omr.score.MeasureFixer;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageReduction;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetReduction;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.SameTimeRelation;
import org.audiveris.omr.sig.relation.SameVoiceRelation;
import org.audiveris.omr.sig.relation.SeparateTimeRelation;
import org.audiveris.omr.sig.relation.SeparateVoiceRelation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code PageStep} handles connections between systems in a page.
 * <ul>
 * <li>Duplicated inters between systems are resolved.</li>
 * <li>Physical system Part instances are abstracted into LogicalPart instances.</li>
 * <li>Slurs are connected across systems.</li>
 * <li>Tied voices.</li>
 * <li>Refined lyric syllables.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PageStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageStep.class);

    private static final Impact WHOLE_IMPACT = new Impact(true, true, true, true, true);

    /** Classes that may impact voices. */
    private static final Set<Class<?>> forVoices;

    /** Classes that may impact lyrics. */
    private static final Set<Class<?>> forLyrics;

    /** Classes that may impact slurs. */
    private static final Set<Class<?>> forSlurs;

    /** Classes that may impact parts. */
    private static final Set<Class<?>> forParts;

    /** Classes that may impact measures. */
    private static final Set<Class<?>> forMeasures;

    /** All impacting classes. */
    private static final Set<Class<?>> impactingClasses;

    static {
        forVoices = new HashSet<>();
        // Inters
        forVoices.add(AbstractTimeInter.class);
        forVoices.add(AugmentationDotInter.class);
        forVoices.add(BarlineInter.class);
        forVoices.add(BeamHookInter.class);
        forVoices.add(BeamInter.class);
        forVoices.add(FlagInter.class);
        forVoices.add(HeadChordInter.class);
        forVoices.add(HeadInter.class);
        forVoices.add(MeasureStack.class);
        forVoices.add(RestChordInter.class);
        forVoices.add(RestInter.class);
        forVoices.add(SlurInter.class);
        forVoices.add(StemInter.class);
        forVoices.add(StaffBarlineInter.class);
        forVoices.add(TimeNumberInter.class);
        forVoices.add(TupletInter.class);
        // Relations
        forVoices.add(AugmentationRelation.class);
        forVoices.add(NextInVoiceRelation.class);
        forVoices.add(SameTimeRelation.class);
        forVoices.add(SameVoiceRelation.class);
        forVoices.add(SeparateTimeRelation.class);
        forVoices.add(SeparateVoiceRelation.class);
        forVoices.add(DoubleDotRelation.class);
        // Tasks

    }

    static {
        forLyrics = new HashSet<>();
        forLyrics.add(LyricItemInter.class);
        forLyrics.add(LyricLineInter.class);
    }

    static {
        forSlurs = new HashSet<>();
        // Inters
        forSlurs.add(SlurInter.class);
        // Relations
        forSlurs.add(SlurHeadRelation.class);
    }

    static {
        forParts = new HashSet<>();
        forParts.add(BraceInter.class);
        forParts.add(SentenceInter.class);
    }

    static {
        forMeasures = new HashSet<>();
        // Inters
        forMeasures.add(BarlineInter.class);
        forMeasures.add(StaffBarlineInter.class);
        // Tasks

    }

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.addAll(forVoices);
        impactingClasses.addAll(forLyrics);
        impactingClasses.addAll(forSlurs);
        impactingClasses.addAll(forParts);
        impactingClasses.addAll(forMeasures);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageStep} object.
     */
    public PageStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        // Clean up gutter between systems one under the other
        new SheetReduction(sheet).process();

        for (Page page : sheet.getPages()) {
            processPage(page);
        }
    }

    //--------//
    // impact //
    //--------//


    //--------------//
    // isImpactedBy //
    //--------------//
    @Override
    public boolean isImpactedBy (Class<?> classe)
    {
        return isImpactedBy(classe, impactingClasses);
    }

    //-------------//
    // processPage //
    //-------------//
    public void processPage (Page page)
    {
        doProcessPage(page, WHOLE_IMPACT);
    }

    //---------------//
    // doProcessPage //
    //---------------//
    private void doProcessPage (Page page,
                                Impact impact)
    {
        if (impact.onParts) {
            // Connect parts across systems in the page
            new PageReduction(page).reduce();
        }

        if (impact.onMeasures) {
            // Merge / renumber measure stacks within the page
            new MeasureFixer().process(page);
        }

        if (impact.onSlurs) {
            // Inter-system slurs connections
            page.connectOrphanSlurs();
            page.checkPageCrossTies();
        }

        if (impact.onLyrics) {
            // Lyrics
            refineLyrics(page);
        }

        if (impact.onVoices) {
            // Refine voices IDs (and thus colors) across all systems of the page
            Voices.refinePage(page);
        }
    }

    //--------------//
    // refineLyrics //
    //--------------//
    /**
     * Refine syllables across systems in page
     *
     * @param page provided page
     */
    private void refineLyrics (Page page)
    {
        for (SystemInfo system : page.getSystems()) {
            for (Inter inter : system.getSig().inters(LyricLineInter.class)) {
                LyricLineInter line = (LyricLineInter) inter;
                line.refineLyricSyllables();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Impact //
    //--------//
    private static class Impact
    {

        boolean onParts = false;

        boolean onSlurs = false;

        boolean onLyrics = false;

        boolean onVoices = false;

        boolean onMeasures = false;

        public Impact ()
        {
        }

        public Impact (boolean onParts,
                       boolean onSlurs,
                       boolean onLyrics,
                       boolean onVoices,
                       boolean onMeasures)
        {
            this.onParts = onParts;
            this.onSlurs = onSlurs;
            this.onLyrics = onLyrics;
            this.onVoices = onVoices;
            this.onMeasures = onMeasures;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("PageImpact{");
            sb.append("parts:").append(onParts);
            sb.append(" slurs:").append(onSlurs);
            sb.append(" lyrics:").append(onLyrics);
            sb.append(" voices:").append(onVoices);
            sb.append(" measures:").append(onMeasures);
            sb.append("}");

            return sb.toString();
        }
    }
}
