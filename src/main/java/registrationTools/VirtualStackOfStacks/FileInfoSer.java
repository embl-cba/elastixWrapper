/*
 * #%L
 * Data streaming, tracking and cropping tools
 * %%
 * Copyright (C) 2017 Christian Tischer
 *
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package registrationTools.VirtualStackOfStacks;

import ij.io.FileInfo;
import javafx.geometry.Point3D;

import java.io.Serializable;

/**
 * Created by tischi on 13/11/16.
 */
public class FileInfoSer implements Cloneable, Serializable {

    /* File format (TIFF, GIF_OR_JPG, BMP, etc.). Used by the File/Revert command */
    public int fileFormat;

    /* File type (GRAY8, GRAY_16_UNSIGNED, RGB, etc.) */
    public int fileType;

    public String fileName;
    public String directory;
    public String url;
    public int width;
    public int height;
    public long longOffset;
    public boolean intelByteOrder;
    public int compression;
    public int[] stripOffsets;
    public int[] stripLengths;
    public int rowsPerStrip;

    public double pixelWidth=1.0;
    public double pixelHeight=1.0;
    public double pixelDepth=1.0;
    public double frameInterval;

    // own stuff
    public int bytesPerPixel;
    public String h5DataSet;
    public String fileTypeString;
    public boolean isCropped = false;
    public int[] pCropOffset = new int[3];
    public int[] pCropSize = new int[3];

    // todo: there is a lot of duplicated information that would only be needed once

    //

    public FileInfoSer() {

    }

    public FileInfoSer(FileInfo info) {
        this.fileFormat = info.fileFormat;
        this.fileName = info.fileName;
        this.directory = info.directory;
        this.fileType = info.fileType;
        this.url = info.url;
        this.width = info.width;
        this.height = info.height;
        this.longOffset = info.getOffset();
        this.intelByteOrder = info.intelByteOrder;
        this.compression = info.compression;
        this.stripOffsets = info.stripOffsets;
        this.stripLengths = info.stripLengths;
        this.rowsPerStrip = info.rowsPerStrip;
        this.pixelWidth = info.pixelWidth;
        this.pixelHeight = info.pixelHeight;
        this.pixelDepth = info.pixelDepth;
        this.frameInterval = info.frameInterval;
        this.bytesPerPixel = info.getBytesPerPixel();
    }

    public FileInfoSer(FileInfoSer info) {
        this.fileFormat = info.fileFormat;
        this.fileName = info.fileName;
        this.directory = info.directory;
        this.fileType = info.fileType;
        this.url = info.url;
        this.width = info.width;
        this.height = info.height;
        this.longOffset = info.longOffset;
        this.intelByteOrder = info.intelByteOrder;
        this.compression = info.compression;
        this.stripOffsets = info.stripOffsets;
        this.stripLengths = info.stripLengths;
        this.rowsPerStrip = info.rowsPerStrip;
        this.pixelWidth = info.pixelWidth;
        this.pixelHeight = info.pixelHeight;
        this.pixelDepth = info.pixelDepth;
        this.frameInterval = info.frameInterval;
        this.bytesPerPixel = info.bytesPerPixel;
        this.h5DataSet = info.h5DataSet;
        this.fileTypeString = info.fileTypeString;
        this.isCropped = info.isCropped;
        this.pCropOffset = info.pCropOffset.clone();
        this.pCropSize = info.pCropSize.clone();
    }


    public FileInfo getFileInfo() {
        FileInfo fi = new FileInfo();
        fi.fileName = this.fileName;
        fi.fileFormat = this.fileFormat;
        fi.fileType = this.fileType;
        fi.directory = this.directory;
        fi.url = this.url;
        fi.width = this.width;
        fi.height = this.height;
        fi.longOffset = this.longOffset;
        fi.intelByteOrder = this.intelByteOrder;
        fi.compression = this.compression;
        fi.stripOffsets = this.stripOffsets;
        fi.stripLengths = this.stripLengths;
        fi.rowsPerStrip = this.rowsPerStrip;
        fi.pixelWidth = this.pixelWidth;
        fi.pixelHeight = this.pixelHeight;
        fi.pixelDepth = this.pixelDepth;
        fi.frameInterval = this.frameInterval;

        return(fi);
    }

    public synchronized Object clone() {
        try {return super.clone();}
        catch (CloneNotSupportedException e) {return null;}
    }

    public void setCropOffset(Point3D p) {
        pCropOffset[0] = (int) p.getX();
        pCropOffset[1] = (int) p.getY();
        pCropOffset[2] = (int) p.getZ();
    }

    public void setCropSize(Point3D p) {
        pCropSize[0] = (int) p.getX();
        pCropSize[1] = (int) p.getY();
        pCropSize[2] = (int) p.getZ();
    }

    public Point3D getCropOffset() {
        return(new Point3D(pCropOffset[0], pCropOffset[1], pCropOffset[2]));
    }

    public Point3D getCropSize() {
        return(new Point3D(pCropSize[0], pCropSize[1], pCropSize[2]));
    }



}
