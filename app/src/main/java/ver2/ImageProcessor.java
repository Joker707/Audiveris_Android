package ver2;


import java.util.*;
import java.awt.*;
import java.awt.image.*;
import ij.gui.*;
import ij.util.*;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.Binner;
import ij.process.AutoThresholder.Method;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Overlay;
import ij.Prefs;

/**
 This abstract class is the superclass for classes that process
 the four data types (byte, short, float and RGB) supported by ImageJ.
 An ImageProcessor contains the pixel data of a 2D image and
 some basic methods to manipulate it.
 @see ByteProcessor
 @see ShortProcessor
 @see FloatProcessor
 @see ColorProcessor
 @see ij.ImagePlus
 @see ij.ImageStack
 */
public abstract class ImageProcessor implements Cloneable {

    /** Value of pixels included in masks. */
    public static final int BLACK = 0xFF000000;

    /** Value returned by getMinThreshold() when thresholding is not enabled. */
    public static final double NO_THRESHOLD = -808080.0;

    /** Left justify text. */
    public static final int LEFT_JUSTIFY = 0;
    /** Center justify text. */
    public static final int CENTER_JUSTIFY = 1;
    /** Right justify text. */
    public static final int RIGHT_JUSTIFY = 2;

    /** Isodata thresholding method */
    public static final int ISODATA = 0;

    /** Modified isodata method used in Image/Adjust/Threshold tool */
    public static final int ISODATA2 = 1;

    /** Interpolation methods */
    public static final int NEAREST_NEIGHBOR=0, NONE=0, BILINEAR=1, BICUBIC=2;

    public static final int BLUR_MORE=0, FIND_EDGES=1, MEDIAN_FILTER=2, MIN=3, MAX=4, CONVOLVE=5;
    static public final int RED_LUT=0, BLACK_AND_WHITE_LUT=1, NO_LUT_UPDATE=2, OVER_UNDER_LUT=3;
    static final int INVERT=0, FILL=1, ADD=2, MULT=3, AND=4, OR=5,
            XOR=6, GAMMA=7, LOG=8, MINIMUM=9, MAXIMUM=10, SQR=11, SQRT=12, EXP=13, ABS=14, SET=15;
    static final String WRONG_LENGTH = "width*height!=pixels.length";

    int fgColor = 0;
    protected int lineWidth = 1;
    protected int cx, cy; //current drawing coordinates
    protected Font font = ij.ImageJ.SansSerif12;
    protected FontMetrics fontMetrics;
    protected boolean antialiasedText;
    protected boolean boldFont;
    private static String[] interpolationMethods;
    // Over/Under tresholding colors
    private static int overRed, overGreen=255, overBlue;
    private static int underRed, underGreen, underBlue=255;
    private static boolean useBicubic;
    private int sliceNumber;
    private Overlay overlay;

    ProgressBar progressBar;
    protected int width, snapshotWidth;
    protected int height, snapshotHeight;
    protected int roiX, roiY, roiWidth, roiHeight;
    protected int xMin, xMax, yMin, yMax;
    boolean snapshotCopyMode;
    ImageProcessor mask;
    protected ColorModel baseCM; // base color model
    protected ColorModel cm;
    protected byte[] rLUT1, gLUT1, bLUT1; // base LUT
    protected byte[] rLUT2, gLUT2, bLUT2; // LUT as modified by setMinAndMax and setThreshold
    protected boolean interpolate;  // replaced by interpolationMethod
    protected int interpolationMethod = NONE;
    protected double minThreshold=NO_THRESHOLD, maxThreshold=NO_THRESHOLD;
    protected int histogramSize = 256;
    protected double histogramMin, histogramMax;
    protected float[] cTable;
    protected boolean lutAnimation;
    protected MemoryImageSource source;
    protected Image img;
    protected boolean newPixels;
    protected Color drawingColor = Color.black;
    protected int clipXMin, clipXMax, clipYMin, clipYMax; // clip rect used by drawTo, drawLine, drawDot and drawPixel
    protected int justification = LEFT_JUSTIFY;
    protected int lutUpdateMode;
    protected WritableRaster raster;
    protected BufferedImage image;
    protected BufferedImage fmImage;
    protected ColorModel cm2;
    protected SampleModel sampleModel;
    protected static IndexColorModel defaultColorModel;
    protected boolean minMaxSet;

