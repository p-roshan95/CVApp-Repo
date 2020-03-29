package org.cv.cons

import java.sql.Timestamp

case class CameraData(CameraId: String, Rows: Int, Columns: Int, DataType: Int, Data: String, TimeStamp: Timestamp) extends Ordered [CameraData] {
  override def compare(that: CameraData): Int = {
    this compareTo that
  }
}