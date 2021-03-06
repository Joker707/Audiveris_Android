/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class extends {@code DataBuffer} and stores data internally as bytes.
 * Values stored in the byte array(s) of this {@code DataBuffer} are treated as
 * unsigned values.
 * <p>
 * <a id="optimizations">
 * Note that some implementations may function more efficiently
 * if they can maintain control over how the data for an image is
 * stored.
 * For example, optimizations such as caching an image in video
 * memory require that the implementation track all modifications
 * to that data.
 * Other implementations may operate better if they can store the
 * data in locations other than a Java array.
 * To maintain optimum compatibility with various optimizations
 * it is best to avoid constructors and methods which expose the
 * underlying storage as a Java array, as noted below in the
 * documentation for those methods.
 * </a>
 */
public final class DataBufferByte extends DataBuffer
{
    /** The default data bank. */
    byte data[];

    /** All data banks */
    byte bankdata[][];

    /**
     * Constructs a byte-based {@code DataBuffer} with a single bank and the
     * specified size.
     *
     * @param size The size of the {@code DataBuffer}.
     */
    public DataBufferByte(int size) {
        super(STABLE, TYPE_BYTE, size);
        data = new byte[size];
        bankdata = new byte[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a byte based {@code DataBuffer} with the specified number of
     * banks all of which are the specified size.
     *
     * @param size The size of the banks in the {@code DataBuffer}.
     * @param numBanks The number of banks in the a {@code DataBuffer}.
     */
    public DataBufferByte(int size, int numBanks) {
        super(STABLE, TYPE_BYTE, size, numBanks);
        bankdata = new byte[numBanks][];
        for (int i= 0; i < numBanks; i++) {
            bankdata[i] = new byte[size];
        }
        data = bankdata[0];
    }

    /**
     * Constructs a byte-based {@code DataBuffer} with a single bank using the
     * specified array, size, and offset.  {@code dataArray} must have at least
     * {@code offset} + {@code size} elements.  Only elements {@code offset}
     * through {@code offset} + {@code size} - 1
     * should be used by accessors of this {@code DataBuffer}.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray The byte array for the {@code DataBuffer}.
     * @param size The size of the {@code DataBuffer} bank.
     * @param offset The offset into the {@code dataArray}. {@code dataArray}
     * must have at least {@code offset} + {@code size} elements.
     */
    public DataBufferByte(byte dataArray[], int size, int offset){
        super(UNTRACKABLE, TYPE_BYTE, size, 1, offset);
        data = dataArray;
        bankdata = new byte[1][];
        bankdata[0] = data;
    }


    /**
     * Returns the default (first) byte data array.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @return The first byte data array.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the data array for the specified bank.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param bank The bank whose data array you want to get.
     * @return The data array for the specified bank.
     */
    public byte[] getData(int bank) {
        theTrackable.setUntrackable();
        return bankdata[bank];
    }

    /**
     * Returns the requested data array element from the first (default) bank.
     *
     * @param i The data array element you want to get.
     * @return The requested data array element as an integer.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int i) {
        return (int)(data[i+offset]) & 0xff;
    }

    /**
     * Returns the requested data array element from the specified bank.
     *
     * @param bank The bank from which you want to get a data array element.
     * @param i The data array element you want to get.
     * @return The requested data array element as an integer.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int bank, int i) {
        return (int)(bankdata[bank][i+offsets[bank]]) & 0xff;
    }

    /**
     * Sets the requested data array element in the first (default) bank
     * to the specified value.
     *
     * @param i The data array element you want to set.
     * @param val The integer value to which you want to set the data array element.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int i, int val) {
        data[i+offset] = (byte)val;
        theTrackable.markDirty();
    }

    /**
     * Sets the requested data array element in the specified bank
     * from the given integer.
     * @param bank The bank in which you want to set the data array element.
     * @param i The data array element you want to set.
     * @param val The integer value to which you want to set the specified data array element.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int bank, int i, int val) {
        bankdata[bank][i+offsets[bank]] = (byte)val;
        theTrackable.markDirty();
    }
}
