package registrationTools;

/**
 MetaImage reader plugin for ImageJ.

 This plugin reads MetaImage text-based tagged format files.

 Author: Kang Li (kangli AT cs.cmu.edu)

 Installation:
 Download MetaImage_Reader_Writer.jar to the plugins folder, or subfolder.
 Restart ImageJ, and there will be new File/Import/MetaImage... and
 File/Save As/MetaImage... commands.

 History:
 2007/04/07: First version
 2008/07/25: Fixed two bugs (thanks to Jon Rohrer)

 References:
 MetaIO Documentation (http://www.itk.org/Wiki/MetaIO/Documentation)
 */

/**
 Copyright (C) 2007-2008 Kang Li. All rights reserved.

 Permission to use, copy, modify, and distribute this software for any purpose
 without fee is hereby granted,  provided that this entire notice  is included
 in all copies of any software which is or includes a copy or modification  of
 this software  and in  all copies  of the  supporting documentation  for such
 software. Any for profit use of this software is expressly forbidden  without
 first obtaining the explicit consent of the author.

 THIS  SOFTWARE IS  BEING PROVIDED  "AS IS",  WITHOUT ANY  EXPRESS OR  IMPLIED
 WARRANTY.  IN PARTICULAR,  THE AUTHOR  DOES NOT  MAKE ANY  REPRESENTATION OR
 WARRANTY OF ANY KIND CONCERNING  THE MERCHANTABILITY OF THIS SOFTWARE  OR ITS
 FITNESS FOR ANY PARTICULAR PURPOSE.
 */

////extionsions by Roman Grothausmann:
//01: also handle compressed data with InflaterInputStream
//02: make mha read correctly: 
//    1. only feed header to Propoerties.load() because load() interprets unicode escape sequences which are likely to occure in data part
//    2. only feed data to InflaterInputStream, therefore skip header on the exact byte count
//03: support for not compressed mha, clean up, optimization, ByteOrder fitting ITK, 64bit float support

import java.io.*;
import java.util.*;
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.io.*;

import java.util.zip.InflaterInputStream;
import java.net.*;
import java.awt.image.*;
import ij.gui.*;

class ExtendedFileOpener extends FileOpener {
    // private class which determines if the file is gzipped or not
    // and adds a GZIPInputStream if required
    public ExtendedFileOpener(FileInfo fi) {
        super(fi);
    }

    public InputStream createInputStream(FileInfo fi) throws IOException, MalformedURLException {
        // use the method in the FileOpener class to generate an inputstream
        InputStream is= super.createInputStream(fi);
        if (is!= null && fi.fileName.toLowerCase().endsWith(".zraw")) {
            // then stick a InflaterInputStream on top of it!
            //IJ.log("File ends with .zraw");
            return new InflaterInputStream(is);
        }
        else if(is!= null && fi.fileName.toLowerCase().endsWith(".mha")){
            ////need to skip header exactly, so do NOT use a buffered reader as InputStreamReader or BufferedReader!!!!
            //IJ.log("File ends with .mha");
            String line= "";
            int c;
            long bc= 0;
            do{
                line= "";
                do{ //read a line icluding \n
                    if((c= is.read()) < 0){
                        IJ.error("Header ended unexpectedly! Aborting at byte: " + bc);
                        System.exit(1);
                    }
                    bc++;
                    line+= (char)c; //cast very important otherwise int (number) is converted to str.
                } while (c != '\n');
                //IJ.log("Line: " + line);
            } while(!line.startsWith("ElementDataFile"));
            //IJ.log("Header length: " + bc);
            return new InflaterInputStream(is);
        }
        else {
            // Just return plain input stream
            return is;
        }
    }

}


public class MetaImage_Reader implements PlugIn {

    public boolean littleEndian = false;

