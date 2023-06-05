
import numpy as np

def calculate_convolution(image,kernel):
    kernel = np.rot90(kernel,2)
    kh = int(kernel.shape[0])
    kw = int(kernel.shape[1])
    #print("卷积前",image.shape)
    #print(kh)
    #print(kw)
    if len(image.shape) == 3:
        if ((kw % 2 != 0) & (kh % 2 != 0)):
            image_rows = image.shape[0] - kh + 1
            image_cols = image.shape[1] - kw + 1
            image_c = image.shape[2]
            new_conv = np.zeros((image_rows,image_cols,image_c))
            for c in range(image_c):
                for i in range(int(image_rows)):
                    for j in range(int(image_cols)):
                        new_conv[i][j][c] = np.sum(kernel * image[i: i + kh, j: j+kw, c])
    else:
        if ((kw % 2 != 0) & (kh % 2 != 0)):
            image_rows = image.shape[0] - kh + 1
            image_cols = image.shape[1] - kw + 1
            new_conv = np.zeros((image_rows,image_cols))
            for i in range(int(image_rows)):
                    for j in range(int(image_cols)):
                        new_conv[i][j] = np.sum(kernel * image[i: i + kh, j: j+kw])
    #print("卷机后",new_conv.shape)
    return new_conv

def get_padding(padding_2):
    if padding_2 == 0:
        padding_before = 0
        padding_after = 0
        return int(padding_before),int(padding_after)
    elif padding_2 < 2:
        padding_before = 0
        padding_after = 1
        return int(padding_before),int(padding_after)
    elif padding_2 % 2 == 0:
        padding_before = padding_2/2
        padding_after = padding_2/2
        return int(padding_before),int(padding_after)
    else:
        padding_2 -= 1
        padding_before = padding_2/2+1
        padding_after = padding_2/2
        return int(padding_before),int(padding_after)

def convolve(image: np.ndarray, kernel: np.ndarray) -> np.ndarray:
    """
    Convolve an image with a kernel assuming zero-padding of the image to handle the borders
    :param image: the image (either greyscale shape=(rows,cols) or colour shape=(rows,cols,channels))
    :type numpy.ndarray
    :param kernel: the kernel (shape=(kheight,kwidth); both dimensions odd)
    :type numpy.ndarray
    :returns the convolved image (of the same shape as the input image)
    :rtype numpy.ndarray
    """
    # Your code here. You'll need to vectorise your implementation to ensure it runs
    # at a reasonable speed.

    stride = 1
    if len(image.shape) == 3:
        rows = image.shape[0]
        cols = image.shape[1]
        channels = image.shape[2]
    else:
        rows = image.shape[0]
        cols = image.shape[1]

    kh = kernel.shape[0]
    kw = kernel.shape[1]

    rows_2padding = (rows - 1) * stride + kh - rows
    cols_2padding = (cols - 1) * stride + kw - cols

    rows_before, rows_after = get_padding(rows_2padding)
    #print("rows_before", rows_before)
    #print("rows_after", rows_after)
    cols_before, cols_after = get_padding(cols_2padding)
    #print("cols_before", cols_before)
    #print("cols_after", cols_after)
    if len(image.shape) == 3:
        image_behind = np.pad(image, ((rows_before, rows_after), (cols_before, cols_after), (0, 0)), 'constant',
                              constant_values=0)
        convolved_image = calculate_convolution(image_behind, kernel)

    else:
        image_behind = np.pad(image, ((rows_before, rows_after), (cols_before, cols_after)), 'constant',
                              constant_values=0)
        convolved_image = calculate_convolution(image_behind, kernel)

    return convolved_image
