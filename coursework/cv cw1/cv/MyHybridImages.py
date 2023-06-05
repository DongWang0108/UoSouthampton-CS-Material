import math
import numpy as np
from MyConvolution import convolve


def makeGaussianKernel(sigma: float) -> np.ndarray:
    kernel_size = int(8 * sigma + 1)
    if kernel_size % 2 == 0:
        kernel_size += 1
    # center os half of window size
    center = (kernel_size - 1) / 2

    # norma;ise by the total sum
    kernel_sum = 0

    kernel = np.zeros((kernel_size, kernel_size))
    coef = 1 / (2 * np.pi * sigma * sigma)
    for i in range(kernel_size):
        for j in range(kernel_size):
            x = pow(((i - center)), 2)
            y = pow(((j - center)), 2)
            fenzi = -(x + y)
            fenmu = 2 * sigma * sigma
            kernel[i][j] = np.exp(fenzi / fenmu) / coef
            kernel_sum += kernel[i][j]

    kernel = kernel / kernel_sum
    return kernel


def myHybridImages(lowImage: np.ndarray, lowSigma: float, highImage: np.ndarray, highSigma: float) -> np.ndarray:
    high_kernel = makeGaussianKernel(highSigma)
    low_kernel = makeGaussianKernel(lowSigma)

    # i_1*G_1
    high_pass_image = convolve(highImage, high_kernel)
    low_pass_image = convolve(lowImage, low_kernel)

    weightlow = 1
    weighthigh = 1

    high_pass_image = highImage - high_pass_image
    #high_pass_image = np.maximum(high_pass_image, 0)
    print(high_pass_image.shape)

    #hybrid_image = np.maximum((low_pass_image * weightlow + high_pass_image * weighthigh)/2, 0)
    hybrid_image = low_pass_image * weightlow + high_pass_image * weighthigh

    return hybrid_image