    public void run(String arg) {
        OpenDialog od = new OpenDialog("Open MetaImage...", arg);
        String dir = od.getDirectory();
        String baseName = od.getFileName();
        if (baseName == null || baseName.length() == 0)
            return;
        int baseLength = baseName.length();
        String lowerBaseName = baseName.toLowerCase();
        boolean mhd = lowerBaseName.endsWith(".mhd");
        boolean mha = lowerBaseName.endsWith(".mha");
        boolean raw = lowerBaseName.endsWith(".raw");
        String headerName;
        if (mha || mhd) {
            headerName = baseName;
            baseName   = baseName.substring(0, baseLength - 4);
        }
        else {
            baseName   = baseName.substring(0, baseLength - 4);
            headerName = baseName + ".mhd";
        }
        IJ.showStatus("Opening " + headerName + "...");
        //ImagePlus imp = load(dir, name, headerName, mha);
        //if (imp != null)
        //    imp.show();
        //IJ.showStatus(baseName + " opened");
    }

    public ImagePlus load(String  dir,
                           String  name,
                           boolean local)
    {
        String headerName = name;
        String baseName   = name.substring(0, name.length() - 4);

        ImagePlus impOut = null;
        try {
            FileInfo fi = readHeader(dir, baseName, headerName, local);
            if (fi.fileName.equals("LIST")) {
                // Loads a sequence of files.
                BufferedReader in = new BufferedReader(new FileReader(dir + headerName));
                ImageStack stackOut = new ImageStack(fi.width, fi.height);
                boolean autoOffset = (fi.longOffset < 0);
                boolean fileNamesBegin = false;
                String line = in.readLine();
                int index = 0, numImages = fi.nImages;
                while (null != line) {
                    line = line.trim();
                    if (fileNamesBegin) {
                        String[] parts = line.split("\\s+");
                        for (int i = 0; i < parts.length; ++i) {
                            // Adjust the fields of FileInfo.
                            fi.nImages = 1;
                            fi.fileName = parts[i];
                            if (autoOffset)
                                fi.longOffset = getOffset(fi);
                            IJ.showStatus("Reading " + fi.fileName + "...");
                            FileOpener opener = new FileOpener(fi);
                            ImagePlus imp = opener.open(false);
                            ImageStack stack = imp.getStack();
                            for (int j = 1; j <= stack.getSize(); ++j) {
                                // Load the first image only even if there are more.
                                ImageProcessor ip = stack.getProcessor(j);
                                stackOut.addSlice(fi.fileName, ip);
                                break;
                            } // for j
                            if (++index >= numImages)
                                break;
                        } // for i
                    }
                    else {
                        if (line.startsWith("ElementDataFile"))
                            fileNamesBegin = true;
                    }
                    if (index >= numImages)
                        break;
                    line = in.readLine();
                }
                impOut = new ImagePlus(baseName, stackOut);
                impOut.setStack(null, stackOut);
            }
            else if (fi.fileName.indexOf('%') >= 0) {
                String[] parts = fi.fileName.split("\\s+");
                int imin = 1;
                int imax = fi.nImages;
                int step = 1;
                if (parts.length > 1) {
                    imin = Integer.parseInt(parts[1]);
                    if (parts.length > 2) {
                        imax = Integer.parseInt(parts[2]);
                        if (parts.length > 3)
                            step = Integer.parseInt(parts[3]);
                    }
                }
                boolean autoOffset = (fi.longOffset < 0);
                ImageStack stackOut = new ImageStack(fi.width, fi.height);
                int index = 0, numImages = fi.nImages;
                fi.nImages = 1;
                for (int i = imin; i <= imax; i += step) {
                    Formatter formatter = new Formatter();
                    formatter.format(parts[0], i);
                    fi.fileName = formatter.toString();
                    if (autoOffset)
                        fi.longOffset = getOffset(fi);
                    IJ.showStatus("Reading " + fi.fileName + "...");
                    FileOpener opener = new FileOpener(fi);
                    ImagePlus imp = opener.open(false);
                    ImageStack stack = imp.getStack();
                    for (int j = 1; j <= stack.getSize(); ++j) {
                        // Load the first image only even if there are more.
                        ImageProcessor ip = stack.getProcessor(j);
                        stackOut.addSlice(fi.fileName, ip);
                        break;
                    } // for j
                    if (++index >= numImages)
                        break;
                } // for i
                impOut = new ImagePlus(baseName, stackOut);
                impOut.setStack(null, stackOut);
            }
            //handling compressed data
            else if (fi.compression == FileInfo.COMPRESSION_UNKNOWN){ //sadly there is no FileInfo.GZIP therefore FlexibleFileOpener is used
                IJ.showStatus("Reading zlib-compressed " + fi.fileName + "...");
                //IJ.log("Reading zlib-compressed " + fi.fileName + "...");
                ExtendedFileOpener opener = new ExtendedFileOpener(fi); //use ExtendedFileOpener which adds ".zraw" as InflaterInputStream to FileOpener
                impOut= opener.open(false);//uses createInputStream which now handles zlib compression
            }
            else {
                if (fi.longOffset < 0)
                    fi.longOffset = getOffset(fi);
                //IJ.log("Reading not compressed " + fi.fileName + "...");
                IJ.showStatus("Reading " + fi.fileName + "...");
                FileOpener opener = new FileOpener(fi);
                impOut = opener.open(false);
            }
        }
        catch (IOException e) {
            IJ.error("MetaImage Reader: " + e.getMessage());
        }
        catch (NumberFormatException e) {
            IJ.error("MetaImage Reader: " + e.getMessage());
        }
        return impOut;
    }


