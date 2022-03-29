//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I m a g e U t i l                                       //
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

import org.audiveris.omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;

/**
 * Class {@code ImageUtil} gathers convenient static methods working on images.
 * <p>
 * TODO: Perhaps chaining JAI commands into a single operation would be more efficient (memory-wise
 * and performance-wise) that performing each bulk operation one after the other. It would also
 * save multiple calls to "getAsBufferedImage()".
 *
 * @author Hervé Bitteur
 */
public abstract class ImageUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

    //~ Constructors -------------------------------------------------------------------------------
    private ImageUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // invert //
    //--------//


    //--------------//
    // maxRgbToGray //
    //--------------//


    //---------------//
    // maxRgbaToGray //
    //---------------//


    //-----------//
    // printInfo //
    //-----------//


    //-----------//
    // rgbToGray //
    //-----------//


    //------------//
    // rgbaToGray //
    //------------//


    //-----------//
    // rgbaToRgb //
    //-----------//


    //------------//
    // saveOnDisk //
    //------------//
    /**
     * Convenient method to save a BufferedImage to disk (in application temp area)
     *
     * @param image the image to save
     * @param name  file name, without extension
     */
    public static void saveOnDisk (BufferedImage image,
                                   String name)
    {
        try {
            final File file = WellKnowns.TEMP_FOLDER.resolve(name + ".png").toFile();
            ImageIO.write(image, "png", file);
            logger.info("Saved {}", file);
        } catch (IOException ex) {
            logger.warn("Error saving " + name, ex);
        }
    }

    //-----//
    // xor //
    //-----//


    //--------//
    // typeOf //
    //--------//

}
