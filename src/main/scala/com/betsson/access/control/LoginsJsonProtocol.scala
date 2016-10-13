package com.betsson.access.control

import spray.json.DefaultJsonProtocol

/**
  * Created by colm on 13/10/16.
  */
object LoginsJsonProtocol extends DefaultJsonProtocol {
  implicit val loginAttemptFormat = jsonFormat4(LoginAttemptMessage)
  implicit val loginFailuresFormat = jsonFormat3(LoginFailuresMessage)
}

