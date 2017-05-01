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



/*
- This code contains modified parts of the HDF5 plugin from the Ronneberger group in Freiburg
 */

package registrationTools.dataStreamingTools;

//import bdv.util.BdvFunctions;
//import bdv.util.BdvSource;
//import io.scif.config.SCIFIOConfig;
//import io.scif.img.ImgIOException;
//import io.scif.img.SCIFIOImgPlus;
//import net.imglib2.img.Img;
//import net.imglib2.type.NativeType;
//import net.imglib2.type.numeric.RealType;
//import net.imglib2.type.numeric.integer.UnsignedShortType;
//import net.imglib2.util.Pair;
//import io.scif.img.ImgOpener;
//import net.imglib2.img.display.imagej.ImageJFunctions;
//import org.scijava.util.Bytes;


import registrationTools.VirtualStackOfStacks.*;
import registrationTools.bigDataTracker.BigDataTrackerPlugIn;
import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import registrationTools.utils.MonitorThreadPoolStatus;
import registrationTools.utils.Utils;
import ch.systemsx.cisd.hdf5.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.FileInfo;
import javafx.geometry.Point3D;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



//import net.imagej.ImageJ;

// todo: put filename as slice label!!
// todo: test
// todo: brightness and contrast adjustment upon loading does not work
// todo: can only some combobox fields be editable?
// todo: - find out why loading and saving info file is so slow
// todo: - save smaller info files
// todo: saving as tiff stacks does not always work, e.g. after object tracking
// todo: check if all files are parsed before allowing to "crop as new stream"
// todo: rearrange the GUI
// todo: consistency check the list lengths with different folders
// todo: stop loading thread upon closing of image
// todo: increase speed of Leica tif parsing, possible?
// todo: make 8 bit work

/**
 * Opens a folder of stacks as a virtual stack.
 *
 * */
public class DataStreamingTools {

    private static Logger logger = new IJLazySwingLogger();

    public DataStreamingTools()
    {
    }

    // TODO: split up in simpler methods
    public ImagePlus openFromDirectory(String directory, String channelTimePattern, String filterPattern, String hdf5DataSet, int numIOThreads)

