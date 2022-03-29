/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.awt;


/**
 * The abstract class {@code Image} is the superclass of all
 * classes that represent graphical images. The image must be
 * obtained in a platform-specific manner.
 *
 * @author      Sami Shaio
 * @author      Arthur van Hoff
 * @since       1.0
 */
public abstract class Image {

    /**
     * Determines the width of the image. If the width is not yet known,
     * this method returns {@code -1} and the specified
     * {@code ImageObserver} object is notified later.
     * @param     observer   an object waiting for the image to be loaded.
     * @return    the width of this image, or {@code -1}
     *                   if the width is not yet known.
     * @see       java.awt.Image#getHeight
     * @see       java.awt.image.ImageObserver
     */
    public abstract int getWidth(ImageObserver observer);

    /**
     * Determines the height of the image. If the height is not yet known,
     * this method returns {@code -1} and the specified
     * {@code ImageObserver} object is notified later.
     * @param     observer   an object waiting for the image to be loaded.
     * @return    the height of this image, or {@code -1}
     *                   if the height is not yet known.
     */
    public abstract int getHeight(ImageObserver observer);

    /**
     * Gets the object that produces the pixels for the image.
     * This method is called by the image filtering classes and by
     * methods that perform image conversion and scaling.
     * @return     the image producer that produces the pixels
     *                                  for this image.
     * @see        java.awt.image.ImageProducer
     */
    public abstract ImageProducer getSource();

    /**
     * Gets a property of this image by name.
     * <p>
     * Individual property names are defined by the various image
     * formats. If a property is not defined for a particular image, this
     * method returns the {@code UndefinedProperty} object.
     * <p>
     * If the properties for this image are not yet known, this method
     * returns {@code null}, and the {@code ImageObserver}
     * object is notified later.
     * <p>
     * The property name {@code "comment"} should be used to store
     * an optional comment which can be presented to the application as a
     * description of the image, its source, or its author.
     * @param       name   a property name.
     * @param       observer   an object waiting for this image to be loaded.
     * @return      the value of the named property.
     * @throws      NullPointerException if the property name is null.
     * @see         java.awt.image.ImageObserver
     * @see         java.awt.Image#UndefinedProperty
     */
    public abstract Object getProperty(String name, ImageObserver observer);

    /**
     * The {@code UndefinedProperty} object should be returned whenever a
     * property which was not defined for a particular image is fetched.
     */
    public static final Object UndefinedProperty = new Object();

    /**
     * Flushes all reconstructable resources being used by this Image object.
     * This includes any pixel data that is being cached for rendering to
     * the screen as well as any system resources that are being used
     * to store data or pixels for the image if they can be recreated.
     * The image is reset to a state similar to when it was first created
     * so that if it is again rendered, the image data will have to be
     * recreated or fetched again from its source.
     * <p>
     * Examples of how this method affects specific types of Image object:
     * <ul>
     * <li>
     * BufferedImage objects leave the primary Raster which stores their
     * pixels untouched, but flush any information cached about those
     * pixels such as copies uploaded to the display hardware for
     * accelerated blits.
     * <li>
     * Image objects created by the Component methods which take a
     * width and height leave their primary buffer of pixels untouched,
     * but have all cached information released much like is done for
     * BufferedImage objects.
     * <li>
     * VolatileImage objects release all of their pixel resources
     * including their primary copy which is typically stored on
     * the display hardware where resources are scarce.
     * These objects can later be restored using their
     * {@link java.awt.image.VolatileImage#validate validate}
     * method.
     * <li>
     * Image objects created by the Toolkit and Component classes which are
     * loaded from files, URLs or produced by an {@link ImageProducer}
     * are unloaded and all local resources are released.
     * These objects can later be reloaded from their original source
     * as needed when they are rendered, just as when they were first
     * created.
     * </ul>
     */
    public void flush() {
        if (surfaceManager != null) {
            surfaceManager.flush();
        }
    }


}
