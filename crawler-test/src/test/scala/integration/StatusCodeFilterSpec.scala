package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import org.blikk.crawler.processors._

class StatusCodeFilterSpec extends IntegrationSuite("StatusCodeFilterSpec") {

  describe("crawler") {
    
    it("should be able to filter results by status code") {
      implicit val (in, system) = createSource()
      implicit val mat = FlowMaterializer()

      val seeds = List(
        WrappedHttpRequest.getUrl("http://localhost:9090/1"),
        WrappedHttpRequest.getUrl("http://localhost:9090/status/301"),
        WrappedHttpRequest.getUrl("http://localhost:9090/status/404"),
        WrappedHttpRequest.getUrl("http://localhost:9090/status/503")
      )
      val frontier = FrontierSink.build(appId)
      val fLinkSender = Sink.foreach[CrawlItem] { item => 
        log.info("{}", item.toString) 
        probes(1).ref ! item.req.uri.toString
      }
      val statusCodeFilter = StatusCodeFilter()

      in.via(statusCodeFilter).to(fLinkSender).run()
      Source(seeds).to(frontier).run()

      probes(1).receiveN(2).toSet should === (Set("http://localhost:9090/1", 
        "http://localhost:9090/status/301"))
      probes(1).expectNoMsg()
    }

  }

}