    {
        String[][] fileLists; // files in sub-folders
        String[][][] ctzFileList;
        int t = 0, z = 0, c = 0;
        ImagePlus imp;
        String fileType = "not determined";
        FileInfo[] info;
        FileInfo fi0;
        String[] channelFolders = null;
        List<String> channels = null, timepoints = null;
        int nC = 0, nT = 0, nZ = 0, nX = 0, nY = 0, bitDepth = 16;

        if (channelTimePattern.equals(Utils.LOAD_CHANNELS_FROM_FOLDERS))
        {

            //
            // Check for sub-folders
            //

            logger.info("checking for sub-folders...");
            channelFolders = getFoldersInFolder(directory);
            if (channelFolders != null)
            {
                fileLists = new String[channelFolders.length][];
                for (int i = 0; i < channelFolders.length; i++)
                {
                    fileLists[i] = getFilesInFolder(directory + channelFolders[i], filterPattern);
                    if (fileLists[i] == null)
                    {
                        logger.info("no files found in folder: " + directory + channelFolders[i]);
                        return (null);
                    }
                }
                logger.info("found sub-folders => interpreting as channel folders.");
            }
            else
            {
                logger.info("no sub-folders found.");
                IJ.showMessage("No sub-folders found; please specify a different options for loading " +
                        "the channels");
                return (null);
            }

        }
        else
        {

            //
            // Get files in main directory
            //

            logger.info("checking for files in folder: " + directory);
            fileLists = new String[1][];
            fileLists[0] = getFilesInFolder(directory, filterPattern);

            if (fileLists[0] != null)
            {

                //
                // check if it is Leica single tiff SPIM files
                //

                Pattern patternLeica = Pattern.compile("LightSheet 2.*");
                for (String fileName : fileLists[0])
                {
                    if (patternLeica.matcher(fileName).matches())
                    {
                        fileType = "leica single tif";
                        logger.info("detected fileType: " + fileType);
                        break;
                    }
                }
            }

            if (fileLists[0] == null || fileLists[0].length == 0)
            {
                IJ.showMessage("No files matching this pattern were found: " + filterPattern);
                return null;
            }

        }

        //
        // generate a nC,nT,nZ fileList
        //

        if (fileType.equals("leica single tif"))
        {

            //
            // Do special stuff related to leica single files
            //

            Matcher matcherZ, matcherC, matcherT, matcherID;
            Pattern patternC = Pattern.compile(".*--C(.*).tif");
            Pattern patternZnoC = Pattern.compile(".*--Z(.*).tif");
            Pattern patternZwithC = Pattern.compile(".*--Z(.*)--C.*");
            Pattern patternT = Pattern.compile(".*--t(.*)--Z.*");
            Pattern patternID = Pattern.compile(".*?_(\\d+).*"); // is this correct?

            if (fileLists[0].length == 0)
            {
                IJ.showMessage("No files matching this pattern were found: " + filterPattern);
                return null;
            }

            // check which different fileIDs there are
            // those are three numbers after the first _
            // this happens due to restarting the imaging
            Set<String> fileIDset = new HashSet();
            for (String fileName : fileLists[0])
            {
                matcherID = patternID.matcher(fileName);
                if (matcherID.matches())
                {
                    fileIDset.add(matcherID.group(1));
                }
            }
            String[] fileIDs = fileIDset.toArray(new String[fileIDset.size()]);

            // check which different C, T and Z there are for each FileID

            ArrayList<HashSet<String>> channelsHS = new ArrayList();
            ArrayList<HashSet<String>> timepointsHS = new ArrayList();
            ArrayList<HashSet<String>> slicesHS = new ArrayList();

            //
            // Deal with different file-names (fileIDs) due to
            // series being restarted during the imaging
            //

            for (String fileID : fileIDs)
            {
                channelsHS.add(new HashSet());
                timepointsHS.add(new HashSet());
                slicesHS.add(new HashSet());
            }

            for (int iFileID = 0; iFileID < fileIDs.length; iFileID++)
            {

                Pattern patternFileID = Pattern.compile(".*?_" + fileIDs[iFileID] + ".*");

                for (String fileName : fileLists[0])
                {

                    if (patternFileID.matcher(fileName).matches())
                    {

                        matcherC = patternC.matcher(fileName);
                        if (matcherC.matches())
                        {
                            // has multi-channels
                            channelsHS.get(iFileID).add(matcherC.group(1));
                            matcherZ = patternZwithC.matcher(fileName);
                            if (matcherZ.matches())
                            {
                                slicesHS.get(iFileID).add(matcherZ.group(1));
                            }
                        }
                        else
                        {
                            // has only one channel
                            matcherZ = patternZnoC.matcher(fileName);
                            if (matcherZ.matches())
                            {
                                slicesHS.get(iFileID).add(matcherZ.group(1));
                            }
                        }

                        matcherT = patternT.matcher(fileName);
                        if (matcherT.matches())
                        {
                            timepointsHS.get(iFileID).add(matcherT.group(1));
                        }
                    }
                }

            }

            nT = 0;
            int[] tOffsets = new int[fileIDs.length + 1]; // last offset is not used, but added anyway
            tOffsets[0] = 0;

            for (int iFileID = 0; iFileID < fileIDs.length; iFileID++)
            {

                nC = Math.max(1, channelsHS.get(iFileID).size());
                nZ = slicesHS.get(iFileID).size(); // must be the same for all fileIDs

                logger.info("FileID: " + fileIDs[iFileID]);
                logger.info("  Channels: " + nC);
                logger.info("  TimePoints: " + timepointsHS.get(iFileID).size());
                logger.info("  Slices: " + nZ);

                nT += timepointsHS.get(iFileID).size();
                tOffsets[iFileID + 1] = nT;
            }


            //
            // Create dummy channel folders, because no real ones exist
            //

            channelFolders = new String[nC];
            for (int ic = 0; ic < nC; ic++) channelFolders[ic] = "";

            //
            // sort into the final file list
            //

            ctzFileList = new String[nC][nT][nZ];

            for (int iFileID = 0; iFileID < fileIDs.length; iFileID++)
            {

                Pattern patternFileID = Pattern.compile(".*" + fileIDs[iFileID] + ".*");

                for (String fileName : fileLists[0])
                {

                    if (patternFileID.matcher(fileName).matches())
                    {

                        // figure out which C,Z,T the file is
                        matcherC = patternC.matcher(fileName);
                        matcherT = patternT.matcher(fileName);
                        if (nC > 1) matcherZ = patternZwithC.matcher(fileName);
                        else matcherZ = patternZnoC.matcher(fileName);

                        if (matcherZ.matches())
                        {
                            z = Integer.parseInt(matcherZ.group(1).toString());
                        }
                        if (matcherT.matches())
                        {
                            t = Integer.parseInt(matcherT.group(1).toString());
                            t += tOffsets[iFileID];
                        }
                        if (matcherC.matches())
                        {
                            c = Integer.parseInt(matcherC.group(1).toString());
                        }
                        else
                        {
                            c = 0;
                        }

                        ctzFileList[c][t][z] = fileName;

                    }
                }
            }

            try
            {
                FastTiffDecoder ftd = new FastTiffDecoder(directory, ctzFileList[0][0][0]);
                info = ftd.getTiffInfo();
            } catch (Exception e)
            {
                info = null;
                IJ.showMessage("Error: " + e.toString());
            }

            fi0 = info[0];
            nX = fi0.width;
            nY = fi0.height;

        }
        else
        {

            //
            // either tif stacks or h5 stacks
            //

            boolean hasCTPattern = false;

            if (channelTimePattern.equals(Utils.LOAD_CHANNELS_FROM_FOLDERS))
            {

                nC = channelFolders.length;
                nT = fileLists[0].length;

            }
            else if (channelTimePattern.equals("None"))
            {

                nC = 1;
                nT = fileLists[0].length;

            }
            else
            {

                hasCTPattern = true;

                if (!(channelTimePattern.contains("<c>") && channelTimePattern.contains("<t>")))
                {
                    IJ.showMessage("The pattern for multi-channel loading must" +
                            "contain <c> and <t> to match channels and time in the filenames.");
                    return (null);
                }

                // replace shortcuts by actual regexp
                channelTimePattern = channelTimePattern.replace("<c>", "(?<C>.*)");
                channelTimePattern = channelTimePattern.replace("<t>", "(?<T>.*)");

                channelFolders = new String[]{""};

                HashSet<String> channelsHS = new HashSet();
                HashSet<String> timepointsHS = new HashSet();

                Pattern patternCT = Pattern.compile(channelTimePattern);

                for (String fileName : fileLists[0])
                {

                    Matcher matcherCT = patternCT.matcher(fileName);
                    if (matcherCT.matches())
                    {
                        channelsHS.add(matcherCT.group("C"));
                        timepointsHS.add(matcherCT.group("T"));
                    }

                }

                // convert HashLists to sorted Lists

                channels = new ArrayList<String>(channelsHS);
                Collections.sort(channels);
                nC = channels.size();

                timepoints = new ArrayList<String>(timepointsHS);
                Collections.sort(timepoints);
                nT = timepoints.size();

            }

            //
            // Create dummy channel folders, if no real ones exist
            //
            if (!channelTimePattern.equals(Utils.LOAD_CHANNELS_FROM_FOLDERS))
            {
                channelFolders = new String[nC];
                for (int ic = 0; ic < nC; ic++) channelFolders[ic] = "";
            }

            //
            // Get nX,nY,nZ from first file
            //


            if (fileLists[0][0].endsWith(".tif"))
            {

                fileType = "tif stacks";

                try
                {
                    FastTiffDecoder ftd = new FastTiffDecoder(directory + channelFolders[0], fileLists[0][0]);
                    info = ftd.getTiffInfo();
                } catch (Exception e)
                {
                    info = null;
                    IJ.showMessage("Error: " + e.toString());
                }

                fi0 = info[0];
                if (fi0.nImages > 1)
                {
                    nZ = fi0.nImages;
                    fi0.nImages = 1;
                }
                else
                {
                    nZ = info.length;
                }
                nX = fi0.width;
                nY = fi0.height;
                bitDepth = fi0.getBytesPerPixel() * 8;



            }
            else if (fileLists[0][0].endsWith(".h5"))
            {

                fileType = "h5";

                IHDF5Reader reader = HDF5Factory.openForReading(directory + channelFolders[c] + "/" + fileLists[0][0]);

                if (!hdf5DataSetExists(reader, hdf5DataSet)) return null;

                HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation("/" + hdf5DataSet);

                if ( dsInfo.getDimensions().length == 3 )
                {
                    nZ = (int) dsInfo.getDimensions()[0];
                    nY = (int) dsInfo.getDimensions()[1];
                    nX = (int) dsInfo.getDimensions()[2];
                }
                else if ( dsInfo.getDimensions().length == 2 )
                {
                    nZ = 1;
                    nY = (int) dsInfo.getDimensions()[0];
                    nX = (int) dsInfo.getDimensions()[1];
                }

                bitDepth = assignHDF5TypeToImagePlusBitdepth(dsInfo);

            }
            else
            {

                IJ.showMessage("Unsupported file type: " + fileLists[0][0]);
                return (null);

            }

            logger.info("File type: " + fileType);


            //
            // create the final file list
            //

            ctzFileList = new String[nC][nT][nZ];

            if (hasCTPattern)
            {

                // no sub-folders
                // channel and t determined by pattern matching

                Pattern patternCT = Pattern.compile(channelTimePattern);

                for (String fileName : fileLists[0])
                {

                    Matcher matcherCT = patternCT.matcher(fileName);
                    if (matcherCT.matches())
                    {
                        try
                        {
                            c = channels.indexOf(matcherCT.group("C"));
                            t = timepoints.indexOf(matcherCT.group("T"));
                        } catch (Exception e)
                        {
                            IJ.showMessage("The multi-channel loading did not match the filenames.\n" +
                                    "Please change the pattern.\n\n" +
                                    "The Java error message was:\n" +
                                    e.toString());
                            return (null);
                        }
                    }

                    for (z = 0; z < nZ; z++)
                    {
                        ctzFileList[c][t][z] = fileName; // all z with same file-name, because it is stacks
                    }

                }

            }
            else
            {

                for (c = 0; c < channelFolders.length; c++)
                {
                    for (t = 0; t < fileLists[c].length; t++)
                    {
                        for (z = 0; z < nZ; z++)
                        {
                            ctzFileList[c][t][z] = fileLists[c][t]; // all z with same file-name, because it is stacks
                        }
                    }
                }

            }

        }

        logger.info("Bit depth: " + bitDepth);


        //
        // init the virtual stack
        //
        VirtualStackOfStacks stack = new VirtualStackOfStacks(directory, channelFolders, ctzFileList, nC, nT, nX, nY, nZ, bitDepth, fileType, hdf5DataSet);
        imp = new ImagePlus("stream", stack);

        //
        // obtain file information for each channel, t, z
        //
        try
        {
            // Spawn the threads
            //
            ExecutorService es = Executors.newFixedThreadPool(numIOThreads);
            List<Future> futures = new ArrayList<>();
            for (t = 0; t < nT; t++)
            {
                futures.add(es.submit(new ParseFilesIntoVirtualStack(imp, t)));
            }


            // Monitor the progress
            //
            Thread thread = new Thread(new Runnable() {
                public void run()
                {
                    MonitorThreadPoolStatus.showProgressAndWaitUntilDone(
                            futures,
                            "Parsed files: ",
                            2000);
                }
            });
            thread.start();

        } catch (Exception e)
        {
            logger.error(e.toString());
        }

        return (imp);

    }

