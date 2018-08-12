/*
package com.startapp

import com.startapp.spring.Logging
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.{Before, Test}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Mock, MockitoAnnotations}
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import scala.util.Try

/**
  * Created by yairf on 16/02/2018.
  */
@RunWith(classOf[SpringRunner])
@SpringBootTest
class AppClassifierTest extends Logging {

  @Mock
  var sparkTestService: OfflineTopReportsService = _


  val cnx = ReportLoadContext(ReportKeyInfo(ReportTypes.TOP), Some(PartitionInfo(720, None)))

  @Before
  def init(): Unit = {
    MockitoAnnotations.initMocks(this)
    reportManager = new ReportManager(reportsDefDao, Some(new LocalFileSystemService))
  }

  @Test
  def testReportManagerNoReport(): Unit = {
    when(reportsDefDao.getReportByType(anyString)).thenReturn(None)
    val resNone = reportManager.process(cnx)
    assertFalse("Result should be empty", resNone.isDefined)
  }

  @Test
  def testReportManager(): Unit = {
    val report = Some(ReportDefs(1, "Test", orgID))
    when(reportsDefDao.getReportByType(anyString)).thenReturn(report)
    val resReport = reportManager.process(cnx)
    assertEquals("Result should be equal", resReport, report)
  }

  @Test
  def testReportManagerLoad(): Unit = {
    Try {
      val report = Some(ReportDefs(1, "Test", orgID))
      when(reportsDefDao.getReportByType(anyString)).thenReturn(report)
      val range = Vector.range(0, 1000)
      range.par.foreach { i =>
        logger.warn(s"running $i")
        reportManager.process(cnx)
      }
      Thread.sleep(60000000)
    }
  } recover {
    case e => assert(true, e)
  }
}



*/
