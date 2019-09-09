# Instructions how to transform files for MMPB

- Login to the cluster login node.
 
- Generate a text file (job script) with below content (modifying the export statements as described below ). 

```
#!/bin/bash

export WORKING_DIR="/tmp/elastix"
export TRAFO="/g/cba/exchange/platy-trafos/linear/TransformParameters.BSpline10-3Channels.0.txt"
export OUT="/g/cba/cluster/tischer/elastix-job-000.out"
export ERR="/g/cba/cluster/tischer/elastix-job-000.err"
export INPUT_IMAGE="/g/cba/tischer/tmp/ProSPr6_Ref.tif"
export OUTPUT_IMAGE="/g/arendt/EM_6dpf_segmentation/EM-Prospr/ProSPr6_Ref-SPM.xml"

srun --mem 16000 -n 1 -N 1 -c 8 -t 30:00 -o $OUT -e $ERR mkdir -p $WORKING_DIR; /g/almf/software/Fiji.app/ImageJ-linux64 --ij2 --headless --run "Transformix" "elastixDirectory='/g/almf/software/elastix_v4.8', workingDirectory='$WORKING_DIR', inputImageFile='$INPUT_IMAGE',transformationFile='$TRAFO',outputFile='$OUTPUT_IMAGE',outputModality='Save as BigDataViewer .xml/.h5',numThreads='1'"
```

- Save this file, e.g. with the name `transformix.job`.
- Submit the file as a job: `sbatch transformix.job`


The `export` statements have to be adapted for your gene and user. 

- TRAFO
    - the transformation to be used
    - SPMs: /g/cba/exchange/platy-trafos/linear/TransformParameters.BSpline10-3Channels.0.txt
        - uses linear interpolation
    - MEDs: /g/cba/exchange/platy-trafos/nn/TransformParameters.BSpline10-3Channels.0.txt
        - ueses nearest neighbor interpolation
- OUT
    - text output
    - use: some file where you have write access
- ERR
    - text output of errors
    - use: some file where you have write access
- INPUT_IMAGE
    - the input image, make sure that the **calibration is correct**, otherwise the transformation will fail!
- OUTPUT_IMAGE
    - the output image, must be in folder `/g/arendt/EM_6dpf_segmentation/EM-Prospr/`
    - MEDs: must end with -MED.xml
    - SPMs: must enfd with -SPM.xml

## Alternative

## Notes

- Faster transformation for testing: `TransformParameters.Similarity-3Channels.0.txt`

## Issues

- I did not manage yet to specify a cluster job specific tmp dir. I tried using $TMPDIR, but it did not work. Like this, jobs running on the same node will screw up each other.