    public ImagePlus openFromInfoFile(String directory, String fileName)
    {

        File f = new File(directory + fileName);

        if (f.exists() && !f.isDirectory())
        {

            logger.info("Loading: " + directory + fileName);
            FileInfoSer[][][] infos = readFileInfosSer(directory + fileName);

            int nC = infos.length;
            int nT = infos[0].length;
            int nZ = infos[0][0].length;

            if (logger.isShowDebug())
            {
                logger.info("nC: " + infos.length);
                logger.info("nT: " + infos[0].length);
                logger.info("nz: " + infos[0][0].length);
            }

            // init the VSS
            VirtualStackOfStacks stack = new VirtualStackOfStacks(directory, infos);
            ImagePlus imp = createImagePlusFromVSS(stack);
            return (imp);

        }
        else
        {
            return (null);
        }
    }

    private boolean hdf5DataSetExists(IHDF5Reader reader, String hdf5DataSet)
    {
        String dataSets = "";
        boolean dataSetExists = false;

        if (reader.object().isDataSet(hdf5DataSet))
        {
            return true;
        }

        for (String dataSet : reader.getGroupMembers("/"))
        {
            /*
            if (dataSet.equals(hdf5DataSet)) {
                dataSetExists = true;
            }
            */
            dataSets += "- " + dataSet + "\n";
        }

        if (!dataSetExists)
        {
            IJ.showMessage("The selected Hdf5 data set does not exist; " +
                    "please change to one of the following:\n\n" +
                    dataSets);
        }

        return dataSetExists;
    }

