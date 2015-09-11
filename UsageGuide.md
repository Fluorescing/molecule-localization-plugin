# Introduction #

The purpose of the M2LE plugin is to find a locate single fluorescent molecules from an image or set of images.

# Source Images #

Images, to be analyzed, must be grayscaled (8-bit, 16-bit, 32-bit) images.  The source images can consist of a single image or a stack of images.  A single image or set of images from `tiff` file can be opened in ImageJ by going to **File->Open...**.  A stack of images can also be produced by selecting **File->Import->Image Sequence...** and selecting the folder to open.

# Analysis #

To start using M2LE, first make sure to first click on the image in question so that it has the focus.  Next, goto **Plugins->M2LE Pipeline->M2LE Localization**.  A window "M2LE Options" should pop up; this window contains the options for different aspects of the plugin (A guide to the options can be found in the next section).  To begin processing, press "OK"; otherwise, the "Cancel" button can be pressed to return without analysis.  A Localization Results table will appear after running (this may take some time depending on the computer and the number of images).

# Localization Options #
The following are descriptions for the user-definable constants and settings:

## Image Analysis Options ##
  * **Signal-to-Noise Cutoff:** This is a constant that defines at what multiple of background noise intensity (per pixel) should pixel intensities be brighter than to be considered as potential molecules.  Increase this value if background noise is being detected as molecules.  Decrease this value if very few molecules are being found that should otherwise be found.
  * **Lowest Noise Estimate:** This option sets a lower limit on the background noise (per pixel) estimate.
  * **Pixel Size:** The physical length that the side of a pixel represents; in nanometers.
  * **Saturation Point:** This is the digital number or number of photons that will cause saturation (e.g. 255 8-bit images, 65535 for 16-bit images).

## Debug Options ##
  * **Debug Mode/Debug Table:** It is not recommened and should be disabled by default.  This option has no definite well-defined behavior as it is meant for debugging purposes only.

## Molecule Rejection Options ##
  * **Ellipticity Threshold:** This threshold affects which potential multi-molecule images are rejected.  Lower the value to make the selection more strict.  The value must fall between 0 and 1.
  * **Ellipticity Threshold:** (Experimental) This threshold affects which potential multi-molecule are rejected.  Lower the value to make the selection more strict.  The value must fall between 0 and 1.
  * **Disable Ellipticity Rejector:**  This disables the ellipticity rejection test (unchecked by default).
  * **Disable Third Moment Rejector:**  (Experimental) This disables the third moment rejection test (checked by default).

## Maximum Likelihood Estimator Options ##
  * **Fixed Width:** Fixes the width of a molecule (depends only on the wavelength of light).
  * **Light Wavelength:** The wavelength of the light captured in the image (in nanometers).
  * **Numerical Aperture:** The Numerical Aperture of the camera used to take the images.
  * **Usable Pixel:** The fraction of the side of a CCD/CMOS pixel that is actually detecting photons.
  * **Position Threshold:** The threshold, below which, the change in the position parameter should be before finishing the estimation.
  * **Intensity Threshold:** The threshold, below which, the change in the intensity parameter should be before finishing the estimation.
  * **Width Threshold:** The threshold, below which, the change in the width parameter should be before finishing the estimation.
  * **Maximum Iterations:** The maximum number of iterations that can be made by the algorithm before forcing the estimation to finishing.

## Parameter Bounds ##
  * **Max Noise Multiplier:** The highest the noise estimate can go from the initial estimate before it is prevented to increasing.
  * **Min Noise Bound:** The minimum noise estimate that can be made by the estimator.
  * **Max Width:** The maximum width of a molecule allowed during estimation.
  * **Min Width:** The minimum width of a molecule allowed during estimation.

## Sample Rendering ##
  * **Enable Rendering:** Sets whether or not to produce a sample rendering from the located molecules.
  * **Render Scale:** This sets how large the sample rendering should be relative to the original image.  Use natural numbers (e.g. 1, 2, 3, 4, etc.).

# Localization Results #

The Localization Results table displays information on the molecules found during the search.  The first column, "**Frame**", is used to indicate which image in the stack was the molecule on that row located in.

## Position ##
There are 4 columns for the position of the molecules:
  * **x (px)**: is the horizontal position of the molecule in the image in pixel units.
  * **y (px)**: is the vertical position of the molecule in the image in pixel units.
  * **x (nm)**: is the horizontal position of the molecule in the image in nanometers.
  * **y (nm)**: is the vertical position of the molecule in the image in nanometers.

## Intensity ##
There are two intensity parameters, "**Intensity x**" and "**Intensity y**", which are estimated indepently of each other but should represent the same values (minor differences may occur).  They are estimated along with the x position parameter and the y position parameter, respectively.  What these coefficients represent depends on the on the source image and the fitting model used.  However, the intensity values are proportional to the intensity of the molecule, so if the intensity of one molecule is known, then the intensities of all others can be found using these values.

## Background ##
There are two background parameters, "**Background x**" and "**Intensity y**", which are estimated indepently of each other but should represent the same values (minor differences may occur).  They are estimated along with the x position parameter and the y position parameter, respectively.  The background parameters represent the number of photons per pixel are in the background noise.

## Width ##
There are two width parameters, "**Width x**" and "**Width y**", which are estimated indepently of each other but should represent the same values (large differences would have been detected by the ellipticity test, and rejected).  They are estimated along with the x position parameter and the y position parameter, respectively.  The width parameters represent the Guassian width of a molecule in pixel units.

# Sample Rendering #

If enabled, a sample rendering of the recenstruction of the image will be displayed after analysis.  This reconstruction is essentially a histogram of molecules per their position.  **Note:** If the image produced appears blank, or is black, the light level may be too low.  This can be fixed by going to **Image->Adjust->Window/Level...** and pressing the "Auto" button.