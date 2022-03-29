//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T a r g e t B u i l d e r                                    //
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.jai.JaiDewarper;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class {@code TargetBuilder} is in charge of building a "perfect" definition of target
 * systems, staves and lines as well as the de-warp grid that allows to transform the
 * original image in to the perfect image.
 *
 * @author Hervé Bitteur
 */
public class TargetBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TargetBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Target width */
    private double targetWidth;

    /** Target height */
    private double targetHeight;

    /** Transform from initial point to de-skewed point */
    private AffineTransform at;

    /** The target page */
    private TargetPage targetPage;

    /** All target lines */
    private final List<TargetLine> allTargetLines = new ArrayList<>();

    /** Source points */
    private final List<Point2D> srcPoints = new ArrayList<>();

    /** Destination points */
    private final List<Point2D> dstPoints = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TargetBuilder object.
     *
     * @param sheet the related sheet
     */
    public TargetBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Build a de-warped image according to target grid.
     */
    public void buildInfo ()
    {
        buildTarget();

        JaiDewarper dewarper = new JaiDewarper();

        // Define the dewarp grid
        buildWarpGrid(dewarper);

        // Dewarp the initial image

        // Add a view on dewarped image?

        // Store dewarped image on disk

    }

    //-------------//
    // renderItems //
    //-------------//


    //---------------//
    // renderSystems //
    //---------------//


    //----------------//
    // renderWarpGrid //
    //----------------//


    //-------------//
    // buildTarget //
    //-------------//
    /**
     * Build a perfect definition of target page, systems, staves and lines.
     * <p>
     * We apply a rotation on every top-left corner
     */
    private void buildTarget ()
    {
        final Skew skew = sheet.getSkew();

        // Target page parameters
        targetPage = new TargetPage(targetWidth, targetHeight);

        TargetLine prevLine = null; // Latest staff line

        // Target system parameters
        for (SystemInfo system : sheet.getSystems()) {
            Staff firstStaff = system.getFirstStaff();
            LineInfo firstLine = firstStaff.getFirstLine();
            Point2D dskLeft = skew.deskewed(firstLine.getEndPoint(LEFT));
            Point2D dskRight = skew.deskewed(firstLine.getEndPoint(RIGHT));

            if (prevLine != null) {
                // Preserve position relative to bottom left of previous system
                Point2D prevDskLeft = skew.deskewed(prevLine.info.getEndPoint(LEFT));
                TargetSystem prevSystem = prevLine.staff.system;
                double dx = prevSystem.left - prevDskLeft.getX();
                double dy = prevLine.y - prevDskLeft.getY();
                dskLeft.setLocation(dskLeft.getX() + dx, dskLeft.getY() + dy);
                dskRight.setLocation(dskRight.getX() + dx, dskRight.getY() + dy);
            }

            TargetSystem targetSystem = new TargetSystem(
                    system,
                    dskLeft.getY(),
                    dskLeft.getX(),
                    dskRight.getX());
            targetPage.systems.add(targetSystem);

            // Target staff parameters
            for (Staff staff : system.getStaves()) {
                dskLeft = skew.deskewed(staff.getFirstLine().getEndPoint(LEFT));

                if (prevLine != null) {
                    // Preserve inter-staff vertical gap
                    Point2D prevDskLeft = skew.deskewed(prevLine.info.getEndPoint(LEFT));
                    dskLeft.setLocation(
                            dskLeft.getX(),
                            dskLeft.getY() + (prevLine.y - prevDskLeft.getY()));
                }

                TargetStaff targetStaff = new TargetStaff(staff, dskLeft.getY(), targetSystem);
                targetSystem.staves.add(targetStaff);

                // Target line parameters
                int lineIdx = -1;

                for (LineInfo line : staff.getLines()) {
                    lineIdx++;

                    // Enforce perfect staff interline
                    TargetLine targetLine = new TargetLine(
                            line,
                            targetStaff.top + (staff.getSpecificInterline() * lineIdx),
                            targetStaff);
                    allTargetLines.add(targetLine);
                    targetStaff.lines.add(targetLine);
                    prevLine = targetLine;
                }
            }
        }
    }

    //---------------//
    // buildWarpGrid //
    //---------------//
    private void buildWarpGrid (JaiDewarper dewarper)
    {
        int xStep = sheet.getInterline();
        int xNumCells = (int) Math.ceil(sheet.getWidth() / (double) xStep);
        int yStep = sheet.getInterline();
        int yNumCells = (int) Math.ceil(sheet.getHeight() / (double) yStep);

        for (int ir = 0; ir <= yNumCells; ir++) {
            for (int ic = 0; ic <= xNumCells; ic++) {
                Point2D dst = new Point2D.Double(ic * xStep, ir * yStep);
                dstPoints.add(dst);

                Point2D src = sourceOf(dst);
                srcPoints.add(src);
            }
        }

        float[] warpPositions = new float[srcPoints.size() * 2];
        int i = 0;

        for (Point2D p : srcPoints) {
            warpPositions[i++] = (float) p.getX();
            warpPositions[i++] = (float) p.getY();
        }

        dewarper.createWarpGrid(0, xStep, xNumCells, 0, yStep, yNumCells, warpPositions);
    }

    //----------//
    // sourceOf //
    //----------//
    /**
     * This key method provides the source point (in original sheet image)
     * that corresponds to a given destination point (in target dewarped image).
     * <p>
     * The strategy is to stay consistent with the staff lines nearby which
     * are used as grid references.
     *
     * @param dst the given destination point
     * @return the corresponding source point
     */
    private Point2D sourceOf (Point2D dst)
    {
        double dstX = dst.getX();
        double dstY = dst.getY();

        // Retrieve north & south lines, if any
        TargetLine northLine = null;
        TargetLine southLine = null;

        for (TargetLine line : allTargetLines) {
            if (line.y <= dstY) {
                northLine = line;
            } else {
                southLine = line;

                break;
            }
        }

        // Case of image top: no northLine
        if (northLine == null) {
            return southLine.sourceOf(dst);
        }

        // Case of image bottom: no southLine
        if (southLine == null) {
            return northLine.sourceOf(dst);
        }

        // Normal case: use y barycenter between projections sources
        Point2D srcNorth = northLine.sourceOf(dstX);
        Point2D srcSouth = southLine.sourceOf(dstX);
        double yRatio = (dstY - northLine.y) / (southLine.y - northLine.y);

        return new Point2D.Double(
                ((1 - yRatio) * srcNorth.getX()) + (yRatio * srcSouth.getX()),
                ((1 - yRatio) * srcNorth.getY()) + (yRatio * srcSouth.getY()));
    }

    //------------//
    // storeImage //
    //------------//


    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//

    //--------------//
    // DewarpedView //
    //--------------//

}