    String[] sortAndFilterFileList(String[] rawlist, String filterPattern)
    {
        int count = 0;

        Pattern patternFilter = Pattern.compile(filterPattern);

        for (int i = 0; i < rawlist.length; i++)
        {
            String name = rawlist[i];
            if (!patternFilter.matcher(name).matches())
                rawlist[i] = null;
            else if (name.endsWith(".tif") || name.endsWith(".h5"))
                count++;
            else
                rawlist[i] = null;


        }
        if (count == 0) return null;
        String[] list = rawlist;
        if (count < rawlist.length)
        {
            list = new String[count];
            int index = 0;
            for (int i = 0; i < rawlist.length; i++)
            {
                if (rawlist[i] != null)
                    list[index++] = rawlist[i];
            }
        }
        int listLength = list.length;
        boolean allSameLength = true;
        int len0 = list[0].length();
        for (int i = 0; i < listLength; i++)
        {
            if (list[i].length() != len0)
            {
                allSameLength = false;
                break;
            }
        }
        if (allSameLength)
        {
            ij.util.StringSorter.sort(list);
            return list;
        }
        int maxDigits = 15;
        String[] list2 = null;
        char ch;
        for (int i = 0; i < listLength; i++)
        {
            int len = list[i].length();
            String num = "";
            for (int j = 0; j < len; j++)
            {
                ch = list[i].charAt(j);
                if (ch >= 48 && ch <= 57) num += ch;
            }
            if (list2 == null) list2 = new String[listLength];
            if (num.length() == 0) num = "aaaaaa";
            num = "000000000000000" + num; // prepend maxDigits leading zeroes
            num = num.substring(num.length() - maxDigits);
            list2[i] = num + list[i];
        }
        if (list2 != null)
        {
            ij.util.StringSorter.sort(list2);
            for (int i = 0; i < listLength; i++)
                list2[i] = list2[i].substring(maxDigits);
            return list2;
        }
        else
        {
            ij.util.StringSorter.sort(list);
            return list;
        }
    }

