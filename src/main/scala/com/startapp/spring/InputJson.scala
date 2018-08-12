package com.startapp.spring

/**
  * Created by yairf on 14/02/2018.
  */

import java.util.HashMap
import java.util.Map

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.mutable
import scala.collection.mutable.StringBuilder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(Array("appId", "package", "appName", "description"))
class InputJson {

  @JsonProperty("appId")
  @BeanProperty
  var appId: String = _

  @JsonProperty("package")
  @BeanProperty
  var `package`: String = _

  @JsonProperty("appName")
  @BeanProperty
  var appName: String = _

  @JsonProperty("description")
  @BeanProperty
  var description: String = _



  override def toString(): String = {
    val builder = StringBuilder.newBuilder
    builder.append("appId", appId).append("package", `package`).append("appName", appName).append("description", description).toString
  }
}