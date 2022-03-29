//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             B o o k                                            //
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
package org.audiveris.omr.sheet;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.Annotations;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.image.ImageLoading;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.score.OpusExporter;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.ScoreExporter;
import org.audiveris.omr.score.ScoreReduction;
import org.audiveris.omr.sheet.rhythm.Voices;
import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;

import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.text.Language;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Memory;
import org.audiveris.omr.util.NaturalSpec;
import org.audiveris.omr.util.OmrExecutors;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.ZipFileSystem;
import org.audiveris.omr.util.param.Param;
import org.audiveris.omr.util.param.StringParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Book} is the root class for handling a physical set of image input
 * files, resulting in one or several logical MusicXML scores.
 * <p>
 * A book instance generally corresponds to an input file containing one or several images, each
 * image resulting in a separate {@link Sheet} instance.
 * <p>
 * A sheet generally contains one or several systems.
 * An indented system (sometimes prefixed by part names) usually indicates a new movement.
 * Such indented system may appear in the middle of a sheet, thus (logical) movement frontiers do
 * not always match (physical) sheet frontiers.
 * <p>
 * A (super-) book may also contain (sub-) books to recursively gather a sequence of input files.
 * <p>
 * Methods are organized as follows:
 * <dl>
 * <dt>Administration</dt>
 * <dd>
 * <ul>
 * <li>{@link #getInputPath}</li>
 * <li>{@link #getAlias}</li>
 * <li>{@link #setAlias}</li>
 * <li>{@link #getRadix}</li>
 * <li>{@link #includeBook}</li>
 * <li>{@link #getOffset}</li>
 * <li>{@link #setOffset}</li>
 * <li>{@link #isDirty}</li>
 * <li>{@link #setDirty}</li>
 * <li>{@link #isModified}</li>
 * <li>{@link #setModified}</li>
 * <li>{@link #close}</li>
 * <li>{@link #closeFileSystem}</li>
 * <li>{@link #isClosing}</li>
 * <li>{@link #setClosing}</li>
 * <li>{@link #getLock}</li>
 * <li>{@link #openBookFile}</li>
 * <li>{@link #openBookFile(Path)}</li>
 * <li>{@link #openSheetFolder}</li>
 * <li>{@link #isUpgraded}</li>
 * <li>{@link #batchUpgradeBooks}</li>
 * <li>{@link #getVersion}</li>
 * <li>{@link #getVersionValue}</li>
 * <li>{@link #setVersionValue}</li>
 * <li>{@link #getStubsWithTableFiles}</li>
 * </ul>
 * </dd>
 *
 * <dt>SheetStubs</dt>
 * <dd>
 * <ul>
 * <li>{@link #createStubs}</li>
 * <li>{@link #loadSheetImage}</li>
 * <li>{@link #isMultiSheet}</li>
 * <li>{@link #getStub}</li>
 * <li>{@link #getStubs}</li>
 * <li>{@link #getStubs(java.util.Collection)}</li>
 * <li>{@link #getSheetsSelection}</li>
 * <li>{@link #setSheetsSelection}</li>
 * <li>{@link #getSelectedStubs}</li>
 * <li>{@link #getFirstValidStub}</li>
 * <li>{@link #getValidSelectedStubs}</li>
 * <li>{@link #getValidStubs}</li>
 * <li>{@link #getValidStubs(java.util.List)}</li>
 * <li>{@link #removeStub}</li>
 * <li>{@link #ids}</li>
 * <li>{@link #swapAllSheets}</li>
 * </ul>
 * </dd>
 *
 * <dt>Parameters</dt>
 * <dd>
 * <ul>
 * <li>{@link #promptedForUpgrade}</li>
 * <li>{@link #setPromptedForUpgrade}</li>
 * <li>{@link #getBinarizationFilter}</li>
 * <li>{@link #getOcrLanguages}</li>
 * <li>{@link #getProcessingSwitches}</li>
 * </ul>
 * </dd>
 *
 * <dt>Transcription</dt>
 * <dd>
 * <ul>
 * <li>{@link #resetTo}</li>
 * <li>{@link #transcribe}</li>
 * <li>{@link #reachBookStep}</li>
 * <li>{@link #updateScores}</li>
 * <li>{@link #reduceScores}</li>
 * <li>{@link #getScore}</li>
 * <li>{@link #getScores}</li>
 * <li>{@link #clearScores}</li>
 * <li>{@link #isMultiMovement}</li>
 * </ul>
 * </dd>
 *
 * <dt>Samples</dt>
 * <dd>
 * <ul>
 * <li>{@link #getSampleRepository}</li>
 * <li>{@link #getSpecificSampleRepository}</li>
 * <li>{@link #hasAllocatedRepository}</li>
 * <li>{@link #hasSpecificRepository}</li>
 * <li>{@link #sample}</li>
 * <li>{@link #annotate}</li>
 * </ul>
 * </dd>
 *
 * <dt>Artifacts</dt>
 * <dd>
 * <ul>
 * <li>{@link #getExportPathSansExt}</li>
 * <li>{@link #setExportPathSansExt}</li>
 * <li>{@link #getOpusExportPath}</li>
 * <li>{@link #getScoreExportPaths}</li>
 * <li>{@link #export}</li>
 * <li>{@link #getPrintPath}</li>
 * <li>{@link #setPrintPath}</li>
 * <li>{@link #print}</li>
 * <li>{@link #getBookPath}</li>
 * <li>{@link #store()}</li>
 * <li>{@link #store(java.nio.file.Path, boolean)}</li>
 * <li>{@link #storeBookInfo}</li>
 * <li>{@link #loadBook}</li>
 * </ul>
 * </dd>
 * </dl>
 *
 * <img src="doc-files/Book-Detail.png" alt="Book detals UML">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "book")
public class Book
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Book.class);

    /** File name for book internals in book file system: {@value}. */
    public static final String BOOK_INTERNALS = "book.xml";

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Related Audiveris version that last operated on this book. */
    @XmlAttribute(name = "software-version")
    private String version;

    /** Related Audiveris build that last operated on this book. */
    @XmlAttribute(name = "software-build")
    private String build;

    /** Sub books, if any. */
    @XmlElement(name = "sub-books")
    private final List<Book> subBooks;

    /** Book alias, if any. */
    @XmlAttribute(name = "alias")
    private String alias;

    /** Input path of the related image(s) file, if any. */
    @XmlAttribute(name = "path")
    @XmlJavaTypeAdapter(Jaxb.PathAdapter.class)
    private final Path path;

    /** Sheet offset of image file with respect to full work, if any. */
    @XmlAttribute(name = "offset")
    private Integer offset;

    /** Indicate if the book scores must be updated. */
    @XmlAttribute(name = "dirty")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean dirty = false;

    /** Handling of binarization filter parameter. */
    @XmlElement(name = "binarization")
    @XmlJavaTypeAdapter(FilterParam.Adapter.class)
    private FilterParam binarizationFilter;

    /** Handling of dominant language(s) for this book. */
    @XmlElement(name = "ocr-languages")
    @XmlJavaTypeAdapter(StringParam.Adapter.class)
    private StringParam ocrLanguages;

    /** Handling of processing switches for this book. */
    @XmlElement(name = "processing")
    @XmlJavaTypeAdapter(ProcessingSwitches.Adapter.class)
    private ProcessingSwitches switches;

    /** Specification of sheets selection, if any. */
    @XmlElement(name = "sheets-selection")
    private String sheetsSelection;

    /** Sequence of all sheets stubs got from image file. */
    @XmlElement(name = "sheet")
    private final List<SheetStub> stubs = new ArrayList<>();

    /** Logical scores for this book. */
    @XmlElement(name = "score")
    private final List<Score> scores = new ArrayList<>();

    // Transient data
    //---------------
    //
    /** Project file lock. */
    private final Lock lock = new ReentrantLock();

    /** The related file radix (file name without extension). */
    private String radix;

    /** File path where the book is kept. */
    private Path bookPath;

    /** File path where the book is printed. */
    private Path printPath;

    /** File path (without extension) where the MusicXML output is stored. */
    private Path exportPathSansExt;

    /** Flag to indicate this book is being closed. */
    private volatile boolean closing;

    /** Set if the book itself has been modified. */
    private boolean modified = false;

    /** Book-level sample repository. */
    private SampleRepository repository;

    /** Has book already been prompted for upgrade?. */
    private boolean promptedForUpgrade = false;

    /** Set of stubs that need to be upgraded. */
    private Set<SheetStub> stubsToUpgrade;


    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a Book with a path to an input images file.
     *
     * @param path the input image path (which may contain several images)
     */
    public Book (Path path)
    {
        Objects.requireNonNull(path, "Trying to create a Book with null path");

        this.path = path;
        subBooks = null;

        initTransients(FileUtil.getNameSansExtension(path).trim(), null);
    }

    /**
     * Create a meta Book, to be later populated with sub-books.
     * <p>
     * NOTA: This meta-book feature is not yet in use.
     *
     * @param nameSansExt a name (sans extension) for this book
     */
    public Book (String nameSansExt)
    {
        Objects.requireNonNull(nameSansExt, "Trying to create a meta Book with null name");

        path = null;
        subBooks = new ArrayList<>();

        initTransients(nameSansExt, null);
    }

    /**
     * No-arg constructor needed by JAXB.
     */
    private Book ()
    {
        path = null;
        subBooks = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // annotate //
    //----------//
    /**
     * Write the book symbol annotations.
     * <p>
     * Generate a whole zip file, in which each valid sheet is represented by a pair
     * composed of sheet image (.png) and sheet annotations (.xml).
     *
     * @param theStubs the stubs to process
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void annotate (List<SheetStub> theStubs)
    {
        Path root = null;

        try {
            final Path bookFolder = BookManager.getDefaultBookFolder(this);
            final Path path = bookFolder.resolve(
                    getRadix() + Annotations.BOOK_ANNOTATIONS_EXTENSION);
            root = ZipFileSystem.create(path);

            for (SheetStub stub : theStubs) {
                try {
                    LogUtil.start(stub);

                    final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());
                    final Sheet sheet = stub.getSheet();
                    sheet.annotate(sheetFolder);
                } catch (Exception ex) {
                    logger.warn("Error annotating {} {}", stub, ex.toString(), ex);
                } finally {
                    LogUtil.stopStub();
                }
            }

            logger.info("Book annotated as {}", path);
        } catch (IOException ex) {
            logger.warn("Error annotating book {} {}", this, ex.toString(), ex);
        } finally {
            if (root != null) {
                try {
                    root.getFileSystem().close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    //-------------------//
    // batchUpgradeBooks //
    //-------------------//
    /**
     * In batch, should we automatically upgrade book sheets?
     *
     * @return true if so
     */
    public static boolean batchUpgradeBooks ()
    {
        return constants.batchUpgradeBooks.isSet();
    }

    //-------------//
    // clearScores //
    //-------------//
    /**
     * Reset the book scores.
     */
    public void clearScores ()
    {
        scores.clear();
    }

    //-------//
    // close //
    //-------//
    /**
     * Delete this book instance, as well as its related resources.
     *
     * @param sheetNumber current sheet number in book, if any
     */
    public void close (Integer sheetNumber)
    {
        setClosing(true);

        // Close contained stubs/sheets

        // Close parameter dialog if any

        // Close browser if any

        // Remove from OMR instances
        OMR.engine.removeBook(this, sheetNumber);

        // Time for some cleanup...
        Memory.gc();

        logger.debug("Book closed.");
    }

    //-----------------//
    // closeFileSystem //
    //-----------------//
    /**
     * Close the provided (book) file system.
     *
     * @param fileSystem the book file system
     */
    public static void closeFileSystem (FileSystem fileSystem)
    {
        try {
            fileSystem.close();

            logger.info("Book file system closed.");
        } catch (IOException ex) {
            logger.warn("Could not close book file system " + ex, ex);
        }
    }

    //-------------//
    // createStubs //
    //-------------//
    /**
     * Create as many sheet stubs as there are images in the input image file.
     * A created stub is nearly empty, the related image will have to be loaded later.
     */
    public void createStubs ()
    {
        ImageLoading.Loader loader = ImageLoading.getLoader(path);

        if (loader != null) {
            final int imageCount = loader.getImageCount();
            loader.dispose();
            logger.info("{} sheet{} in {}", imageCount, ((imageCount > 1) ? "s" : ""), path);

            for (int i = 1; i <= imageCount; i++) {
                stubs.add(new SheetStub(this, i));
            }
        }
    }

    //-----------------//
    // createStubsTabs //
    //-----------------//
    /**
     * Insert stubs assemblies in UI tabbed pane.
     * <p>
     * GUI will focus on first valid stub, unless a stub number is provided.
     *
     * @param focus the stub number to focus upon, or null
     */


    //--------//
    // export //
    //--------//
    /**
     * Export this book scores using MusicXML format.
     * <p>
     * Assuming 'BOOK' is the radix of book name, several outputs can be considered:
     * <ul>
     * <li>If we don't use opus and the book contains a single score, it is exported as "BOOK.ext"
     * where "ext" is either "mxl" or "xml" depending upon whether compression is used.</li>
     * <li>If we don't use opus and the book contains several scores, it is exported as several
     * "BOOK.mvt#.ext" files, where "#" stands for the movement number and "ext" is either "mxl" or
     * "xml" depending upon whether compression is used.</li>
     * <li>If we use opus, everything goes into "BOOK.opus.mxl" as a single container file.</li>
     * </ul>
     *
     * @param theStubs  the valid selected stubs
     * @param theScores the scores to use
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void export (List<SheetStub> theStubs,
                        List<Score> theScores)
    {
        // Make sure material is ready
        transcribe(theStubs, theScores);

        // path/to/scores/Book
        final Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));
        final boolean sig = BookManager.useSignature();

        // Export each movement score
        final String bookName = bookPathSansExt.getFileName().toString();

        if (BookManager.useOpus()) {
            // Export the book as one opus file
            final Path opusPath = getOpusExportPath();

            try {
                new OpusExporter(this).export(opusPath, bookName, sig, theScores);
            } catch (Exception ex) {
                logger.warn("Could not export opus " + opusPath, ex);
            }
        } else {
            // Export the book as one or several movement files
            final Map<Score, Path> scoreMap = getScoreExportPaths(theScores);
            final boolean compressed = BookManager.useCompression();

            for (Entry<Score, Path> entry : scoreMap.entrySet()) {
                final Score score = entry.getKey();
                final Path scorePath = entry.getValue();
                final String scoreName = (!isMultiMovement()) ? bookName
                        : (bookName + OMR.MOVEMENT_EXTENSION + score.getId());

                try {
                    new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                } catch (Exception ex) {
                    logger.warn("Could not export score " + scoreName, ex);
                }
            }
        }
    }

    //-------------------//
    // getOpusExportPath //
    //-------------------//
    /**
     * Report the opus export path.
     * <p>
     * Using opus, everything goes into "BOOK.opus.mxl" as a single container file
     *
     * @return the target opus path
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Path getOpusExportPath ()
    {
        final Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));
        final String bookName = bookPathSansExt.getFileName().toString();

        return bookPathSansExt.resolveSibling(bookName + OMR.OPUS_EXTENSION);
    }

    //---------------------//
    // getScoreExportPaths //
    //---------------------//
    /**
     * Report the export path for each exported score (using no opus).
     * <ul>
     * <li>A <i>single-movement</i> book is exported as one "BOOK.ext" file.
     * <li>A <i>multi-movement</i> book is exported as several "BOOK.mvt#.ext" files,
     * where "#" stands for the movement number.
     * </ul>
     * Extension 'ext' is either "mxl" or "xml" depending upon whether compression is used or not.
     *
     * @param theScores the scores to export
     * @return the populated map of export paths, one per score
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Map<Score, Path> getScoreExportPaths (List<Score> theScores)
    {
        final Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));
        final String bookName = bookPathSansExt.getFileName().toString();
        final Map<Score, Path> pathMap = new LinkedHashMap<>();
        final boolean compressed = BookManager.useCompression();
        final String ext = compressed ? OMR.COMPRESSED_SCORE_EXTENSION : OMR.SCORE_EXTENSION;

        for (Score score : theScores) {
            final String scoreName = (!isMultiMovement()) ? bookName
                    : (bookName + OMR.MOVEMENT_EXTENSION + score.getId());
            pathMap.put(score, bookPathSansExt.resolveSibling(scoreName + ext));
        }

        return pathMap;
    }

    //----------//
    // getAlias //
    //----------//
    /**
     * Report the book name alias if any.
     *
     * @return book alias or null
     */
    public String getAlias ()
    {
        return alias;
    }

    //----------//
    // setAlias //
    //----------//
    /**
     * Set the book alias
     *
     * @param alias the book alias
     */
    public void setAlias (String alias)
    {
        this.alias = alias;
        radix = alias;
    }

    //--------------------//
    // promptedForUpgrade //
    //--------------------//
    /**
     * @return true if already prompted For Upgrade
     */
    public boolean promptedForUpgrade ()
    {
        return promptedForUpgrade;
    }

    //-----------------------//
    // setPromptedForUpgrade //
    //-----------------------//
    /**
     * Set promptedForUpgrade to true.
     */
    public void setPromptedForUpgrade ()
    {
        promptedForUpgrade = true;
    }

    //-----------------------//
    // getBinarizationFilter //
    //-----------------------//
    /**
     * Report the binarization filter defined at book level.
     *
     * @return the filter parameter
     */
    public FilterParam getBinarizationFilter ()
    {
        if (binarizationFilter == null) {
            binarizationFilter = new FilterParam();
            binarizationFilter.setParent(FilterDescriptor.defaultFilter);
        }

        return binarizationFilter;
    }

    //-------------//
    // getBookPath //
    //-------------//
    /**
     * Report where the book is kept.
     *
     * @return the path to book .omr file
     */
    public Path getBookPath ()
    {
        return bookPath;
    }

    //-----------------//
    // getBrowserFrame //
    //-----------------//
    /**
     * Create a dedicated frame, where book hierarchy can be browsed interactively.
     *
     * @return the created frame
     */


    //----------------------//
    // getExportPathSansExt //
    //----------------------//
    /**
     * Report the path (without extension) where book is to be exported.
     *
     * @return the book export path without extension, or null
     */
    public Path getExportPathSansExt ()
    {
        return exportPathSansExt;
    }

    //----------------------//
    // setExportPathSansExt //
    //----------------------//
    /**
     * Remember the path (without extension) where the book is to be exported.
     *
     * @param exportPathSansExt the book export path (without extension)
     */
    public void setExportPathSansExt (Path exportPathSansExt)
    {
        this.exportPathSansExt = exportPathSansExt;
    }

    //-------------------//
    // getFirstValidStub //
    //-------------------//
    /**
     * Report the first non-discarded stub in this book
     *
     * @return the first non-discarded stub, or null
     */
    public SheetStub getFirstValidStub ()
    {
        for (SheetStub stub : stubs) {
            if (stub.isValid()) {
                return stub;
            }
        }

        return null; // No valid stub found!
    }

    //--------------//
    // getInputPath //
    //--------------//
    /**
     * Report the path to the book image(s) input.
     *
     * @return the image input path
     */
    public Path getInputPath ()
    {
        return path;
    }

    //---------//
    // getLock //
    //---------//
    /**
     * Report the lock that protects book project file.
     *
     * @return book project lock
     */
    public Lock getLock ()
    {
        return lock;
    }

    //-----------------//
    // getOcrLanguages //
    //-----------------//
    /**
     * Report the OCR language(s) specification defined at book level, if any.
     *
     * @return the OCR language(s) spec
     */
    public Param<String> getOcrLanguages ()
    {
        if (ocrLanguages == null) {
            ocrLanguages = new StringParam();
            ocrLanguages.setParent(Language.ocrDefaultLanguages);
        }

        return ocrLanguages;
    }

    //-----------//
    // getOffset //
    //-----------//
    /**
     * Report the offset of this book, with respect to a containing super-book.
     *
     * @return the offset (in terms of number of sheets)
     */
    public Integer getOffset ()
    {
        return offset;
    }

    //-----------//
    // setOffset //
    //-----------//
    /**
     * Assign this book offset (WRT containing super-book)
     *
     * @param offset the offset to set
     */
    public void setOffset (Integer offset)
    {
        this.offset = offset;
    }

    //--------------------//
    // getParameterDialog //
    //--------------------//
    /**
     * Report the active parameter dialog, if any.
     *
     * @return the active parameter dialog, perhaps null
     */


    //--------------------//
    // setParameterDialog //
    //--------------------//
    /**
     * Register the provided dialog as the active parameter dialog.
     *
     * @param dialog new parameter dialog, perhaps null
     */


    //--------------//
    // getPrintPath //
    //--------------//
    /**
     * Report the path, if any, where book is to be printed.
     *
     * @return the print path, or null
     */
    public Path getPrintPath ()
    {
        return printPath;
    }

    //--------------//
    // setPrintPath //
    //--------------//
    /**
     * Remember to which path book print data is to be written.
     *
     * @param printPath the print path
     */
    public void setPrintPath (Path printPath)
    {
        this.printPath = printPath;
    }

    //-----------------------//
    // getProcessingSwitches //
    //-----------------------//
    /**
     * Report the processing switches defined at book level, if any.
     *
     * @return the processing switches
     */
    public ProcessingSwitches getProcessingSwitches ()
    {
        if (switches == null) {
            switches = new ProcessingSwitches(ProcessingSwitches.getDefaultSwitches());
        }

        return switches;
    }

    //----------//
    // getRadix //
    //----------//
    /**
     * Report the radix of the file that corresponds to the book.
     * It is based on the simple file name of the book, with no path and no extension.
     *
     * @return the book input file radix
     */
    public String getRadix ()
    {
        return radix;
    }

    //---------------------//
    // getSampleRepository //
    //---------------------//
    /**
     * Report the sample repository (specific or global) to populate for this book
     *
     * @return a specific book repository if possible, otherwise the global one
     */
    public SampleRepository getSampleRepository ()
    {
        SampleRepository repo = getSpecificSampleRepository();

        if (repo != null) {
            return repo;
        }

        // No specific repository is possible, so use global
        return SampleRepository.getGlobalInstance();
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score which contains the provided page.
     *
     * @param page provided page
     * @return containing score (can it be null?)
     */
    public Score getScore (Page page)
    {
        for (Score score : scores) {
            int pageIndex = score.getPageIndex(page);

            if (pageIndex != -1) {
                return score;
            }
        }

        return null;
    }

    //-----------//
    // getScores //
    //-----------//
    /**
     * Report the scores (movements) detected in this book.
     *
     * @return the live list of scores
     */
    public List<Score> getScores ()
    {
        return scores;
    }

    //------------------//
    // getSelectedStubs //
    //------------------//
    /**
     * Report the selected stubs according to selection specification.
     *
     * @return (copy of) the list of selected stubs (valid or not)
     */
    public List<SheetStub> getSelectedStubs ()
    {
        if (sheetsSelection == null) {
            return new ArrayList<>(stubs);
        }

        return getStubs(NaturalSpec.decode(sheetsSelection, true));
    }

    //--------------------//
    // getSheetsSelection //
    //--------------------//
    /**
     * Report the specification for sheets selection.
     *
     * @return the sheetsSelection string, perhaps null
     */
    public String getSheetsSelection ()
    {
        return sheetsSelection;
    }

    //--------------------//
    // setSheetsSelection //
    //--------------------//
    /**
     * Remember a new specification for sheets selection.
     *
     * @param sheetsSelection the sheetsSelection to set, perhaps null
     * @return true if the spec was actually modified
     */
    public boolean setSheetsSelection (String sheetsSelection)
    {
        boolean modif = false;

        if (sheetsSelection == null) {
            if (this.sheetsSelection != null) {
                modif = true;
            }
        } else {
            if (!sheetsSelection.equals(this.sheetsSelection)) {
                modif = true;
            }
        }

        this.sheetsSelection = sheetsSelection;

        if (modif) {
            setModified(true); // Book has been modified
        }

        return modif;
    }

    //-----------------------------//
    // getSpecificSampleRepository //
    //-----------------------------//
    /**
     * Report (after allocation if needed) the book <b>specific</b> sample repository
     *
     * @return the repository instance with material for this book only, or null
     */
    public SampleRepository getSpecificSampleRepository ()
    {
        if (repository == null) {
            repository = SampleRepository.getInstance(this, true);
        }

        return repository;
    }

    //---------//
    // getStub //
    //---------//
    /**
     * Report the sheet stub with provided id (counted from 1).
     *
     * @param sheetId the desired value for sheet id
     * @return the proper sheet stub, or null if not found
     */
    public SheetStub getStub (int sheetId)
    {
        return stubs.get(sheetId - 1);
    }

    //----------//
    // getStubs //
    //----------//
    /**
     * Report all the sheets stubs contained in this book.
     *
     * @return the immutable list of sheets stubs, list may be empty but is never null
     */
    public List<SheetStub> getStubs ()
    {
        return Collections.unmodifiableList(stubs);
    }

    //----------//
    // getStubs //
    //----------//
    /**
     * Report the sheets stubs corresponding to the provided sheet IDs.
     *
     * @param sheetIds list of IDs of desired stubs, perhaps null
     * @return the immutable list of selected sheets stubs, list may be empty but is never null
     */
    public List<SheetStub> getStubs (Collection<Integer> sheetIds)
    {
        if (sheetIds == null) {
            return getStubs();
        }

        final List<SheetStub> found = new ArrayList<>();

        for (int id : sheetIds) {
            if (id < 1 || id > stubs.size()) {
                logger.warn("No sheet #{} in {}", id, this);
            } else {
                found.add(stubs.get(id - 1));
            }
        }

        return found;
    }

    //-----------------------//
    // getValidSelectedStubs //
    //-----------------------//
    /**
     * Report the valid sheets among the sheets selection.
     *
     * @return the valid selected stubs
     */
    public List<SheetStub> getValidSelectedStubs ()
    {
        final List<SheetStub> sel = getSelectedStubs();

        for (Iterator<SheetStub> it = sel.iterator(); it.hasNext();) {
            final SheetStub stub = it.next();

            if (!stub.isValid()) {
                it.remove();
            }
        }

        return sel;
    }

    //---------------//
    // getValidStubs //
    //---------------//
    /**
     * Report the non-discarded sheets stubs in this book.
     *
     * @return the list of valid sheets stubs
     */
    public List<SheetStub> getValidStubs ()
    {
        return getValidStubs(stubs);
    }

    //---------------//
    // getValidStubs //
    //---------------//
    /**
     * Report the valid stubs among the provided stubs.
     *
     * @param theStubs the provided stubs
     * @return the list of valid sheets stubs
     */
    public static List<SheetStub> getValidStubs (List<SheetStub> theStubs)
    {
        List<SheetStub> valids = new ArrayList<>();

        for (SheetStub stub : theStubs) {
            if (stub.isValid()) {
                valids.add(stub);
            }
        }

        return valids;
    }

    //------------//
    // getVersion //
    //------------//
    public Version getVersion ()
    {
        return new Version(version);
    }

    //-----------------//
    // getVersionValue //
    //-----------------//
    public String getVersionValue ()
    {
        return version;
    }

    //-----------------//
    // setVersionValue //
    //-----------------//
    public void setVersionValue (String version)
    {
        this.version = version;
    }

    //------------------------//
    // hasAllocatedRepository //
    //------------------------//
    /**
     * Tell whether the book has allocated a dedicated sample repository.
     *
     * @return true if allocated
     */
    public boolean hasAllocatedRepository ()
    {
        return repository != null;
    }

    //-----------------------//
    // hasSpecificRepository //
    //-----------------------//
    /**
     * Tell whether the book has an existing specific sample repository.
     *
     * @return true if specific repository exists
     */
    public boolean hasSpecificRepository ()
    {
        if (repository != null) {
            return true;
        }

        // Look for needed files
        return SampleRepository.repositoryExists(this);
    }

    //------------------//
    // hideInvalidStubs //
    //------------------//
    /**
     * Hide stub assemblies of invalid sheets.
     */


    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the stub collection.
     *
     * @param theStubs the collection of stub instances
     * @return the string built
     */
    public static String ids (List<SheetStub> theStubs)
    {
        if (theStubs == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (SheetStub entity : theStubs) {
            sb.append("#").append(entity.getNumber());
        }

        sb.append("]");

        return sb.toString();
    }

    //-------------//
    // includeBook //
    //-------------//
    /**
     * Include a (sub) book into this (super) book.
     *
     * @param book the sub book to include
     */
    public void includeBook (Book book)
    {
        subBooks.add(book);
    }

    //-----------//
    // isClosing //
    //-----------//
    /**
     * Report whether this book is being closed.
     *
     * @return the closing flag
     */
    public boolean isClosing ()
    {
        return closing;
    }

    //------------//
    // setClosing //
    //------------//
    /**
     * Flag this book as closing.
     *
     * @param closing the closing to set
     */
    public void setClosing (boolean closing)
    {
        this.closing = closing;
    }

    //---------//
    // isDirty //
    //---------//
    /**
     * Report whether the book scores need to be reduced.
     *
     * @return true if dirty
     */
    public boolean isDirty ()
    {
        return dirty;
    }

    //----------//
    // setDirty //
    //----------//
    /**
     * Set the dirty flag.
     *
     * @param dirty the new flag value
     */
    public void setDirty (boolean dirty)
    {
        this.dirty = dirty;
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Report whether the book has been modified with respect to its persisted data.
     *
     * @return true if modified
     */
    public boolean isModified ()
    {
        if (modified) {
            return true; // The book itself is modified
        }

        if ((repository != null) && repository.isModified()) {
            return true; // The book sample repository is modified
        }

        for (SheetStub stub : stubs) {
            if (stub.isModified()) {
                return true; // This sheet is modified
            }
        }

        return false;
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Set the modified flag.
     *
     * @param modified the new flag value
     */
    public void setModified (boolean modified)
    {
        this.modified = modified;
    }

    //-----------------//
    // isMultiMovement //
    //-----------------//
    /**
     * Report whether this book contains several movements (scores).
     *
     * @return true if multi scores
     */
    public boolean isMultiMovement ()
    {
        return scores.size() > 1;
    }

    //------------//
    // isUpgraded //
    //------------//
    /**
     * Report whether the book has been upgraded with respect to its persisted data.
     *
     * @return true if upgraded
     */
    public boolean isUpgraded ()
    {
        for (SheetStub stub : stubs) {
            if (stub.isUpgraded()) {
                return true; // This sheet is upgraded
            }
        }

        return false;
    }

    //--------------//
    // isMultiSheet //
    //--------------//
    /**
     * Report whether this book contains several sheets.
     *
     * @return true for several sheets
     */
    public boolean isMultiSheet ()
    {
        return stubs.size() > 1;
    }

    //----------//
    // loadBook //
    //----------//
    /**
     * Load a book out of a provided book file.
     *
     * @param bookPath path to the (zipped) book file
     * @return the loaded book if successful
     */
    public static Book loadBook (Path bookPath)
    {
        StopWatch watch = new StopWatch("loadBook " + bookPath);
        Book book = null;

        try {
            logger.info("Loading book {}", bookPath);
            watch.start("book");

            // Open book file
            Path rootPath = ZipFileSystem.open(bookPath);

            // Load book internals (just the stubs) out of book.xml
            Path internalsPath = rootPath.resolve(BOOK_INTERNALS);

            try (InputStream is = Files.newInputStream(internalsPath, StandardOpenOption.READ)) {
                JAXBContext ctx = getJaxbContext();
                Unmarshaller um = ctx.createUnmarshaller();
                book = (Book) um.unmarshal(is);
                LogUtil.start(book);
                book.getLock().lock();
                rootPath.getFileSystem().close(); // Close book file

                boolean ok = book.initTransients(null, bookPath);

                if (!ok) {
                    logger.info("Discarded {}", bookPath);

                    return null;
                }

                book.checkScore(); // TODO: remove ASAP

                // Book successfully loaded (but sheets may need upgrade later).
                return book;
            }
        } catch (IOException |
                 JAXBException ex) {
            logger.warn("Error loading book " + bookPath + " " + ex, ex);

            return null;
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }

            if (book != null) {
                book.getLock().unlock();
            }

            LogUtil.stopBook();
        }
    }

    //----------------//
    // loadSheetImage //
    //----------------//
    /**
     * Actually load the image that corresponds to the specified sheet id.
     *
     * @param id specified sheet id
     * @return the loaded sheet image
     */
    public synchronized Bitmap loadSheetImage (int id)
    {
        try {
            if (!Files.exists(path)) {
                logger.warn("Book input {} not found", path);

                return null;
            }

            final ImageLoading.Loader loader = ImageLoading.getLoader(path);

            if (loader == null) {
                return null;
            }

            Bitmap img = loader.getImage(id);
            logger.info("Loaded image {} {}x{} from {}", id, img.getWidth(), img.getHeight(), path);

            loader.dispose();

            return img;
        } catch (IOException ex) {
            logger.warn("Error in book.loadSheetImage", ex);

            return null;
        }
    }

    //--------------//
    // openBookFile //
    //--------------//
    /**
     * Open the book file (supposed to already exist at location provided by
     * '{@code bookPath}' member) for reading or writing.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) book file system
     * @throws java.io.IOException if anything goes wrong
     */
    public Path openBookFile ()
            throws IOException
    {
        return ZipFileSystem.open(bookPath);
    }

    //--------------//
    // openBookFile //
    //--------------//
    /**
     * Open the book file (supposed to already exist at location provided by
     * '{@code bookPath}' parameter) for reading or writing.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @param bookPath book path name
     * @return the root path of the (zipped) book file system
     */
    public static Path openBookFile (Path bookPath)
    {
        if (bookPath == null) {
            throw new IllegalStateException("bookPath is null");
        }

        try {
            logger.debug("Book file system opened");

            FileSystem fileSystem = FileSystems.newFileSystem(bookPath, (ClassLoader) null);

            return fileSystem.getPath(fileSystem.getSeparator());
        } catch (FileNotFoundException ex) {
            logger.warn("File not found: " + bookPath, ex);
        } catch (IOException ex) {
            logger.warn("Error reading book:" + bookPath, ex);
        }

        return null;
    }

    //-----------------//
    // openSheetFolder //
    //-----------------//
    /**
     * Open (in the book zipped file) the folder for provided sheet number
     *
     * @param number sheet number (1-based) within the book
     * @return the path to sheet folder
     * @throws IOException if anything goes wrong
     */
    public Path openSheetFolder (int number)
            throws IOException
    {
        Path root = openBookFile();

        return root.resolve(INTERNALS_RADIX + number);
    }

    //-------//
    // print //
    //-------//
    /**
     * Print this book in PDF format.
     *
     * @param theStubs the valid selected stubs
     */
    public void print (List<SheetStub> theStubs)
    {
        System.out.println("I was here");
    }

    //---------------//
    // reachBookStep //
    //---------------//
    /**
     * Reach a specific step (and all needed intermediate steps) on valid selected
     * sheets of this book.
     *
     * @param target   the targeted step
     * @param force    if true and step already reached, sheet is reset and processed until step
     * @param theStubs the valid selected stubs
     * @return true if OK on all sheet actions
     */
    public boolean reachBookStep (final Step target,
                                  final boolean force,
                                  final List<SheetStub> theStubs)
    {
        try {
            logger.debug("reachStep {} force:{} stubs:{}", target, force, theStubs);

            if (!force) {
                // Check against the least advanced step performed across all sheets concerned
                Step least = getLeastStep(theStubs);

                if ((least != null) && (least.compareTo(target) >= 0)) {
                    return true; // Nothing to do
                }
            }

            // Launch the steps on each sheet
            long startTime = System.currentTimeMillis();
            logger.info("Book reaching {}{} on sheets:{}",
                        target, force ? " force" : "", ids(theStubs));

            try {
                boolean someFailure = false;

                if (isMultiSheet() && constants.processAllStubsInParallel.isSet()
                            && (OmrExecutors.defaultParallelism.getValue() == true)) {
                    // Process all stubs in parallel
                    List<Callable<Boolean>> tasks = new ArrayList<>();

                    for (final SheetStub stub : theStubs) {
                        tasks.add(() -> {
                            LogUtil.start(stub);

                            try {
                                boolean ok = stub.reachStep(target, force);

                                if (ok) {
                                    stub.swapSheet(); // Save sheet & global book info to disk
                                }

                                return ok;
                            } finally {
                                LogUtil.stopStub();
                            }
                        });
                    }

                    try {
                        List<Future<Boolean>> futures = OmrExecutors.getCachedLowExecutor()
                                .invokeAll(tasks);

                        for (Future<Boolean> future : futures) {
                            try {
                                if (!future.get()) {
                                    someFailure = true;
                                }
                            } catch (InterruptedException |
                                     ExecutionException ex) {
                                logger.warn("Future exception", ex);
                                someFailure = true;
                            }
                        }

                        return !someFailure;
                    } catch (InterruptedException ex) {
                        logger.warn("Error in parallel reachBookStep", ex);
                        someFailure = true;
                    }
                } else {
                    // Process one stub after the other
                    for (SheetStub stub : theStubs) {
                        LogUtil.start(stub);

                        try {
                            if (stub.reachStep(target, force)) {
                                stub.swapSheet(); // Save sheet & global book info to disk
                            } else {
                                someFailure = true;
                            }
                        } catch (Exception ex) {
                            // Exception (such as timeout) raised on stub
                            // Let processing continue for the other stubs
                            logger.warn("Error processing stub");
                            someFailure = true;
                        } finally {
                            LogUtil.stopStub();
                        }
                    }
                }

                return !someFailure;
            } finally {
                LogUtil.stopStub();

                long stopTime = System.currentTimeMillis();
                logger.debug("End of step set in {} ms.", (stopTime - startTime));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Error in performing " + target, ex);
        }

        return false;
    }

    //--------------//
    // reduceScores //
    //--------------//
    /**
     * Determine the logical parts of each score.
     *
     * @param theStubs  the valid selected stubs
     * @param theScores the scores to populate
     * @return the count of modifications done
     */
    public int reduceScores (List<SheetStub> theStubs,
                             List<Score> theScores)
    {
        int modifs = 0;

        for (Score score : theScores) {
            // (re) build the score logical parts
            modifs += new ScoreReduction(score).reduce(theStubs);

            // Voices connection across pages in score
            modifs += Voices.refineScore(score, theStubs);
        }

        if (modifs > 0) {
            if (theScores == this.scores) {
                setModified(true);
            }

            logger.info("Scores built: {}", theScores.size());
        }

        if (theScores == this.scores) {
            setDirty(false);
        }

        return modifs;
    }

    //------------//
    // removeStub //
    //------------//
    /**
     * Remove the specified sheet stub from the containing book.
     * <p>
     * Typically, when the sheet carries no music information, it can be removed from the book
     * (without changing the IDs of the sibling sheets in the book)
     *
     * @param stub the sheet stub to remove
     * @return true if actually removed
     */
    public boolean removeStub (SheetStub stub)
    {
        return stubs.remove(stub);
    }

    //---------//
    // resetTo //
    //---------//
    /**
     * Reset all valid selected sheets of this book to their gray or binary images.
     *
     * @param step either LOAD or BINARY step only
     */
    public void resetTo (Step step)
    {
        if (step != Step.LOAD && step != Step.BINARY) {
            logger.error("Method resetTo is reserved to LOAD and BINARY steps");
            return;
        }

        for (SheetStub stub : getValidSelectedStubs()) {
            if (stub.isDone(step)) {
                if (step == Step.LOAD) {
                    stub.resetToGray();
                } else {
                    stub.resetToBinary();
                }
            }
        }

        scores.clear();
    }

    //--------//
    // sample //
    //--------//
    /**
     * Write the book symbol samples into its sample repository.
     *
     * @param theStubs the selected valid stubs
     */
    public void sample (List<SheetStub> theStubs)
    {
        for (SheetStub stub : theStubs) {
            Sheet sheet = stub.getSheet();
            sheet.sample();
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store book to disk.
     *
     * @param bookPath   target path for storing the book
     * @param withBackup if true, rename beforehand any existing target as a backup
     */
    public void store (Path bookPath,
                       boolean withBackup)
    {
        Memory.gc(); // Launch garbage collection, to save on weak glyph references ...

        boolean diskWritten = false; // Has disk actually been written?

        // Backup existing book file?
        if (withBackup && Files.exists(bookPath)) {
            Path backup = FileUtil.backup(bookPath);

            if (backup != null) {
                logger.info("Previous book file renamed as {}", backup);
            }
        }

        Path root = null; // Root of the zip file system

        try {
            getLock().lock();
            checkRadixChange(bookPath);
            logger.debug("Storing book...");

            if ((this.bookPath == null)
                        || this.bookPath.toAbsolutePath().equals(bookPath.toAbsolutePath())) {
                if (this.bookPath == null) {
                    root = ZipFileSystem.create(bookPath);
                    diskWritten = true;
                } else {
                    root = ZipFileSystem.open(bookPath);
                }

                if (isModified() || isUpgraded()) {
                    storeBookInfo(root); // Book info (book.xml)
                    diskWritten = true;
                }

                // Contained sheets
                for (SheetStub stub : stubs) {
                    if (stub.isModified() || stub.isUpgraded()) {
                        final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());
                        stub.getSheet().store(sheetFolder, null);
                        diskWritten = true;
                    }
                }

                // Separate repository
                if ((repository != null) && repository.isModified()) {
                    repository.storeRepository();
                }
            } else {
                // Switch from old to new book file
                root = createBookFile(bookPath);
                diskWritten = true;

                storeBookInfo(root); // Book info (book.xml)

                // Contained sheets
                final Path oldRoot = openBookFile(this.bookPath);

                for (SheetStub stub : stubs) {
                    final Path oldSheetFolder = oldRoot.resolve(INTERNALS_RADIX + stub.getNumber());
                    final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());

                    // By default, copy existing sheet files
                    if (Files.exists(oldSheetFolder)) {
                        FileUtil.copyTree(oldSheetFolder, sheetFolder);
                    }

                    // Update modified sheet files
                    if (stub.isModified() || stub.isUpgraded()) {
                        stub.getSheet().store(sheetFolder, oldSheetFolder);
                    }
                }

                oldRoot.getFileSystem().close(); // Close old book file
            }

            this.bookPath = bookPath;

            if (diskWritten) {
                logger.info("Book stored as {}", bookPath);
            }
        } catch (Exception ex) {
            logger.warn("Error storing " + this + " to " + bookPath + " ex:" + ex, ex);
        } finally {
            if (root != null) {
                try {
                    root.getFileSystem().close();
                } catch (IOException ignored) {
                }
            }

            getLock().unlock();
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store book to disk, using its current book path.
     */
    public void store ()
    {
        if (bookPath == null) {
            logger.warn("Bookpath not defined");
        } else {
            store(bookPath, false);
        }
    }

    //---------------//
    // storeBookInfo //
    //---------------//
    /**
     * Store the book information (global info + stub steps) into book file system.
     *
     * @param root root path of book file system
     * @throws Exception if anything goes wrong
     */
    public void storeBookInfo (Path root)
            throws Exception
    {
        // Book version should always be the oldest of all sheets versions
        Version oldest = getOldestSheetVersion();

        if (oldest != null) {
            setVersionValue(oldest.value);
        }

        Path bookInternals = root.resolve(BOOK_INTERNALS);
        Files.deleteIfExists(bookInternals);
        Jaxb.marshal(this, bookInternals, getJaxbContext());
        setModified(false);
        logger.info("Stored {}", bookInternals);
    }

    //---------------//
    // swapAllSheets //
    //---------------//
    /**
     * Swap out all sheets, except the current one if any.
     */
    public void swapAllSheets ()
    {
        if (isModified() || isUpgraded()) {
            logger.info("{} storing", this);
            store();
        }

        SheetStub currentStub = null;

        for (SheetStub stub : stubs) {
            if (stub != currentStub) {
                stub.swapSheet();
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Book{");
        sb.append(radix);

        if ((offset != null) && (offset > 0)) {
            sb.append(" offset:").append(offset);
        }

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // transcribe //
    //------------//
    /**
     * Convenient method to perform all needed transcription steps on selected valid sheets
     * of this book and building the book score(s).
     *
     * @param theStubs  the valid selected stubs
     * @param theScores the collection of scores to populate
     * @return true if OK
     */
    public boolean transcribe (List<SheetStub> theStubs,
                               List<Score> theScores)
    {
        boolean ok = reachBookStep(Step.last(), false, theStubs);

        if (theScores.isEmpty()) {
            createScores(theStubs, theScores);
        }

        reduceScores(theStubs, theScores);

        return ok;
    }

    //--------------//
    // updateScores //
    //--------------//
    /**
     * Update the gathering of sheet pages into scores.
     * <p>
     * The question is which scores should we update.
     * Clearing all and rebuilding all is OK for pageRefs of all scores without loading sheets.
     * But doing so, we lose logicalPart information of <b>all</b> scores, and to rebuild it we'll
     * need to reload all valid sheets.
     * <p>
     * A better approach is to check the stub before and the stub after the current one.
     * This may result in the addition or the removal of scores.
     *
     * @param currentStub the current stub
     */
    public synchronized void updateScores (SheetStub currentStub)
    {
        if (scores.isEmpty()) {
            // Easy: allocate scores based on all relevant book stubs
            createScores(null, scores);
        } else {
            try {
                // Determine just the impacted pageRefs
                final SortedSet<PageRef> impactedRefs = new TreeSet<>();
                final int stubNumber = currentStub.getNumber();

                if (!currentStub.getPageRefs().isEmpty()) {
                    // Look in stub before current stub?
                    final PageRef firstPageRef = currentStub.getFirstPageRef();

                    if (!firstPageRef.isMovementStart()) {
                        final SheetStub prevStub = (stubNumber > 1) ? stubs.get(stubNumber - 2)
                                : null;

                        if (prevStub != null) {
                            final PageRef prevPageRef = prevStub.getLastPageRef();

                            if (prevPageRef != null) {
                                impactedRefs.addAll(getScore(prevPageRef).getPageRefs()); // NPE
                            }
                        }
                    }

                    // Take pages of current stub
                    impactedRefs.addAll(currentStub.getPageRefs());

                    // Look in stub after current stub?
                    final SheetStub nextStub = (stubNumber < stubs.size()) ? stubs.get(stubNumber)
                            : null;

                    if (nextStub != null) {
                        final PageRef nextPageRef = nextStub.getFirstPageRef();

                        if ((nextPageRef != null) && !nextPageRef.isMovementStart()) {
                            impactedRefs.addAll(getScore(nextPageRef).getPageRefs()); // NPE
                        }
                    }
                }

                // Determine and remove the impacted scores
                final List<Score> impactedScores = scoresOf(impactedRefs);
                Integer scoreIndex = null;

                if (!impactedScores.isEmpty()) {
                    scoreIndex = scores.indexOf(impactedScores.get(0));
                } else {
                    for (Score score : scores) {
                        if (score.getFirstPageRef().getSheetNumber() > stubNumber) {
                            scoreIndex = scores.indexOf(score);

                            break;
                        }
                    }
                }

                if (scoreIndex == null) {
                    scoreIndex = scores.size();
                }

                logger.debug("Impacted pages:{} scores:{}", impactedRefs, impactedScores);
                scores.removeAll(impactedScores);

                // Insert new score(s) to replace the impacted one(s)?
                if (!currentStub.isValid()) {
                    impactedRefs.removeAll(currentStub.getPageRefs());
                }

                insertScores(currentStub, impactedRefs, scoreIndex);
            } catch (Exception ex) {
                // This seems to result from inconsistency between scores info and stubs info.
                // Initial cause can be a sheet not marshalled (because of use by another process)
                // followed by a reload of now non-consistent book.xml

                // Workaround: Clear all scores and rebuild them from stubs info
                // (Doing so, we may lose logical-part informations)
                logger.info("Rebuilding score(s) from stubs information.");
                scores.clear();
                createScores(null, scores);
            }
        }
    }

    //--------------//
    // upgradeStubs //
    //--------------//


    //-------------------//
    // getStubsToUpgrade //
    //-------------------//


    //------------------------//
    // getStubsWithOldVersion //
    //------------------------//


    //------------------------//
    // getStubsWithTableFiles //
    //------------------------//
    /**
     * Report the stubs that still have table files.
     *
     * @return the set of stubs with table files, perhaps empty but not null
     */
    private Set<SheetStub> getStubsWithTableFiles ()
    {
        final Set<SheetStub> found = new LinkedHashSet<>();
        final Lock bookLock = getLock();
        bookLock.lock();

        try {
            final Path theBookPath = BookManager.getDefaultSavePath(this);

            if (!Files.exists(theBookPath)) {
                // No book project file yet
                return found;
            }

            final Path root = ZipFileSystem.open(theBookPath);
            for (SheetStub stub : stubs) {
                final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());

                for (Picture.TableKey key : Picture.TableKey.values()) {
                    final Path tablePath = sheetFolder.resolve(key + ".xml");
                    logger.debug("Checking existence of {}", tablePath);

                    if (Files.exists(tablePath)) {
                        found.add(stub);
                        break;
                    }
                }
            }

            root.getFileSystem().close();
        } catch (Exception ex) {
            logger.warn("Error browsing project file of {} {}", this, ex.toString(), ex);
        } finally {
            bookLock.unlock();
        }

        return found;
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        if ((binarizationFilter != null) && !binarizationFilter.isSpecific()) {
            binarizationFilter = null;
        }

        if ((ocrLanguages != null) && !ocrLanguages.isSpecific()) {
            ocrLanguages = null;
        }

        if ((switches != null) && switches.isEmpty()) {
            switches = null;
        }
    }

    //------------------//
    // checkRadixChange //
    //------------------//
    /**
     * If the (new) book name does not match current one, update the book radix
     * (and the title of first displayed sheet if any).
     *
     * @param bookPath new book target path
     */
    private void checkRadixChange (Path bookPath)
    {
        // Are we changing the target name WRT the default name?
        final String newRadix = FileUtil.avoidExtensions(bookPath.getFileName(), OMR.BOOK_EXTENSION)
                .toString().trim();

        if (!newRadix.equals(radix)) {
            // Update book radix
            radix = newRadix;

            // We are really changing the radix, so nullify all other paths
            exportPathSansExt = printPath = null;

        }
    }

    //------------//
    // checkScore // Dirty hack, to be removed ASAP
    //------------//
    private void checkScore ()
    {
        for (Score score : scores) {
            PageRef ref = score.getFirstPageRef();

            if (ref == null) {
                logger.info("Discarding invalid score data.");
                scores.clear();

                break;
            }
        }
    }

    //--------------//
    // createScores //
    //--------------//
    /**
     * Create scores out of (all or selected) valid book stubs.
     *
     * @param validSelectedStubs valid selected stubs, or null
     * @param theScores          the scores to populate
     */
    private void createScores (List<SheetStub> validSelectedStubs,
                               List<Score> theScores)
    {
        if (validSelectedStubs == null) {
            validSelectedStubs = getValidSelectedStubs();
        }

        Score score = null;

        // Group provided sheets pages into scores
        for (SheetStub stub : stubs) {
            // An invalid or not-selected or not-yet-processed stub triggers a score break
            if (!validSelectedStubs.contains(stub) || stub.getPageRefs().isEmpty()) {
                score = null;
            } else {
                for (PageRef pageRef : stub.getPageRefs()) {
                    if ((score == null) || pageRef.isMovementStart()) {
                        theScores.add(score = new Score());
                        score.setBook(this);
                    }

                    score.addPageRef(stub.getNumber(), pageRef);
                }
            }
        }

        logger.debug("Created scores:{}", theScores);
    }

    //-------------------//
    // getConcernedStubs //
    //-------------------//


    //--------------//
    // getLeastStep //
    //--------------//
    /**
     * Report the least advanced step reached among all provided stubs.
     *
     * @param theStubs the provided stubs
     * @return the least step, null if any stub has not reached the first step (LOAD)
     */
    private Step getLeastStep (List<SheetStub> theStubs)
    {
        Step least = Step.last();

        for (SheetStub stub : theStubs) {
            Step latest = stub.getLatestStep();

            if (latest == null) {
                return null; // This sheet has not been processed at all
            }

            if (latest.compareTo(least) < 0) {
                least = latest;
            }
        }

        return least;
    }

    //-----------------------//
    // getOldestSheetVersion //
    //-----------------------//
    /**
     * Report the oldest version among all (valid) sheet stubs.
     *
     * @return the oldest version found or null if none found
     */
    private Version getOldestSheetVersion ()
    {
        Version oldest = null;

        for (SheetStub stub : stubs) {
            if (stub.isValid()) {
                final Version stubVersion;
                final String stubVersionValue = stub.getVersionValue();

                if (stubVersionValue != null) {
                    stubVersion = new Version(stubVersionValue);
                } else {
                    // Stub without explicit version is assumed to have book version
                    stubVersion = getVersion();
                }

                if ((oldest == null) || oldest.compareWithLabelTo(stubVersion) > 0) {
                    oldest = stubVersion;
                }
            }
        }

        return oldest;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score if any that contains the provided PageRef.
     *
     * @param pageRef the provided page ref (sheet#, page#)
     * @return the containing score or null if not found
     */
    private Score getScore (PageRef pageRef)
    {
        for (Score score : scores) {
            PageRef ref = score.getPageRef(pageRef.getSheetNumber());

            if ((ref != null) && (ref.getId() == pageRef.getId())) {
                return score;
            }
        }

        return null;
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize transient data.
     *
     * @param nameSansExt book name without extension, if any
     * @param bookPath    full path to book .omr file, if any
     * @return true if OK
     */
    private boolean initTransients (String nameSansExt,
                                    Path bookPath)
    {
        if (binarizationFilter != null) {
            binarizationFilter.setParent(FilterDescriptor.defaultFilter);
        }

        if (ocrLanguages != null) {
            ocrLanguages.setParent(Language.ocrDefaultLanguages);
        }

        if (switches != null) {
            switches.setParent(ProcessingSwitches.getDefaultSwitches());
        }

        if (alias == null) {
            alias = checkAlias(getInputPath());

            if (alias != null) {
                nameSansExt = alias;
            }
        }

        if (nameSansExt != null) {
            radix = nameSansExt.trim();
        }

        if (bookPath != null) {
            this.bookPath = bookPath;

            if (nameSansExt == null) {
                radix = FileUtil.getNameSansExtension(bookPath).trim();
            }
        }

        if (build == null) {
            build = WellKnowns.TOOL_BUILD;
        }

        if (version == null) {
            version = WellKnowns.TOOL_REF;
        } else {
            System.out.println("I was here");
        }



        return true;
    }

    //--------------//
    // insertScores //
    //--------------//
    /**
     * Insert scores out of provided sequence of PageRef's.
     *
     * @param currentStub stub being processed
     * @param pageRefs    sequence of pageRefs
     * @param insertIndex insertion index in scores list
     */
    private void insertScores (SheetStub currentStub,
                               SortedSet<PageRef> pageRefs,
                               int insertIndex)
    {
        Score score = null;
        Integer stubNumber = null;
        int index = insertIndex;

        for (PageRef ref : pageRefs) {
            if (stubNumber == null) {
                // Very first
                score = null;
            } else if (stubNumber < (ref.getSheetNumber() - 1)) {
                // One or several stubs missing
                score = null;
            }

            if (ref.isMovementStart()) {
                // Movement start
                score = null;
            }

            if (score == null) {
                scores.add(index++, score = new Score());
                score.setBook(this);
            }

            score.addPageRef(ref.getSheetNumber(), ref);
            stubNumber = ref.getSheetNumber();
        }

        logger.debug("Inserted scores:{}", scores.subList(insertIndex, index));
    }

    //----------//
    // scoresOf //
    //----------//
    /**
     * Retrieve the list of scores that embrace the provided sequence of pageRefs.
     *
     * @param refs the provided pageRefs (sorted)
     * @return the impacted scores
     */
    private List<Score> scoresOf (SortedSet<PageRef> refs)
    {
        final List<Score> impacted = new ArrayList<>();

        if (!refs.isEmpty()) {
            final int firstNumber = refs.first().getSheetNumber();
            final int lastNumber = refs.last().getSheetNumber();

            for (Score score : scores) {
                if (score.getLastPageRef().getSheetNumber() < firstNumber) {
                    continue;
                }

                if (score.getFirstPageRef().getSheetNumber() > lastNumber) {
                    break;
                }

                List<PageRef> scoreRefs = new ArrayList<>(score.getPageRefs());
                scoreRefs.retainAll(refs);

                if (!scoreRefs.isEmpty()) {
                    impacted.add(score);
                }
            }
        }

        return impacted;
    }

    //------------//
    // checkAlias //
    //------------//
    private static String checkAlias (Path path)
    {
        // Alias?
        if (AliasPatterns.useAliasPatterns()) {
            final String nameSansExt = FileUtil.getNameSansExtension(path);

            return BookManager.getInstance().getAlias(nameSansExt);
        }

        return null;
    }

    //----------------//
    // createBookFile //
    //----------------//
    /**
     * Create a new book file system dedicated to this book at the location provided
     * by '{@code bookpath}' member.
     * If such file already exists, it is deleted beforehand.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) book file system
     */
    private static Path createBookFile (Path bookPath)
            throws IOException
    {
        if (bookPath == null) {
            throw new IllegalStateException("bookPath is null");
        }

        try {
            Files.deleteIfExists(bookPath);
        } catch (IOException ex) {
            logger.warn("Error deleting book: " + bookPath, ex);
        }

        // Make sure the containing folder exists
        Files.createDirectories(bookPath.getParent());

        // Make it a zip file
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(bookPath.toFile()));
        zos.close();

        // Finally open the book file just created
        return ZipFileSystem.open(bookPath);
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Book.class, RunTable.class);
        }

        return jaxbContext;
    }

    //~ Inner classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch for book loading?");

        private final Constant.Boolean processAllStubsInParallel = new Constant.Boolean(
                false,
                "Should we process all stubs of a book in parallel? (beware of many stubs)");

        private final Constant.Boolean checkBookVersion = new Constant.Boolean(
                true,
                "Should we check version of loaded book files?");

        private final Constant.Boolean resetOldBooks = new Constant.Boolean(
                false,
                "In batch, should we reset to binary the too old book files?");

        private final Constant.Boolean batchUpgradeBooks = new Constant.Boolean(
                false,
                "In batch, should we automatically upgrade all book sheets?");
    }

    //------------------//
    // OcrBookLanguages //
    //------------------//
    private static class OcrBookLanguages
            extends Param<String>
    {

        @Override
        public boolean setSpecific (String specific)
        {
            if ((specific != null) && specific.isEmpty()) {
                specific = null;
            }

            return super.setSpecific(specific);
        }

        /**
         * JAXB adapter to mimic XmlValue.
         */
        public static class Adapter
                extends XmlAdapter<String, OcrBookLanguages>
        {

            @Override
            public String marshal (OcrBookLanguages val)
                    throws Exception
            {
                if (val == null) {
                    return null;
                }

                return val.getSpecific();
            }

            @Override
            public OcrBookLanguages unmarshal (String str)
                    throws Exception
            {
                OcrBookLanguages ol = new OcrBookLanguages();
                ol.setSpecific(str);

                return ol;
            }
        }
    }
}