    String[] getFilesInFolder(String directory, String filterPattern)
    {
        // todo: can getting the file-list be faster?
        String[] list = new File(directory).list();
        if (list == null || list.length == 0)
            return null;
        list = this.sortAndFilterFileList(list, filterPattern);
        if (list == null) return null;
        else return (list);
    }

    String[] getFoldersInFolder(String directory)
    {
        //info("# getFoldersInFolder: " + directory);
        String[] list = new File(directory).list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name)
            {
                return new File(current, name).isDirectory();
            }
        });
        if (list == null || list.length == 0)
            return null;
        //list = this.sortFileList(list);
        return (list);

    }

    public boolean writeFileInfosSer(FileInfoSer[][][] infos, String path)
    {

        try
        {
            FileOutputStream fout = new FileOutputStream(path, true);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(infos);
            oos.flush();
            oos.close();
            fout.close();
            logger.info("Wrote: " + path);
            return true;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            logger.error("Writing failed: " + path);
            return false;
        }

    }

    public FileInfoSer[][][] readFileInfosSer(String path)
    {
        try
        {
            FileInputStream fis = new FileInputStream(path);
            ObjectInputStream ois = new ObjectInputStream(fis);
            FileInfoSer infos[][][] = (FileInfoSer[][][]) ois.readObject();
            ois.close();
            fis.close();
            return (infos);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    public static ImagePlus getCroppedVSS(ImagePlus imp, Roi roi, int zMin, int zMax, int tMin, int tMax)
    {

        int nt = tMax - tMin + 1;

        Point3D[] po = new Point3D[ nt ];
        Point3D ps = null;

        if (roi != null && roi.isArea())
        {
            for ( int t = 0; t < nt; t++ )
            {
                po[t] = new Point3D(roi.getBounds().getX(), roi.getBounds().getY(), zMin);
            }
            ps = new Point3D(roi.getBounds().getWidth(), roi.getBounds().getHeight(), zMax - zMin + 1);
        }
        else
        {
            logger.warning("No area ROI provided => no cropping in XY.");

            for ( int t = 0; t < nt; t++ )
            {
                po[t] = new Point3D(0, 0, zMin - 1);
            }
            ps = new Point3D(imp.getWidth(), imp.getHeight(), zMax - zMin + 1);

        }

        // Crop
        //
        ImagePlus impCropped = getCroppedVSS(imp, po, ps, tMin, tMax);
        impCropped.setTitle(imp.getTitle() + "-crop");
        return impCropped;

    }

    public static ImagePlus getCroppedVSS(ImagePlus imp, Point3D[] po, Point3D ps, int tMin, int tMax)
    {

        VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();
        FileInfoSer[][][] infos = vss.getFileInfosSer();

        int nC = infos.length;
        int nT = tMax - tMin + 1;
        int nZ = infos[0][0].length;

        FileInfoSer[][][] croppedInfos = new FileInfoSer[nC][nT][nZ];

        if (logger.isShowDebug())
        {
            logger.info("# DataStreamingTools.openCroppedFromInfos");
            logger.info("tMin: " + tMin);
            logger.info("tMax: " + tMax);
        }

        for (int c = 0; c < nC; c++)
        {

            for (int t = tMin; t <= tMax; t++)
            {

                for (int z = 0; z < nZ; z++)
                {

                    croppedInfos[c][t - tMin][z] = new FileInfoSer(infos[c][t][z]);
                    if (croppedInfos[c][t - tMin][z].isCropped)
                    {
                        croppedInfos[c][t - tMin][z].setCropOffset(po[t].add(croppedInfos[c][t - tMin][z].getCropOffset()));
                    }
                    else
                    {
                        croppedInfos[c][t - tMin][z].isCropped = true;
                        croppedInfos[c][t - tMin][z].setCropOffset(po[t - tMin]);
                    }
                    croppedInfos[c][t - tMin][z].setCropSize(ps);
                    //info("channel "+channel);
                    //info("t "+t);
                    //info("z "+z);
                    //info("offset "+croppedInfos[channel][t-tMin][z].pCropOffset.toString());

                }

            }

        }

        VirtualStackOfStacks parentStack = (VirtualStackOfStacks) imp.getStack();
        VirtualStackOfStacks stack = new VirtualStackOfStacks(parentStack.getDirectory(), croppedInfos);
        return (createImagePlusFromVSS(stack));

    }

    // TODO: is this method needed?
    private static ImagePlus createImagePlusFromVSS(VirtualStackOfStacks stack)
    {
        int nC = stack.getChannels();
        int nZ = stack.getDepth();
        int nT = stack.getFrames();
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        FileInfoSer[][][] infos = stack.getFileInfosSer();
        FileInfoSer fi = infos[0][0][0];

        ImagePlus imp = new ImagePlus("", stack);

        // todo: what does this do?
        if (imp.getType() == ImagePlus.GRAY16 || imp.getType() == ImagePlus.GRAY32)
            imp.getProcessor().setMinAndMax(min, max);

        imp.setFileInfo(fi.getFileInfo()); // saves FileInfo of the first image

        if (logger.isShowDebug())
        {
            logger.info("# DataStreamingTools.createImagePlusFromVSS");
            logger.info("nC: " + nC);
            logger.info("nZ: " + nZ);
            logger.info("nT: " + nT);
        }

        imp.setDimensions(nC, nZ, nT);
        imp.setOpenAsHyperStack(true);
        return (imp);
    }

    /**
     * @param imp
     * @param nIOthreads
     * @return
     */
    public static ImagePlus loadVSSFullyIntoRAM(ImagePlus imp, int nIOthreads)
    {

        // Initialize RAM image
        //
        ImageStack stack = imp.getStack();
        ImageStack stackRAM = ImageStack.create(stack.getWidth(), stack.getHeight(), stack.getSize(), stack.getBitDepth());
        ImagePlus impRAM = new ImagePlus("RAM", stackRAM);
        int[] dim = imp.getDimensions();
        impRAM.setDimensions(dim[2], dim[3], dim[4]);

        // Multi-threaded loading into RAM (increases speed if SSDs are available)
        //
        ExecutorService es = Executors.newFixedThreadPool(nIOthreads);
        List<Future> futures = new ArrayList<>();
        for (int t = 0; t < imp.getNFrames(); t++)
        {
            futures.add(es.submit(new LoadFrameFromVSSIntoRAM(imp, t, impRAM)));
        }


        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                MonitorThreadPoolStatus.showProgressAndWaitUntilDone(
                        futures,
                        "Loaded into RAM: ",
                        2000);
            }
        });
        thread.start();


        String info = (String) imp.getProperty("Info");
        if (info != null)
            impRAM.setProperty("Info", info);
        if (imp.isHyperStack())
            impRAM.setOpenAsHyperStack(true);

        return (impRAM);

    }

    public static void saveVSSAsStacks(ImagePlus imp, String bin, boolean saveVolume, boolean saveProjection,
                                       String filePath, Utils.FileType fileType,
                                       String compression, int rowsPerStrip, int threads)
    {

        // Do the jobs
        //
        ExecutorService es = Executors.newFixedThreadPool(threads);
        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < imp.getNFrames(); i++)
        {
            futures.add(es.submit(new SaveVSSFrame(imp, i, bin, saveVolume, saveProjection,
                    filePath, fileType, compression, rowsPerStrip)));
        }

        // Monitor the progress
        //
        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                MonitorThreadPoolStatus.showProgressAndWaitUntilDone(
                        futures,
                        "Saved to disk: ",
                        2000);
            }
        });
        thread.start();


    }


    class ParseFilesIntoVirtualStack implements Runnable {
        ImagePlus imp;
        private int t;

        ParseFilesIntoVirtualStack(ImagePlus imp, int t)
        {
            this.imp = imp;
            this.t = t;
        }

        public void run()
        {

            VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();

            for (int c = 0; c < vss.getChannels(); c++)
            {
                vss.setStackFromFile(t, c);
            }

            if (t == 0)
            {
                showImageAndInfo(vss);
            }

        }

        public void showImageAndInfo(VirtualStackOfStacks vss)
        {
            // show image
            //
            if (vss != null && vss.getSize() > 0)
            {
                imp = createImagePlusFromVSS(vss);
            }
            else
            {
                logger.error("Something went wrong loading the first image stack!");
                return;
            }

            Utils.show(imp);
            imp.setTitle("stream"); // TODO: get the selected directory as image name

            // show compression info
            //
            FileInfoSer[][][] infos = vss.getFileInfosSer();

            if (infos[0][0][0].compression == 0)
                logger.info("Compression = Unknown");
            else if (infos[0][0][0].compression == 1)
                logger.info("Compression = None");
            else if (infos[0][0][0].compression == 2)
                logger.info("Compression = LZW");
            else if (infos[0][0][0].compression == 6)
                logger.info("Compression = ZIP");
            else
                logger.info("Compression = " + infos[0][0][0].compression);

        }

    }


    static int assignHDF5TypeToImagePlusBitdepth(HDF5DataSetInformation dsInfo)
    {

        String type = dsInfoToTypeString( dsInfo );

        int nBits = 0;
        if (type.equals("uint8"))
        {
            nBits = 8;

        }
        else if (type.equals("uint16") || type.equals("int16"))
        {
            nBits = 16;
        }
        else if (type.equals("float32") || type.equals("float64"))
        {
            nBits = 32;
        }
        else
        {
            IJ.error("Type '" + type + "' Not handled yet!");
        }
        return nBits;
    }


    static String dsInfoToTypeString(HDF5DataSetInformation dsInfo)
    {
        HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();
        String typeText = "";

        if (dsType.isSigned() == false)
        {
            typeText += "u";
        }

        switch (dsType.getDataClass())
        {
            case INTEGER:
                typeText += "int" + 8 * dsType.getElementSize();
                break;
            case FLOAT:
                typeText += "float" + 8 * dsType.getElementSize();
                break;
            default:
                typeText += dsInfo.toString();
        }
        return typeText;
    }

}

    /*
    public < T extends RealType< T > & NativeType< T > >
            void openUsingSCIFIO(String path)
            throws ImgIOException
    {
        // Mouse over: intensity?

        //
        // Test SCIFIO and BIGDATAVIEWER
        //

        ImgOpener imgOpener = new ImgOpener();

        // Open as ArrayImg
        java.util.List<SCIFIOImgPlus<?>> imgs = imgOpener.openImgs( path );
        Img<T> img = (Img<T>) imgs.get(0);
        BdvSource bdv = BdvFunctions.show(img, "RAM");
        bdv.setDisplayRange(0,1000);

        // Open as CellImg
        SCIFIOConfig config = new SCIFIOConfig();
        config.imgOpenerSetImgModes( SCIFIOConfig.ImgMode.CELL );
        java.util.List<SCIFIOImgPlus<?>> cellImgs = imgOpener.openImgs( path, config );
        Img<T> cellImg = (Img<T>) cellImgs.get(0);
        BdvSource bdv2 = BdvFunctions.show(cellImg, "STREAM");
        bdv2.setDisplayRange(0,1000);

        /*
        First of all it is awesome that all of that works!
        As to be expected the visualisation of the of the cellImg is much slower.
        To be honest I was a bit surprised as to how slow it is given that it is only 2.8 MB and
        was stored on SSD on my MacBook Air.
        I was wondering about a really simple caching strategy for the BDV, in pseudo code:

        t = timePointToBeDisplayed
        taskRender(cellImg(t)).start()
        if(cellImg(t) fits in RAM):
          arrayImgThisTimePoint = cellImg(t).loadAsArrayImg()
          taskRender(cellImg(t)).end()
          taskRender(arrayImgThisTimePoint).start()

        Would that make any sense?
    }
       */

