/*
package com.startapp.spring

/**
  * Created by yairf on 13/02/2018.
  */

import org.springframework.context.{ApplicationListener, ApplicationEvent}

/**
  * Adapter to allow handling [[org.springframework.context.ApplicationEvent]]s with
  * pattern matching.
  *
  * @author Tomasz Nurkiewicz
  * @author Arjen Poutsma
  */
trait ApplicationListenerAdapter extends ApplicationListener[ApplicationEvent] {

  final def onApplicationEvent(event: ApplicationEvent) {
    if (onEvent isDefinedAt event) {
      onEvent(event)
    }
  }

  /**
    * Handle an application event with a function.
    *
    * @return a function that takes an [[org.springframework.context.ApplicationEvent]] as
    *         parameter
    */
  def onEvent: PartialFunction[ApplicationEvent, Unit]

}*/
