//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R h y t h m s S t e p                                     //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.SameTimeRelation;
import org.audiveris.omr.sig.relation.SameVoiceRelation;
import org.audiveris.omr.sig.relation.SeparateTimeRelation;
import org.audiveris.omr.sig.relation.SeparateVoiceRelation;
import org.audiveris.omr.step.AbstractStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code RhythmsStep} is a comprehensive step that handles the timing of every
 * relevant item within a page.
 *
 * @author Hervé Bitteur
 */
public class RhythmsStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RhythmsStep.class);

    /** Classes that impact just a measure stack. */
    private static final Set<Class<?>> forStack;

    /** Classes that impact a whole page. */
    private static final Set<Class<?>> forPage;

    /** All impacting classes. */
    private static final Set<Class<?>> impactingClasses;

    static {
        forStack = new HashSet<>();
        // Inters
        forStack.add(AugmentationDotInter.class);
        forStack.add(BarlineInter.class);
        forStack.add(BeamHookInter.class);
        forStack.add(BeamInter.class);
        forStack.add(FlagInter.class);
        forStack.add(HeadChordInter.class);
        forStack.add(HeadInter.class);
        forStack.add(RestChordInter.class);
        forStack.add(RestInter.class);
        forStack.add(SmallBeamInter.class);
        forStack.add(SmallChordInter.class);
        forStack.add(SmallFlagInter.class);
        forStack.add(StaffBarlineInter.class);
        forStack.add(StemInter.class);
        forStack.add(TupletInter.class);
        forStack.add(MeasureStack.class);
        // Relations
        forStack.add(AugmentationRelation.class);
        forStack.add(BeamStemRelation.class);
        forStack.add(ChordTupletRelation.class);
        forStack.add(DoubleDotRelation.class);
        forStack.add(HeadStemRelation.class);
        forStack.add(NextInVoiceRelation.class);
        forStack.add(SameTimeRelation.class);
        forStack.add(SameVoiceRelation.class);
        forStack.add(SeparateTimeRelation.class);
        forStack.add(SeparateVoiceRelation.class);
    }

    static {
        forPage = new HashSet<>();
        // Inters
        forPage.add(AbstractTimeInter.class);
        forPage.add(BraceInter.class); // Possibility of part merge/split
        forPage.add(SlurInter.class); // Possibility of ties
        forPage.add(TimeNumberInter.class);
        // Tasks
    }

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.addAll(forStack);
        impactingClasses.addAll(forPage);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RhythmsStep} object.
     */
    public RhythmsStep ()
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
        // Process each page of the sheet
        for (Page page : sheet.getPages()) {
            new PageRhythm(page).process();
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Impact //
    //--------//
    private static class Impact
    {

        boolean onPage = false;

        Set<MeasureStack> onStacks = new LinkedHashSet<>();

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("RhythmsImpact{");
            sb.append("page:").append(onPage);
            sb.append(" stacks:").append(onStacks);
            sb.append("}");

            return sb.toString();
        }

        public void add (MeasureStack stack)
        {
            if (stack != null) {
                onStacks.add(stack);
            }
        }
    }
}
