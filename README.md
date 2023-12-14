# PF_ImagingXAFS
ImageJ/Fiji plugin for Imaging XAFS data analysis at KEK-PF BL-15A1 and PF-AR NW2A.

**BL-15A1** is a semi-micro XAFS/XRF/XRD experiment beamline at the Photon Factory, KEK, Japan.  
<https://doi.org/10.1088/1742-6596/425/7/072016>

**NW2A** is a beamline for time-resolved and spatially-resolved XAFS experiments at the PF-AR, KEK, Japan.  
<https://doi.org/10.1063/1.5084621>

Using this plugin, one can  
1. Check an image (and apply reference if necessary).
1. Import energy stack data.
1. Pre-process the stack such as energy correction and drift correction if necessary.
1. Show multiple roi/point spectra, normalize them by pre- and post-edge lines, and fit them using standard spectra.
1. Normalize and fit (by singular value decomposition) whole energy stack.  
1. Process tiling/mosaic energy stacks with a batch job and stitch the results.

# Subpackages
- **imagingXAFS.common** contains the common functions for Imaging XAFS analysis shared within this plugin, as shown above.
- **imagingXAFS.bl15a1** contains XRF/XANES map data import, apply reference, work with correlation plot of two images, and export Tiff files for TXM XANES Wizard.
BL15A1Props.config is used to store data import parameters.
- **imagingXAFS.nw2a_orca** contains data import, pre-process, and batch-job operations for direct-projection Imaging XAFS measurement using Hamamatsu ORCA-Flash or ORCA-Quest at PF-AR NW2A and Ritsumeikan SR Center BL-4.
OrcaProps.config is used to store data import and energy correction parameters.
- **imagingXAFS.nw2a_ultra** is for zoneplate-projection 2D XANES / Mosaic 2D XANES measurement using Zeiss Ultra XRM at PF-AR NW2A.

# Installation
Place 'PF_ImaginagXAFS.jar', 'commons-math3-3.6.1.jar', 'poi-3.17.jar', 'BL15A1Props.config', and 'OrcaProps.config' in /plugins/PF_ImagingXAFS/.  
Place 'Jet.lut' in /luts/.  
If your ImageJ/Fiji already has installed [mrsutherland/XRM_Reader](https://github.com/mrsutherland/XRM_Reader "mrsutherland/XRM_Reader: ImageJ plugin to read xrm files.") 
plugin, remove it to avoid conflicts.  
On vanilla ImageJ, 'Stitching_-3.1.6.jar' and many other dependencies are necessary to run stiching and drift correction functions.

# Acknowledgements
Stitching function in imagingXAFS.common and drift correction function in imagingXAFS.nw2a_ultra use 
[fiji/Stiching](https://github.com/fiji/Stitching "fiji/Stitching: Fiji's Stitching plugins reconstruct big images from tiled input images.") 
plugin.

Modified version of [mrsutherland/XRM_Reader](https://github.com/mrsutherland/XRM_Reader "mrsutherland/XRM_Reader: ImageJ plugin to read xrm files.")
plugin is used in importing Zeiss Ultra XRM images.

The energy correction method in imagingXAFS.nw2a_orca is based on 
[M. Katayama et al.](https://doi.org/10.1107/S0909049512028282 "M. Katayama et al., J. Synchrotron Rad. 19, 717 (2012).")

The manner of Imaging XAFS data analysis in this plugin is highly inspired by 
[TXM Wizard](https://sourceforge.net/projects/txm-wizard/ "TXM-Wizard download | SourceForge.net"). 
About TXM Wizard, refer to [Y. Liu et al.](https://doi.org/10.1107/S0909049511049144 "Y. Liu et al., J. Synchrotron Rad. 19, 281 (2012).")

Singular value decomposition in imagingXAFS.common is baed on [Koprinarov et al.](https://doi.org/10.1021/jp013281l "I. N. Koprinarov et al., J. Phys. Chem. B 106, 5358 (2002)."),
that is commonly used for soft X-ray scanning transmission X-ray microscopy (STXM) data analysis: 
[aXis2000](http://unicorn.chemistry.mcmaster.ca/axis/aXis2000.html "aXis2000 source").
