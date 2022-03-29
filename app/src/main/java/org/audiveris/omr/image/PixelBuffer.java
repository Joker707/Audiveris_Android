//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P i x e l B u f f e r                                      //
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

import ij.process.ByteProcessor;

import net.jcip.annotations.ThreadSafe;
import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Class {@code PixelBuffer} handles a plain rectangular buffer of bytes.
 * <p>
 * It is an efficient {@link PixelFilter} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelBuffer
        extends Table.UnsignedByte
        implements PixelFilter, PixelSink
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PixelBuffer.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PixelBuffer object.
     *
     * @param dimension the buffer dimension
     */
    public PixelBuffer (Dimension dimension)
    {
        super(dimension.width, dimension.height);

        // Initialize the whole buffer with background color value
        fill(BACKGROUND);
    }

    //-------------//
    // PixelBuffer //
    //-------------//


    //-------------//
    // PixelBuffer //
    //-------------//


    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public ByteProcessor filteredImage ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int get (int x,
                    int y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        return new Context(BACKGROUND / 2);
    }

    //---------//
    // getCopy //
    //---------//
    @Override
    public PixelBuffer getCopy (Rectangle roi)
    {
        PixelBuffer copy;

        if (roi == null) {
            copy = new PixelBuffer(new Dimension(width, height));
            System.arraycopy(data, 0, copy.data, 0, data.length);
        } else {
            checkRoi(roi);

            copy = new PixelBuffer(new Dimension(roi.width, roi.height));

            for (int y = 0; y < roi.height; y++) {
                int p = ((y + roi.y) * width) + roi.x;
                System.arraycopy(data, p, copy.data, y * roi.width, roi.width);
            }
        }

        return copy;
    }

    //--------------//
    // injectBuffer //
    //--------------//


    //--------//
    // isFore //
    //--------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return getValue(x, y) < 225; //TODO: Why not 255 ?????????? A typo?
    }

    //-----------------//
    // toBufferedImage //
    //-----------------//

}
