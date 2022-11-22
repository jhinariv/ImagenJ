package ij.process;

public class General {

  public static String CalculatePixels(
    int roiX, int roiY, int roiHeight, int roiWidth,
    int height, int width,
    double tmp1, double tmp2, double tmp3, double tmp4,
    double sa, double ca, double centerX, double centerY,
    int xMax, double dwidth, int interpolationMethod,
    double xlimit, double xlimit2, double ylimit, double ylimit2,
    String pixels, byte[] pixels2,
    int bgColor, double interpolaridad
    ) {

    // pixels = null;
    int index, ixs, iys;
    double xs, ys;

    for (int y=roiY; y<(roiY + roiHeight); y++) {
      index = y*width + roiX;
      tmp3 = tmp1 - y*sa + centerX;
      tmp4 = tmp2 + y*ca + centerY;
      for (int x=roiX; x<=xMax; x++) {
        xs = x*ca + tmp3;
        ys = x*sa + tmp4;
        if ((xs>=-0.01) && (xs<dwidth) && (ys>=-0.01) && (ys<dheight)) {
          if (interpolationMethod==BILINEAR) {
            if (xs<0.0) xs = 0.0;
            if (xs>=xlimit) xs = xlimit2;
            if (ys<0.0) ys = 0.0;
            if (ys>=ylimit) ys = ylimit2;
            pixels[index++] = (int) interpolaridad;
          } else {
            ixs = (int)(xs+0.5);
            iys = (int)(ys+0.5);
            if (ixs>=width) ixs = width - 1;
            if (iys>=height) iys = height -1;
            pixels[index++] = pixels2[width*iys+ixs];
          }
        } else
          pixels[index++] = (int) bgColor;
      }
    }
  }
}