    private FileInfo readHeader(String  dir,
                                String  baseName,
                                String  headerName,
                                boolean local) throws IOException, NumberFormatException
    {
        FileInfo fi = new FileInfo();
        fi.directory  = dir;
        fi.fileFormat = FileInfo.RAW;

        Properties p = new Properties();
        ////for mha it is necessary to only pass the header to properties
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dir + headerName)));
        String header= "";
        String line= "";
        do {
            line = in.readLine();
            header+= line + "\n";
        }while(line!= null && !line.startsWith("ElementDataFile"));
        //IJ.log("Header: " + header);
        long header_size= header.length();
        p.load(new ByteArrayInputStream(header.getBytes("ISO-8859-1"))); //load expects "ISO-8859-1" from a stream: http://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load%28java.io.InputStream%29

        String strObjectType = p.getProperty("ObjectType");
        String strNDims = p.getProperty("NDims");
        String strDimSize = p.getProperty("DimSize");
        String strElementSize = p.getProperty("ElementSize");
        String strElementDataFile = p.getProperty("ElementDataFile");
        String strElementByteOrderMSB = p.getProperty("ElementByteOrderMSB");
        if (null == strElementByteOrderMSB)
            strElementByteOrderMSB = p.getProperty("BinaryDataByteOrderMSB");
        String strElementNumberOfChannels = p.getProperty("ElementNumberOfChannels", "1");
        String strElementType = p.getProperty("ElementType", "MET_NONE");
        String strHeaderSize = p.getProperty("HeaderSize", "0");
        String strCompressedData = p.getProperty("CompressedData");

        if (strObjectType == null || !strObjectType.equalsIgnoreCase("Image"))
            throw new IOException("The specified file does not contain an image.");
        int ndims = Integer.parseInt(strNDims);
        if (strDimSize == null)
            throw new IOException("The image dimension size is unspecified.");
        else {
            String[] parts = strDimSize.split("\\s+");
            if (parts.length < ndims)
                throw new IOException("Invalid dimension size.");
            if (ndims > 1) {
                fi.width = Integer.parseInt(parts[0]);
                fi.height = Integer.parseInt(parts[1]);
                fi.nImages = 1;
                if (ndims > 2) {
                    for (int i = ndims - 1; i >= 2; --i)
                        fi.nImages *= Integer.parseInt(parts[i]);
                }
            }
            else {
                throw new IOException("Unsupported number of dimensions.");
            }
        }
        if (strElementSize != null) {
            String[] parts = strElementSize.split("\\s+");
            if (parts.length > 0)
                fi.pixelWidth  = Double.parseDouble(parts[0]);
            if (parts.length > 1)
                fi.pixelHeight = Double.parseDouble(parts[1]);
            if (parts.length > 2)
                fi.pixelDepth  = Double.parseDouble(parts[2]);
        }
        int numChannels = Integer.parseInt(strElementNumberOfChannels);
        if (numChannels == 1) {
            if (strElementType.equals("MET_UCHAR"))       { fi.fileType = FileInfo.GRAY8;           }
            else if (strElementType.equals("MET_CHAR"))        { fi.fileType = FileInfo.GRAY8;           }
            else if (strElementType.equals("MET_SHORT"))  { fi.fileType = FileInfo.GRAY16_SIGNED;   }
            else if (strElementType.equals("MET_USHORT")) { fi.fileType = FileInfo.GRAY16_UNSIGNED; }
            else if (strElementType.equals("MET_INT"))    { fi.fileType = FileInfo.GRAY32_INT;      }
            else if (strElementType.equals("MET_UINT"))   { fi.fileType = FileInfo.GRAY32_UNSIGNED; }
            else if (strElementType.equals("MET_FLOAT"))  { fi.fileType = FileInfo.GRAY32_FLOAT;    }
            else if (strElementType.equals("MET_DOUBLE")) { fi.fileType = FileInfo.GRAY64_FLOAT;    }
            else {
                throw new IOException(
                        "Unsupported element type: " +
                                strElementType +
                                ".");
            }
        }
        else if (numChannels == 3) {
            if (strElementType.equals("MET_UCHAR") || strElementType.equals("MET_UCHAR_ARRAY")) fi.fileType = FileInfo.RGB;
            else if (strElementType.equals("MET_USHORT") || strElementType.equals("MET_USHORT_ARRAY"))
                fi.fileType = FileInfo.RGB48;
            else {
                throw new IOException(
                        "Unsupported element type: " +
                                strElementType +
                                ".");
            }
        }
        else {
            throw new IOException("Unsupported number of channels.");
        }

        if (strElementDataFile != null && strElementDataFile.length() > 0) {
            if (strElementDataFile.equals("LOCAL")){
                fi.fileName = headerName;
                //IJ.log("Detected local data storage!" + fi.fileName);
            }
            else
                fi.fileName = strElementDataFile;
        }
        else {
            if (!local)
                fi.fileName = baseName + ".raw";
            else
                fi.fileName = headerName;
        }

        if (strElementByteOrderMSB != null) {
            if (strElementByteOrderMSB.length() > 0
                    && (strElementByteOrderMSB.charAt(0) == 'T' ||
                    strElementByteOrderMSB.charAt(0) == 't' ||
                    strElementByteOrderMSB.charAt(0) == '1'))
                fi.intelByteOrder = false;
            else
                fi.intelByteOrder = true;
        }
        //store compression info strCompressedData
        if (strCompressedData != null) {
            if (strCompressedData.length() > 0
                    && (strCompressedData.charAt(0) == 'T' ||
                    strCompressedData.charAt(0) == 't' ||
                    strCompressedData.charAt(0) == '1'))
                ////no FileInfo.ZLIB: http://rsbweb.nih.gov/ij/developer/api/constant-values.html#ij.io.FileInfo.ZIP
                fi.compression = FileInfo.COMPRESSION_UNKNOWN; //FileOpener.createInputStream will return null which will cause the ExtendedFileOpener to check for .zraw ;-)
        }

        fi.longOffset = (long)Integer.parseInt(strHeaderSize);
        if(local && fi.compression != FileInfo.COMPRESSION_UNKNOWN)
            fi.longOffset+= header_size;

        return fi;
    }


    private int getBytesPerPixel(FileInfo fi) {
        int bpp = 0;
        switch (fi.fileType) {
            case FileInfo.GRAY8:           return  1;
            case FileInfo.GRAY16_SIGNED:   return  2;
            case FileInfo.GRAY16_UNSIGNED: return  2;
            case FileInfo.GRAY32_INT:      return  4;
            case FileInfo.GRAY32_UNSIGNED: return  4;
            case FileInfo.GRAY32_FLOAT:    return  4;
            case FileInfo.RGB:             return 24;
            case FileInfo.RGB48:           return 48;
            default:
                break;
        }
        return bpp;
    }


    private long getOffset(FileInfo fi) {
        // Automatically calculate the header size.
        int bpp = getBytesPerPixel(fi);
        long dataBytes = bpp * fi.width * fi.height * fi.nImages;
        File file = new File(fi.directory + fi.fileName);
        return file.length() - dataBytes;
    }
}