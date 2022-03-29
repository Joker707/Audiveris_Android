/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/* ****************************************************************
 ******************************************************************
 ******************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997
 *** As  an unpublished  work pursuant to Title 17 of the United
 *** States Code.  All rights reserved.
 ******************************************************************
 ******************************************************************
 ******************************************************************/

package java.awt.image;
import java.awt.Rectangle;
import java.awt.Point;

/**
 * This class extends Raster to provide pixel writing capabilities.
 * Refer to the class comment for Raster for descriptions of how
 * a Raster stores pixels.
 *
 * <p> The constructors of this class are protected.  To instantiate
 * a WritableRaster, use one of the createWritableRaster factory methods
 * in the Raster class.
 */
public class WritableRaster extends Raster {

    /**
     *  Constructs a WritableRaster with the given SampleModel.  The
     *  WritableRaster's upper left corner is origin and it is the
     *  same size as the  SampleModel.  A DataBuffer large enough to
     *  describe the WritableRaster is automatically created.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param origin          The Point that specifies the origin.
     *  @throws RasterFormatException if computing either
     *          {@code origin.x + sampleModel.getWidth()} or
     *          {@code origin.y + sampleModel.getHeight()} results
     *          in integer overflow
     */
    protected WritableRaster(SampleModel sampleModel,
                             Point origin) {
        this(sampleModel,
                sampleModel.createDataBuffer(),
                new Rectangle(origin.x,
                        origin.y,
                        sampleModel.getWidth(),
                        sampleModel.getHeight()),
                origin,
                null);
    }

    /**
     *  Constructs a WritableRaster with the given SampleModel and DataBuffer.
     *  The WritableRaster's upper left corner is origin and it is the same
     *  size as the SampleModel.  The DataBuffer is not initialized and must
     *  be compatible with SampleModel.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param dataBuffer      The DataBuffer that contains the image data.
     *  @param origin          The Point that specifies the origin.
     *  @throws RasterFormatException if computing either
     *          {@code origin.x + sampleModel.getWidth()} or
     *          {@code origin.y + sampleModel.getHeight()} results
     *          in integer overflow
     */
    protected WritableRaster(SampleModel sampleModel,
                             DataBuffer dataBuffer,
                             Point origin) {
        this(sampleModel,
                dataBuffer,
                new Rectangle(origin.x,
                        origin.y,
                        sampleModel.getWidth(),
                        sampleModel.getHeight()),
                origin,
                null);
    }

    /**
     * Constructs a WritableRaster with the given SampleModel, DataBuffer,
     * and parent.  aRegion specifies the bounding rectangle of the new
     * Raster.  When translated into the base Raster's coordinate
     * system, aRegion must be contained by the base Raster.
     * (The base Raster is the Raster's ancestor which has no parent.)
     * sampleModelTranslate specifies the sampleModelTranslateX and
     * sampleModelTranslateY values of the new Raster.
     *
     * Note that this constructor should generally be called by other
     * constructors or create methods, it should not be used directly.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBuffer that contains the image data.
     * @param aRegion         The Rectangle that specifies the image area.
     * @param sampleModelTranslate  The Point that specifies the translation
     *                        from SampleModel to Raster coordinates.
     * @param parent          The parent (if any) of this raster.
     * @throws RasterFormatException if {@code aRegion} has width
     *         or height less than or equal to zero, or computing either
     *         {@code aRegion.x + aRegion.width} or
     *         {@code aRegion.y + aRegion.height} results in integer
     *         overflow
     */
    protected WritableRaster(SampleModel sampleModel,
                             DataBuffer dataBuffer,
                             Rectangle aRegion,
                             Point sampleModelTranslate,
                             WritableRaster parent){
        super(sampleModel,dataBuffer,aRegion,sampleModelTranslate,parent);
    }


    /**
     * Sets the data for a rectangle of pixels from a
     * primitive array of type TransferType.  For image data supported by
     * the Java 2D API, this will be one of DataBuffer.TYPE_BYTE,
     * DataBuffer.TYPE_USHORT, DataBuffer.TYPE_INT, DataBuffer.TYPE_SHORT,
     * DataBuffer.TYPE_FLOAT, or DataBuffer.TYPE_DOUBLE.  Data in the array
     * may be in a packed format, thus increasing efficiency for data
     * transfers.
     * An ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds, or if inData is not large enough to hold the pixel data.
     * However, explicit bounds checking is not guaranteed.
     * A ClassCastException will be thrown if the input object is not null
     * and references anything other than an array of TransferType.
     * @see java.awt.image.SampleModel#setDataElements(int, int, int, int, Object, DataBuffer)
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param inData   An object reference to an array of type defined by
     *                 getTransferType() and length w*h*getNumDataElements()
     *                 containing the pixel data to place between x,y and
     *                 x+w-1, y+h-1.
     *
     * @throws NullPointerException if inData is null.
     * @throws ArrayIndexOutOfBoundsException if the coordinates are not
     * in bounds, or if inData is too small to hold the input.
     */
    public void setDataElements(int x, int y, int w, int h, Object inData) {
        sampleModel.setDataElements(x-sampleModelTranslateX,
                y-sampleModelTranslateY,
                w,h,inData,dataBuffer);
    }








}
