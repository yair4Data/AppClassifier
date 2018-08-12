package com.startapp.spring

import com.startapp.service.AppClassifierService
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
/**
  * Created by yairf on 13/02/2018.
  */
/*
@Configuration
class AppConfig {
  @Bean
  def appClassifierService(): AppClassifierService = new AppClassifierService



  @EventListener(Array(classOf[ApplicationReadyEvent]))
  def doSomethingAfterStartup(appClassifierService: AppClassifierService): Unit = {
    appClassifierService.ping()
    //log.info("hello world, I have just started up")
  }
}
*/
