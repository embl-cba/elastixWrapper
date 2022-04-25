# Elastix Wrapper

This repository contains the code for the Fiji plugin [Elastix](https://imagej.net/Elastix) for 2D and 3D image registration. The plugin is a wrapper around the image registration suite [elastix](https://elastix.lumc.nl/). Wrapping elastix into Fiji provides the convenience of easy image handling, visualisation and a graphical user interface.

## Citation

This plugin:

- Tischer, C. (2019). ElastixWrapper: Fiji plugin for 3D image registration with elastix. Zenodo. http://doi.org/10.5281/zenodo.2602549

elastix:

- S. Klein, M. Staring, K. Murphy, M.A. Viergever, J.P.W. Pluim, "elastix: a toolbox for intensity based medical image registration," IEEE Transactions on Medical Imaging, vol. 29, no. 1, pp. 196 - 205, January 2010.

- D.P. Shamonin, E.E. Bron, B.P.F. Lelieveldt, M. Smits, S. Klein and M. Staring, "Fast Parallel Image Registration on CPU and GPU for Diagnostic Classification of Alzheimer’s Disease", Frontiers in Neuroinformatics, vol. 7, no. 50, pp. 1-15, January 2014.

## Learn more

- [Elastix manual](https://elastix.lumc.nl/download/elastix-5.0.1-manual.pdf)

## Installation

### Install Fiji and the plugin

- Install Fiji: https://fiji.sc/
- Add the update site: ElastixWrapper
	- The plugins will be available here: [ Plugins > Registration > Elastix ]
	- Note: The ElastixWrapper update site has conflicts with the EMBL-CBA update site, which thus must be disabled.

### Install elastix binary

- Windows, MacOS and Linux
  - install elastix: http://elastix.isi.uu.nl/download.php
 
#### Windows specific requirements

- install the corresponding Visual C++: http://www.microsoft.com/en-us/download/details.aspx?id=30679
    - see also here: https://github.com/SuperElastix/elastix/wiki/FAQ

## Usage instructions

- Run: [Plugins > Registration > Elastix > Elastix]
- Installation folder: point to the folder where you installed elastix, e.g.
	- C:\Program Files\elastix_v4.8, or
	- /Applications/elastix_macosx64_v4.8/
- Transform:
	- Translation: 3-D shift
	- Euler: 3-D shift and rotation
	- Similarity: 3-D shift, rotation, and uniform scaling
	- Affine: 3-D shift, rotation, and 3-D scaling
	- BSpline: local deformations
- Iterations:
	- Number of iterations to find the best registration in each time-point
	- Try 1000
- Spatial samples:
	- Number of data points that will be used in each iteration for finding the registration
	- To use all data point type: full
	- Around 3000 often is a good number

## Further notes

- Using Windows OS, there sometimes is an error pop-up window during the running, which you can ignore.

## Groovy scripting

Run elastix: https://github.com/embl-cba/elastixWrapper/blob/master/scripts/run_elastix.groovy

## Running elastix from command line

Elastix can be called via ImageJ on the command line, as in this example:

```
/Applications/Fiji.app/Contents/MacOS/ImageJ-macosx --ij2 --headless --run "Elastix" "elastixDirectory='/Applications/elastix_macosx64_v4.8', workingDirectory='/Users/tischer/Desktop/elastix-tmp', fixedImageFile='/Users/tischer/Desktop/elastix-output/muscles.tif-transformed.tif', movingImageFile='/Users/tischer/Desktop/elastix-output/muscles.tif-transformed.tif', elastixParameters='Default',useMask='false',useInitialTransformation='false', transformationType='Translation',numIterations='1',numSpatialSamples='100', gaussianSmoothingSigmas='10,10,10',finalResampler='FinalLinearInterpolator', outputModality='Save output as Tiff'"
```

The transformed output images will be stored in the specified `workingDirectory`. 

To adapt it to your own application, the following parameters should be adapted:

- workingDirectory
    - please use another directory for storing your temporary data
- fixedImageFile
    - the reference image file against which you want to register
- movingImageFile
    - the image file, which is to be transformed
- transformationType
    - Translation: 3-D shift
    - Euler: 3-D shift and rotation
    - Similarity: 3-D shift, rotation, and uniform scaling
    - Affine: 3-D shift, rotation, and 3-D scaling
    - BSpline: local deformations
        - When using BSpline you need to add another key value pair to the parameter list, e.g.: `bSplineGridSpacing='50,50,50'`
- numSpatialSamples
    - around 3000 for 3D images can be enough
- numIterations
    - around 1000 typically is often enough
- finalResampler
    - FinalLinearInterpolator
        - for intensity images
    - FinalNearestNeighborInterpolator
        - for binary or label masks
- gaussianSmoothingSigmas
    - specify how much you want to smooth in pixels in 3D (my experience is that rather smoothing more is better than less)
    - you may specify a semicolon separated list of smoothing factors in order to do the registration at different resolutions, e.g. "10,10,10;1,1,1" will first do the registration with a 10x10x10 downsampled version of the data and then in a second step at full resolution.
- outputModality
    - Save transformed image as Tiff
        - This will save the transformed output image into the `workingDirectory` 

And just a reminder: Elastix works in physical units and it is thus important that your images are properly calibrated.

### Running elastix on EMBL slurm cluster

```
/g/almf/software/Fiji.app/ImageJ-linux64 --ij2 --headless --run "Elastix" "elastixDirectory='/g/almf/software/elastix_v4.8',workingDirectory='/g/almf/software/elastix-test/elastix-tmp',fixedImageFile='/g/almf/software/elastix-test/muscles.tif',movingImageFile='/g/almf/software/elastix-test/muscles.tif',elastixParameters='Default',useMask='false',useInitialTransformation='false', transformationType='Translation',numIterations='1',numSpatialSamples='100', gaussianSmoothingSigmas='10,10,10',finalResampler='FinalLinearInterpolator', outputModality='Save transformed image as Tiff'"
```

Above command should run on all cluster nodes; please adapt parameters as explained above.

## Running transformix from command line

Transformix can be called via ImageJ on the command line, as in this example:

```
/Applications/Fiji.app/Contents/MacOS/ImageJ-macosx --ij2 --headless --run "Transformix"  "elastixDirectory='/Applications/elastix_macosx64_v4.8', workingDirectory='/Users/tischer/Desktop/elastix-tmp', inputImageFile='/Users/tischer/Documents/detlev-arendt-clem-registration--data/data/prospr-new/muscles.tif',transformationFile='/Users/tischer/Documents/detlev-arendt-clem-registration/transformations-new/TransformParameters.Similarity-3Channels.0.txt',outputDirectory='/Users/tischer/Desktop/elastix-output',outputModality='Save as Tiff stack'"
```

The parameters, given as a comma separated key value pair list, are:

- elastixDirectory
    - installation directory of elastix
- workingDirectory
    - temporary directory
- inputImageFile
- transformationFile
- outputDirectory
- outputModality

### Running transformix on EMBL Slurm cluster

#### Executable command

```
/g/almf/software/Fiji.app/ImageJ-linux64 --ij2 --headless --run "Transformix" "elastixDirectory='/g/almf/software/elastix_v4.8', workingDirectory='/g/almf/software/elastix-test/tmp', inputImageFile='/g/almf/software/elastix-test/muscles.tif',transformationFile='/g/almf/software/elastix-test/TransformParameters.RotationPreAlign.0.txt',outputFile='/g/almf/software/elastix-test/out',outputModality='Save as BigDataViewer .xml/.h5'"
```

Above command should run on all cluster nodes.

To adapt it to your own application, the following parameters should be adapted:

- workingDirectory
    - please use another directory for storing your temporary data
- inputImageFile
    - your input file
- transformationFile
    - your transformation file, as generated using elastix 
- outputModality
    - 'Save as Tiff'
    - 'Save as BigDataViewer .xml/.h5'

And just a reminder: Elastix works in physical units and it is thus important that your images are properly calibrated.


#### Submit as cluster job

```
srun --mem 16000 -n 1 -N 1 -t 10:00 -o /g/cba/cluster/tischer/elastix-job-000.out -e /g/cba/cluster/tischer/elastix-job-000.err /g/almf/software/Fiji.app/ImageJ-linux64 --ij2 --headless --run "Transformix"  "elastixDirectory='/g/almf/software/elastix_v4.8', workingDirectory='/g/almf/software/elastix-test/tmp', inputImageFile='/g/almf/software/elastix-test/muscles.tif',transformationFile='/g/almf/software/elastix-test/TransformParameters.RotationPreAlign.0.txt',outputFile='/g/almf/software/elastix-test/out',outputModality='Save as BigDataViewer .xml/.h5'"
```

#### Submit MMPB transformation as a  cluster job

```
srun --mem 16000 -n 1 -N 1 -t 10:00 -o /g/cba/cluster/tischer/elastix-job-000.out -e /g/cba/cluster/tischer/elastix-job-000.err /g/almf/software/Fiji.app/ImageJ-linux64 --ij2 --headless --run "Transformix"  "elastixDirectory='/g/almf/software/elastix_v4.8', workingDirectory='/g/cba/tischer/tmp-elastix', inputImageFile='/g/cba/tischer/tmp/ProSPr6_Ref.tif',transformationFile='/g/cba/exchange/platy-trafos/linear/TransformParameters.Similarity-3Channels.0.txt',outputFile='/g/arendt/EM_6dpf_segmentation/EM-Prospr/ProSPr6_Ref-SPM',outputModality='Save as BigDataViewer .xml/.h5'" &
```

### Running elastix via API

Below link(s) point to example java code for using the API.
It should, hopefully, be rather straightforward to convert the java code into groovy or jython scripts.

- [ExampleElastixAPI](https://github.com/tischi/fiji-plugin-elastixWrapper/blob/master/src/test/java/ExampleElastixAPI.java#L11)

The example data that is used in above script(s) can be found here:

- [ellipsoid-at45degrees](https://github.com/tischi/fiji-plugin-elastixWrapper/blob/master/src/test/resources/ellipsoid-at45degrees-dxyz200nm.tif)
- [ellipsoid-horizontal](https://github.com/tischi/fiji-plugin-elastixWrapper/blob/master/src/test/resources/ellipsoid-horizontal-dxyz200nm.tif)
 


