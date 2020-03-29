package org.cv.cons

import java.util

import org.apache.log4j.Logger
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.util.Base64

import scala.collection.JavaConversions._

object MotionDetector extends Serializable  {
  private val logger = Logger.getLogger(MotionDetector.getClass)
  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  @throws[Exception]
  def detectMotion(camID: String, frameIterator: Iterator[CameraData], ouputDirectory: String, prevProcessCamData: CameraData): CameraData = {
    var curentProcessCamData = CameraData.apply("", 0, 0, 0, "",null)
    var frame: Mat = null
    var copyFrame: Mat = null
    var grayFrame: Mat = null
    var firstFrame: Mat = null
    val deltaFrame = new Mat
    val thresholdFrame = new Mat
    var rectArray = new util.ArrayList[Rect]

    if (prevProcessCamData != null) {
      val preFrame = getMat(prevProcessCamData)
      val preGrayFrame = new Mat(preFrame.size, CvType.CV_8UC1)
      Imgproc.cvtColor(preFrame, preGrayFrame, Imgproc.COLOR_BGR2GRAY)
      Imgproc.GaussianBlur(preGrayFrame, preGrayFrame, new Size(3, 3), 0)
      firstFrame = preGrayFrame
    }




    val sortedList = new util.ArrayList[CameraData]
    while (frameIterator.hasNext) {
      sortedList.add(frameIterator.next)
    }
    sortedList.sorted
    logger.warn("cameraId=" + camID + " total frames=" + sortedList.size)
    for (camData <- sortedList) {
      frame = getMat(camData)
      copyFrame = frame.clone
      grayFrame = new Mat(frame.size, CvType.CV_8UC1)
      Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY)
      Imgproc.GaussianBlur(grayFrame, grayFrame, new Size(3, 3), 0)
      logger.warn("cameraId=" + camID + " timestamp=" + camData.TimeStamp)

      if (firstFrame != null) {
        Core.absdiff(firstFrame, grayFrame, deltaFrame)
        Imgproc.threshold(deltaFrame, thresholdFrame, 20, 255, Imgproc.THRESH_BINARY)
        rectArray = getContourArea(thresholdFrame)
        if (rectArray.size > 0) {
          val it2 = rectArray.iterator
          while (it2.hasNext) {
            val obj = it2.next
            Imgproc.rectangle(copyFrame, obj.br, obj.tl, new Scalar(0, 255, 0), 2)
          }
          logger.warn("Motion detected for cameraId=" + camData.CameraId + ", timestamp=" + camData.TimeStamp)
          saveImage(copyFrame, camData, ouputDirectory)
        }
      }
      firstFrame = grayFrame
      curentProcessCamData = camData
    }
    curentProcessCamData
  }


  @throws[Exception]
  private def getMat(camData: CameraData): Mat = {
    val mat = new Mat(camData.Rows, camData.Columns, camData.DataType)
    mat.put(0, 0, Base64.getDecoder.decode(camData.Data))
    mat
  }

  private def getContourArea(mat: Mat) = {
    val hierarchy = new Mat
    val image = mat.clone
    val contours = new util.ArrayList[MatOfPoint]
    Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    var rect: Rect = null
    val maxArea: Double = 300
    val arr = new util.ArrayList[Rect]
    for (i <- 0 until contours.size) {
      val contour = contours.get(i)
      val contourArea = Imgproc.contourArea(contour)
      if (contourArea > maxArea) {
        rect = Imgproc.boundingRect(contours.get(i))
        arr.add(rect)
      }
    }
    arr
  }

  private def saveImage(frame: Mat, camData: CameraData, outputDir: String): Unit = {
    val imagePath = outputDir + camData.CameraId + "-T-" + camData.TimeStamp.getTime + ".png"
    logger.warn("Saving images to " + imagePath)
    val result = Imgcodecs.imwrite(imagePath, frame)
    if (!result)
      logger.error("Couldn't save images to path " + outputDir + ".Please check if this path exists. This is configured in processed.output.dir key of property file.")
  }
}