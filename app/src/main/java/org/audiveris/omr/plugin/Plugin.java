//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P l u g i n                                           //
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
package org.audiveris.omr.plugin;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.util.VoidTask;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Plugin} describes a plugin instance, encapsulating the relationship
 * with an external program to consume MusicXML export.
 * <p>
 * A plugin element is an XML fragment, made of:
 * <dl>
 * <dt>id</dt>
 * <dd>(mandatory attribute) Unique name for the plugin</dd>
 * <dt>tip</dt>
 * <dd>(optional attribute) A description text to appear as a user tip</dd>
 * <dt>arg</dt>
 * <dd>(mandatory element) There must be one arg element for each object in the command line.
 * A specific arg value ({}) indicates where to insert at run-time the path to MusicXML file.
 * </dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "plugin")
public class Plugin
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Plugin.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Id. */
    @XmlAttribute(name = "id")
    private final String id;

    /** Description used for tool tip. */
    @XmlAttribute(name = "tip")
    private final String tip;

    /** Sequence of arguments. */
    @XmlElement(name = "arg")
    private final List<String> args;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Plugin} object.
     *
     * @param id   Unique name
     * @param tip  Description text
     * @param args list of arguments
     */
    public Plugin (String id,
                   String tip,
                   List<String> args)
    {
        this.id = id;
        this.tip = tip;
        this.args = args;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private Plugin ()
    {
        id = null;
        tip = null;
        args = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // check //
    //-------//
    /**
     * Check if the plugin description is correct.
     *
     * @return true if OK
     */
    public boolean check ()
    {
        if ((id == null) || id.isEmpty()) {
            logger.warn("No id for {}", this);

            return false;
        }

        for (String arg : args) {
            if (arg.trim().equals("{}")) {
                return true;
            }
        }

        logger.warn("Missing special '{}' arg in plugin {}", getId());

        return false;
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a descriptive sentence for this plugin.
     *
     * @return a sentence meant for tool tip, perhaps null
     */
    public String getDescription ()
    {
        return tip;
    }

    //---------//
    // getname //
    //---------//
    /**
     * Report a name meant for user interface.
     *
     * @return a name for this plugin
     */
    public String getId ()
    {
        return id;
    }

    //---------//
    // getTask //
    //---------//


    //-----------//
    // runPlugin //
    //-----------//


    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append("id:\"").append(id).append("\"");

        if (tip != null) {
            sb.append(" tip:\"").append(tip).append("\"");
        }

        sb.append(" args:").append(args);
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // buildCli //
    //----------//
    /**
     * Build the CLI command.
     *
     * @param paths the export path(s) to process
     * @return the resulting CLI command
     */
    private List<String> buildCli (Collection<Path> paths)
    {
        final List<String> cli = new ArrayList<>();

        for (String arg : args) {
            // Special token to indicate where paths must be inserted
            if (arg.trim().equals("{}")) {
                for (Path path : paths) {
                    cli.add(path.toString());
                }
            } else {
                cli.add(arg);
            }
        }

        return cli;
    }

    //-------------//
    // deleteFiles //
    //-------------//
    /**
     * Delete all the provided files.
     *
     * @param paths paths to delete
     * @throws IOException
     */
    private void deleteFiles (Collection<Path> paths)
            throws IOException
    {
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    //------------//
    // exportIsOk //
    //------------//
    /**
     * Check if the provided path does exist and is dated after provided bookTime if any.
     *
     * @param bookTime time stamp of book file, perhaps null
     * @param path     required path
     * @return true if OK
     * @throws IOException can be thrown when checking file date
     */
    private boolean exportIsOk (FileTime bookTime,
                                Path path)
            throws IOException
    {
        if (!Files.exists(path)) {
            return false;
        }

        return (bookTime == null) || (Files.getLastModifiedTime(path).compareTo(bookTime) > 0);
    }

    //------------//
    // exportIsOk //
    //------------//
    /**
     * Check if all the provided paths do exist and are dated after provided bookTime
     * if any.
     *
     * @param bookTime time stamp of book file, perhaps null
     * @param paths    all required paths
     * @return true if OK
     * @throws IOException can be thrown when checking file date
     */
    private boolean exportIsOk (FileTime bookTime,
                                Collection<Path> paths)
            throws IOException
    {
        for (Path path : paths) {
            if (!exportIsOk(bookTime, path)) {
                return false;
            }
        }

        return true;
    }

    //---------------------//
    // retrieveExportFiles //
    //---------------------//
    /**
     * Try to get (retrieve or build) up-to-date export file(s).
     *
     * @param book the related book
     * @return paths to export file(s), null if no export file is usable
     */
    private Collection<Path> retrieveExportFiles (Book book)
    {
        try {
            // Target export file(s)
            final Collection<Path> paths = BookManager.useOpus()
                    ? Arrays.asList(book.getOpusExportPath())
                    : book.getScoreExportPaths(book.getScores()).values();

            // Reuse existing export?
            if (!book.isModified()) {
                final Path bookPath = book.getBookPath();

                if ((bookPath != null) && Files.exists(bookPath)) {
                    final FileTime bookTime = Files.getLastModifiedTime(bookPath); // .omr file time

                    if (exportIsOk(bookTime, paths)) {
                        return paths;
                    }
                }
            }

            // We must (re-) export
            deleteFiles(paths);
            book.export(book.getValidSelectedStubs(), book.getScores());

            if (exportIsOk(null, paths)) {
                return paths; // We can use new export
            }
        } catch (IOException ex) {
            logger.warn("Error getting export file(s) {}", ex.toString(), ex);
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // PluginTask //
    //------------//

}
