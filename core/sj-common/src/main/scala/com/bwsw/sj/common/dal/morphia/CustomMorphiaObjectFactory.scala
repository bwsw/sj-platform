package com.bwsw.sj.common.dal.morphia

import java.lang.reflect.Constructor

import com.bwsw.common.JsonSerializer
import org.mongodb.morphia.mapping.{DefaultCreator, MappingException}
import sun.reflect.ReflectionFactory

class CustomMorphiaObjectFactory extends DefaultCreator {

  override def createInstance[T](clazz: Class[T]): T = {
    try {
      val constructor = getNoArgsConstructor(clazz)
      if (constructor.isDefined) {
        return clazz.cast(constructor.get.newInstance())
      }
      try {
        val instance = ReflectionFactory.getReflectionFactory
          .newConstructorForSerialization(clazz, classOf[AnyRef].getDeclaredConstructor())
          .newInstance()

        clazz.cast(instance)
      } catch {
        case e: Exception => throw new MappingException("Failed to instantiate " + clazz.getName, e);
      }
    } catch {
      case e: Exception => throw new RuntimeException(e);
    }
  }

  private def getNoArgsConstructor(constructorType: Class[_]): Option[Constructor[_]] =
    try {
      val constructor = constructorType.getDeclaredConstructor()
      constructor.setAccessible(true)

      Some(constructor)
    } catch {
      case e: NoSuchMethodException => None
    }

  /**
    * It's necessary because of when a MesosSchedulerDriver (in mesos framework) is being created something is going wrong
    * (probably it should be but it's not our case) and after it the all instances have a null value of class loader.
    * May be it is a temporary measure (if we will find a different solution)
    */
  override def getClassLoaderForClass: ClassLoader = {
    classOf[JsonSerializer].getClassLoader
  }
}
