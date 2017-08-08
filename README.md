# Fiji wrapper plugin around elastix for 3-D registration

For questions please contact: tischitischer@gmail.com

This repository conatins a Fiji script to run elastix registration algorithms; Fiji handles the data and runs elastix via system calls.

## Installation

- Install Fiji: https://fiji.sc/
- Windows
  - install elastix: http://elastix.isi.uu.nl/download.php
  - install corresponding Visual C++: http://www.microsoft.com/en-us/download/details.aspx?id=30679
    - see also here: http://elastix.isi.uu.nl/FAQ.php
- download and extract below file and put it into you Fiji plugin folder: 
	- https://github.com/tischi/fiji-plugin-registrationTools/raw/master/out/artifacts/registrationTools_.jar

## Run it

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
	

## Notes

- On windows there sometimes is an error pop-up window during the running, which you can ignore.

### Learn more

- Elastix manual: http://elastix.isi.uu.nl/download/elastix_manual_v4.8.pdf

