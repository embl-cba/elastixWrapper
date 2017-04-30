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



package registrationTools.testing;

import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.base.mdarray.MDShortArray;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.IJ;

import java.util.List;

import static ij.IJ.log;

public class OpenHdf5Test {

    Logger logger = new IJLazySwingLogger();

    public OpenHdf5Test() {

    }

    public ImagePlus openOneFileAsImp(String path) {
        ImagePlus imp = null;
        String dataSet = "Data444";
        IHDF5Reader reader = HDF5Factory.openForReading(path);
        browse(reader, reader.object().getGroupMemberInformation("/", true), "");

        HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation("/"+dataSet);
          logger.info("" + dsInfo.getRank());
        int nZ = (int)dsInfo.getDimensions()[0];
        int nY = (int)dsInfo.getDimensions()[1];
        int nX = (int)dsInfo.getDimensions()[2];
          logger.info("nx,ny,nz :" + nX + "," + nY + "," + nZ + ",");

        final MDShortArray block = reader.uint16().readMDArrayBlockWithOffset(dataSet, new int[] {1, 150, 150}, new long[] {(int)nZ/2,70,70} );
        final short[] asFlatArray = block.getAsFlatArray();

        imp = IJ.createHyperStack(dataSet, 150, 150, 1, 1, 1, 16);
        ImageProcessor ip = imp.getStack().getProcessor(imp.getStackIndex(1,1,1));
        System.arraycopy( asFlatArray, 0, (short[])ip.getPixels(),  0, asFlatArray.length);


        reader.close();
        imp.show();

        return(imp);
    }


    static void browse(IHDF5Reader reader, List<HDF5LinkInformation> members, String prefix)
    {
        for (HDF5LinkInformation info : members) {
            log(prefix + info.getPath() + ":" + info.getType());
            switch (info.getType())
            {
                case DATASET:
                    HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(info.getPath());
                    log(prefix + "     " + dsInfo);
                    break;
                case SOFT_LINK:
                    log(prefix + "     -> " + info.tryGetSymbolicLinkTarget());
                    break;
                case GROUP:
                    browse(reader, reader.object().getGroupMemberInformation(info.getPath(), true),
                            prefix + "  ");
                    break;
                default:
                    break;
            }
        }
    }

}