    protected void showProgress(double percentDone) {
        if (progressBar!=null)
            progressBar.show(percentDone);
    }

    /** @deprecated */
    protected void hideProgress() {
        showProgress(1.0);
    }

    /** Returns the width of this image in pixels. */
    public int getWidth() {
        return width;
    }

    /** Returns the height of this image in pixels. */
    public int getHeight() {
        return height;
    }



    protected void makeDefaultColorModel() {
        cm = getDefaultColorModel();
    }



    protected boolean inversionTested = false;



    /** Returns true if the image is using the default grayscale LUT. */
    public boolean isDefaultLut() {
        if (cm==null)
            makeDefaultColorModel();
        if (!(cm instanceof IndexColorModel))
            return false;
        IndexColorModel icm = (IndexColorModel)cm;
        int mapSize = icm.getMapSize();
        if (mapSize!=256)
            return false;
        byte[] reds = new byte[mapSize];
        byte[] greens = new byte[mapSize];
        byte[] blues = new byte[mapSize];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);
        boolean isDefault = true;
        for (int i=0; i<mapSize; i++) {
            if ((reds[i]&255)!=i || (greens[i]&255)!=i || (blues[i]&255)!=i) {
                isDefault = false;
                break;
            }
        }
        return isDefault;
    }




    /** Sets the ROI (Region of Interest) and clipping rectangle to the entire image. */
    public void resetRoi() {
        roiX=0; roiY=0; roiWidth=width; roiHeight=height;
        xMin=1; xMax=width-2; yMin=1; yMax=height-2;
        mask=null;
        clipXMin=0; clipXMax=width-1; clipYMin=0; clipYMax=height-1;
    }

    /** Returns a Rectangle that represents the current
     region of interest. */
    public Rectangle getRoi() {
        return new Rectangle(roiX, roiY, roiWidth, roiHeight);
    }

    /** Defines a byte mask that limits processing to an
     irregular ROI. Background pixels in the mask have
     a value of zero. */
    public void setMask(ImageProcessor mask) {
        this.mask = mask;
    }

    /** For images with irregular ROIs, returns a mask, otherwise,
     returns null. Pixels outside the mask have a value of zero. */
    public ImageProcessor getMask() {
        return mask;
    }



    static final double a = 0.5; // Catmull-Rom interpolation
    public static final double cubic(double x) {
        if (x < 0.0) x = -x;
        double z = 0.0;
        if (x < 1.0)
            z = x*x*(x*(-a+2.0) + (a-3.0)) + 1.0;
        else if (x < 2.0)
            z = -a*x*x*x + 5.0*a*x*x - 8.0*a*x + 4.0*a;
        return z;
    }


    protected SampleModel getIndexSampleModel() {
        if (sampleModel==null) {
            IndexColorModel icm = getDefaultColorModel();
            WritableRaster wr = icm.createCompatibleWritableRaster(1, 1);
            sampleModel = wr.getSampleModel();
            sampleModel = sampleModel.createCompatibleSampleModel(width, height);
        }
        return sampleModel;
    }

    /** Returns the default grayscale IndexColorModel. */
    public IndexColorModel getDefaultColorModel() {
        if (defaultColorModel==null) {
            byte[] r = new byte[256];
            byte[] g = new byte[256];
            byte[] b = new byte[256];
            for(int i=0; i<256; i++) {
                r[i]=(byte)i;
                g[i]=(byte)i;
                b[i]=(byte)i;
            }
            defaultColorModel = new IndexColorModel(8, 256, r, g, b);
        }
        return defaultColorModel;
    }





}
