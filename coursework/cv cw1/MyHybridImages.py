#!/usr/bin/python3 python
# encoding: utf-8
import math
import numpy as np
from cv.MyHybridImages import convolve
def makeGaussianKernel(sigma: float) -> np.ndarray:
    """
    Use this function to create a 2D gaussian kernel with standard deviation sigma.
    The kernel values should sum to 1.0, and the size should be floor(8*sigma+1) or
    floor(8*sigma+1)+1 (whichever is odd) as per the assignment specification.
    """
    # compute kernel size
    k_size = math.floor(8 * sigma + 1)

    # if even value
    k_size = k_size + 1 if k_size % 2 == 0 else k_size

    # Create the guassian kernel
    gaussian_kernel = np.zeros((k_size, k_size))
    for i in range(k_size):
        for j in range(k_size):
            x = i - k_size // 2
            y = j - k_size // 2
            value = math.exp(-(x ** 2 + y ** 2) / (2 * sigma ** 2)) / (2 * math.pi * sigma ** 2)
            gaussian_kernel[i][j] = value

    # normalize
    Z = gaussian_kernel.sum()
    gaussian_kernel = (1 / Z) * gaussian_kernel

    return gaussian_kernel


def myHybridImages(lowImage: np.ndarray, lowSigma: float, highImage: np.ndarray, highSigma: float) -> np.ndarray:
    """
    Create hybrid images by combining a low-pass and high-pass filtered pair.
    :param lowImage: the image to low-pass filter (either greyscale shape=(rows,cols) or colour
    shape=(rows,cols,channels))
    :type numpy.ndarray
    :param lowSigma: the standard deviation of the Gaussian used for low-pass filtering lowImage
    :type float
    :param highImage: the image to high-pass filter (either greyscale shape=(rows,cols) or colour
    shape=(rows,cols,channels))
    :type numpy.ndarray
    :param highSigma: the standard deviation of the Gaussian used for low-pass filtering highImage
    before subtraction to create the high-pass filtered image
    :type float
    :returns returns the hybrid image created
    by low-pass filtering lowImage with a Gaussian of s.d. lowSigma and combining it with
    a high-pass image created by subtracting highImage from highImage convolved with
    a Gaussian of s.d. highSigma. The resultant image has the same size as the input images.
    :rtype numpy.ndarray
    """

    # Create 2 gaussian kernels
    low_pass_kernel = makeGaussianKernel(lowSigma)
    high_pass_kernel = makeGaussianKernel(highSigma)

    # Convolve
    img_low = convolve(lowImage, low_pass_kernel)
    img_high = convolve(highImage, high_pass_kernel)

    img_high = highImage - img_high

    # Create Hybrid Image
    hybrid_img = img_low + img_high

    return hybrid_img



if __name__ == '__main__':
    from PIL import Image
    image = Image.open('')
    image = np.array(image)
    image2 = Image.open('C:\Users\DELL\Desktop\cv\data\cat.bmp')
    image2 = np.array(image2)
    final = myHybridImages(image, 2, image2, 3)
    print(final.shape)
    Image.fromarray(final.astype(np.uint8))

