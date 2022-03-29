//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G r i d B u i l d e r                                      //
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Sheet;


import org.audiveris.omr.step.StepException;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code GridBuilder} computes the grid of systems of a sheet picture, based on
 * the retrieval of horizontal staff lines and of vertical bar lines.
 * <p>
 * The actual processing is delegated to 3 companions:<ul>
 * <li>{@link LinesRetriever} for retrieving horizontal staff lines.</li>
 * <li>{@link BarsRetriever} for retrieving vertical bar lines.</li>
 * <li>Optionally, {@link TargetBuilder} for building the target grid.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class GridBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GridBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Companion in charge of staff lines. */
    public final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines. */
    public final BarsRetriever barsRetriever;

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Retrieve the frames of all staff lines.
     *
     * @param sheet the sheet to process
     */
    public GridBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        barsRetriever = new BarsRetriever(sheet);
        linesRetriever = new LinesRetriever(sheet, barsRetriever);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture.
     *
     * @throws StepException if step was stopped
     */
    public void buildInfo ()
            throws StepException
    {
        StopWatch watch = new StopWatch("GridBuilder");

        try {
            // Build the vertical and horizontal lags
            watch.start("buildAllLags");
            buildAllLags();

            // Display

            // Retrieve the horizontal staff lines filaments with long sections
            watch.start("retrieveLines");
            linesRetriever.retrieveLines();

            // Complete horizontal lag with short sections
            linesRetriever.createShortSections();

            // Retrieve the vertical barlines and thus the systems
            watch.start("retrieveBarlines");
            barsRetriever.process();

            // Complete the staff lines w/ short sections & filaments left over
            watch.start("completeLines");
            linesRetriever.completeLines();

            /** Companion in charge of target grid. */
            // Define the destination grid, if so desired
            if (constants.buildDewarpedTarget.isSet()) {
                watch.start("targetBuilder");

                TargetBuilder targetBuilder = new TargetBuilder(sheet);
                targetBuilder.buildInfo();
            }
        } catch (StepException se) {
            throw se;
        } catch (Throwable ex) {
            logger.warn("Error in GridBuilder: {}", ex.toString(), ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //------------//
    // updateBars //
    //------------//


    //--------------//
    // buildAllLags //
    //--------------//
    /**
     * From the BINARY table, build the horizontal lag (for staff lines) and the
     * vertical lag (for barlines).
     */
    private void buildAllLags ()
    {
        final StopWatch watch = new StopWatch("buildAllLags");

        try {
            // We already have all foreground pixels as vertical runs

            // hLag creation
            watch.start("buildHorizontalLag");

            RunTable longVertTable = linesRetriever.buildHorizontalLag();

            // vLag creation
            watch.start("buildVerticalLag");
            sheet.getLagManager().buildVerticalLag(longVertTable);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean buildDewarpedTarget = new Constant.Boolean(
                false,
                "Should we build a dewarped target?");

        private final Constant.Boolean showGrid = new Constant.Boolean(
                false,
                "Should we show the details of grid?");

        private final Constant.Boolean displayFilaments = new Constant.Boolean(
                false,
                "Should we show the filaments view?");
    }
}
