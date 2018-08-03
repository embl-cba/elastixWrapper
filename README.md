# Fiji wrapper plugin around elastix for 2-D and 3-D registration

This repository contains a Fiji plugin to run elastix registration algorithms via a GUI.

Fiji handles image the data, provides the GUI and runs elastix via system calls.

## Question

For questions please contact: christian.tischer@embl.de

## Learn more

- Elastix manual: http://elastix.isi.uu.nl/download/elastix_manual_v4.8.pdf

## Installation

### Install Fiji and the plugin

- Install Fiji: https://fiji.sc/
- Add the update site: EMBL-CBA
	- The plugin will be available here: [ Plugins > Registration > Elastix ]

### Install elastix binary

- Windows, MacOS and Linux
  - install elastix: http://elastix.isi.uu.nl/download.php
 
#### Windows specific requirements

- install the corresponding Visual C++: http://www.microsoft.com/en-us/download/details.aspx?id=30679
    - see also here: http://elastix.isi.uu.nl/FAQ.php

## Usage instructions

- Open the time-series that you want to register in Fiji
- Run this tool: [Plugins > Registration > Elastix > Compute Transformation (elastix)]
	- Note: Elastix is not Elastic, which is also a registration plugin :-)
- Installation folder: point to the folder where you installed elastix, e.g.
	- C:\Program Files\elastix_v4.8
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

- Using the Windows OS, there sometimes is an error pop-up window during the running, which you can ignore.
- Multi-channel images are currently not supported



