# Instructions how to transform files for MMPB

Login to the cluster node and execute below lines of code in order to transform one file.

```
bash

export TRAFO="/g/cba/exchange/platy-trafos/linear/TransformParameters.BSpline10-3Channels.0.txt"
export OUT="/g/cba/cluster/tischer/elastix-job-000.out"
export ERR="/g/cba/cluster/tischer/elastix-job-000.err"
export INPUT_IMAGE="/g/cba/tischer/tmp/ProSPr6_Ref.tif"
export OUTPUT_IMAGE="/g/arendt/EM_6dpf_segmentation/EM-Prospr/ProSPr6_Ref-SPM.xml"

srun --mem 16000 -n 1 -N 1 -t 30:00 -o $OUT -e $ERR /g/almf/software/Fiji.app/ImageJ-linux64 --ij2 --headless --run "Transformix" "elastixDirectory='/g/almf/software/elastix_v4.8', workingDirectory='/tmp', inputImageFile='$INPUT_IMAGE',transformationFile='/g/cba/exchange/platy-trafos/linear/TransformParameters.BSpline10-3Channels.0.txt
',outputFile='$OUTPUT_IMAGE',outputModality='Save as BigDataViewer .xml/.h5',numThreads='1'" &
```

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
    - the input image, make sure that the *calibration is correct*, otherwise the transformation will fail!
- OUTPUT_IMAGE
    - the output image, must be in folder `/g/arendt/EM_6dpf_segmentation/EM-Prospr/`
    - MEDs: must end with -MED.xml
    - SPMs: must enfd with -SPM.xml
    



### Notes

- Faster transformation for testing: `TransformParameters.Similarity-3Channels.0.txt`
- It is super weird: setting `-n` to a value larger than `1` did execute the job multiple times...
- I did not manage yet to specify a cluster job specific tmp dir. I tried using $TMPDIR, but it did not work. Like this, jobs running on the same node will screw up each other.
