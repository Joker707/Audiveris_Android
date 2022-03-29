package ver2;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;


/**
 This is an 8-bit image and methods that operate on that image. Based on the ImageProcessor class
 from "KickAss Java Programming" by Tonny Espeset.
 */
public class ByteProcessor extends ImageProcessor {

    protected byte[] pixels;


    /** Creates a ByteProcessor from a TYPE_BYTE_GRAY BufferedImage. */
    public ByteProcessor(Bitmap bm) {
        if (bi.getType()!=BufferedImage.TYPE_BYTE_GRAY)
            throw new IllegalArgumentException("Type!=TYPE_BYTE_GRAYY");


//        WritableRaster raster = bi.getRaster();
//        DataBuffer buffer = raster.getDataBuffer();
//        pixels = ((DataBufferByte) buffer).getData();
//        width = raster.getWidth();
//        height = raster.getHeight();
//        resetRoi();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
        pixels = stream.toByteArray();
        bm.recycle();

    }

//    public Image createImage() {
//        if (cm==null) cm = getDefaultColorModel();
//        if (ij.IJ.isJava16()) return  createBufferedImage();
//        if (source==null) {
//            source = new MemoryImageSource(width, height, cm, pixels, 0, width);
//            source.setAnimated(true);
//            source.setFullBufferUpdates(true);
//            img = Toolkit.getDefaultToolkit().createImage(source);
//        } else if (newPixels) {
//            source.newPixels(pixels, cm, 0, width);
//            newPixels = false;
//        } else
//            source.newPixels();
//        return img;
//    }


    Bitmap createBufferedImage() {

//        if (raster==null) {
//            SampleModel sm = getIndexSampleModel();
//            DataBuffer db = new DataBufferByte(pixels, width*height, 0);
//            raster = Raster.createWritableRaster(sm, db, null);
//        }
//        if (image==null || cm!=cm2) {
//            if (cm==null) cm=getDefaultColorModel();
//            image = new BufferedImage(cm, raster, false, null);
//            cm2 = cm;
//        }
//        return image;

        return BitmapFactory.decodeByteArray(pixels, 0, pixels.length);


    }

    /** Returns this image as a BufferedImage. */
    public Bitmap getBufferedImage() {
        return createBufferedImage();
    }




}

