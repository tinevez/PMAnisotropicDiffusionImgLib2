Perone & Malik anisotropic diffusion for Icy, based on ImgLib2.
===============================================================

An Icy plugin based on ImgLib2 that implements Perona and Malik anisotropic diffusion.

Algorithm.
----------

This algorithm implements the so-called anisotropic diffusion scheme of Perona
& Malik, 1990, with imglib. For details on the anisotropic diffusion
principles, see http://en.wikipedia.org/wiki/Anisotropic_diffusion, and the
original paper: 

````
Perona and Malik.  Scale-Space and Edge Detection Using Anisotropic Diffusion. 
IEEE Transactions on Pattern Analysis and Machine Intelligence (1990) vol. 12 pp. 629-639
````

Implementation.
---------------

This implementation uses ImgLib2 for its core. Filtering is done in place, and a
call to the process() method does only one iteration of the process on the
given image. This allow to change all parameters at each iteration if desired.
This implementation is dimension generic: the filtering is done considering a
3x3 neighborhood for a 2D image, a 3x3x3 neighborhood for a 3D image, and so
on.

For every pixel of the image, the contribution of all close neighbors in a cube
(whatever is the dimensionality) around the central pixel is considered. Image
gradient is evaluated by finite differences in direction of the neighbor
currently inspected. The value of this component of the gradient is used to
compute the diffusion coefficient, through a function that must implements the
DiffusionFunction interface. Users can specify their own function. Two
functions are offered, taken from Perona and Malik original paper:
StrongEdgeEnhancer and WideRegionEnhancer.

This implementation is multithreaded; the number of used thread can be
specified with the setNumThreads(int) or setNumThreads() methods.

