# Fiji wrapper plugin around elastix for 3-D registration

For questions please contact: tischitischer@gmail.com

This repository conatins a Fiji script to run elastix registration algorithms; Fiji handles the data and runs elastix via system calls.

## Installation

### Install Fiji

- Install Fiji: https://fiji.sc/

### Install elastix binary

- Windows, MacOS and Linux
  - install elastix: http://elastix.isi.uu.nl/download.php
 
#### Windows specific requirements

- install the corresponding Visual C++: http://www.microsoft.com/en-us/download/details.aspx?id=30679
    - see also here: http://elastix.isi.uu.nl/FAQ.php

#### Linux and MacOS specific requirements

In your elastix folder please create a text file called:

`run_elastix.sh`

with the following content:

```
#!/bin/bash

PATH_ELASTIX=/g/almf/software/elastix_v4.8

export LD_LIBRARY_PATH=$PATH_ELASTIX/lib/
echo $LD_LIBRARY_PATH
$PATH_ELASTIX/bin/elastix $@
```

Please exchange the text following `PATH_ELASTIX=` with the correct path for your installation.

Finally, you also need to make this file executable by typing in your terminal window (in the folder where the script that you just created is):

`chmod +x run_elastix.sh`


### Install the Fiji elastix wrapper plugin

- download and extract below file and put it into you Fiji plugin folder: 
	- https://github.com/tischi/fiji-plugin-registrationTools/raw/master/out/artifacts/registrationTools_.jar




## Usage

- Open the time-series that you want to register in Fiji
- Run this tool: [Plugins > Registration > Elastix]
	- Note: Elastix is not Elastic, which is also a registration plugin :-)
- Installation folder: point to the folder where you installed elastix, e.g.
	- C:\Program Files\elastix_v4.8
- Transform:
	- Translation: 3-D shift
	- Euler: 3-D shift and rotation
	- Affine: 3-D shift, rotation and scaling
	- BSpline: local deformations
- Iterations:
	- Number of iterations to find the best registration in each time-point
	- Try 1000
- Spatial samples:
	- Number of data points that will be used in each iteration for finding the registration
	- To use all data point type: full
	- Often around 3000 is a good number
- Axial range:
    - This setting determines which z-range will be taken into account for computing the registration, however the whole image will be registered.
    - Note: This can be combined with a ROI (see below).
- ROI: 
   - If you place a rectangluar ROI on your image only this part of the image will be taken into account for computing the registration, however the whole image will be registered. 
    - Note: combining a rectangluar ROI with the "Axial range" settings enables you to select a 3-D data cube for computing the registration.
- Image background value:
    - This will value will be subtracted during the registration. This can help to improve the registration results. Currently also the final result will have this background value subtracted.


## Notes

- Using the Windows OS there sometimes is an error pop-up window during the running, which you can ignore.

- Multi-channel images are currently not supported


### Learn more

- Elastix manual: http://elastix.isi.uu.nl/download/elastix_manual_v4.8.pdf

