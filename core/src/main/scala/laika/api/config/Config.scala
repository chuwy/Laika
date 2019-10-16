/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package laika.api.config

import laika.ast.Path
import laika.parse.hocon.HoconParsers.{Field, ObjectValue, Origin, TracedValue}

/**
  * @author Jens Halm
  */
class Config (private[laika] val root: ObjectValue, 
              private[laika] val origin: Origin, 
              private[laika] val fallback: Option[Config] = None) {

//  println()
//  println("=====================================================================")
//  println(s"  CREATING CONFIG WITH KEYS: '${root.values.map(_.key).mkString(", ")}' FROM '$origin")
//  fallback.foreach(fb => println(s"          AND FALLBACK KEYS: '${fb.root.values.map(_.key).mkString(", ")}' FROM '${fb.origin}'"))
//  println("=====================================================================")
//  println()
  
  private def lookup(keySegments: List[String], target: ObjectValue): Option[Field] = {
    (target.values.find(_.key == keySegments.head), keySegments.tail) match {
      case (res, Nil) => res
      case (Some(Field(_, ov: ObjectValue)), rest) => lookup(rest, ov)
      case _ => None
    }
  }

  private def lookup(path: Path): Option[Field] = 
    if (path == Path.Root) Some(Field("", root)) else lookup(path.components, root).orElse {
      if (path.components.head == "config") lookup(Path(path.components.tail)) // legacy path prefix pre-0.12
      else None
    }
  
  def get[T](key: Path)(implicit decoder: ConfigDecoder[T]): Either[ConfigError, T] = {
    lookup(key).fold(
      fallback.fold[Either[ConfigError, T]](Left(NotFound(key)))(_.get[T](key))
    ) { field =>
      decoder(TracedValue(field.value, Set()))
    }
    // TODO - use actual origin, join objects, overload all methods with Path variant
  }
  
  def get[T](key: String)(implicit decoder: ConfigDecoder[T]): Either[ConfigError, T] = {
    val segments = key.split("\\.").toList
    get[T](Path(segments))
  }
  
  def get[T](key: String, default: => T)(implicit decoder: ConfigDecoder[T]): Either[ConfigError, T] = 
    getOpt(key).map(_.getOrElse(default))
  
  def getOpt[T](key: String)(implicit decoder: ConfigDecoder[T]): Either[ConfigError, Option[T]] = get(key).fold(
    e => if (e.isInstanceOf[NotFound]) Right(None) else Left(e),
    r => Right(Some(r))
  )
  
  def get[T](implicit decoder: ConfigDecoder[T], defaultKey: DefaultKey[T]): Either[ConfigError, T] = get[T](defaultKey.value)
  
  def withValue[T](key: String, value: T)(implicit encoder: ConfigEncoder[T]) : ConfigBuilder = 
    ConfigBuilder.empty.withValue(key, value).withFallback(this)

  def withValue[T](value: T)(implicit encoder: ConfigEncoder[T], defaultKey: DefaultKey[T]): ConfigBuilder =
    ConfigBuilder.empty.withValue(value).withFallback(this)
  
  def withFallback(other: Config): Config =
    if (other.root.values.isEmpty && other.fallback.isEmpty) this
    else if (root.values.isEmpty) fallback.fold(other)(_.withFallback(other))
    else new Config(root, origin, Some(fallback.fold(other)(_.withFallback(other)))) // TODO - should only exist on ConfigBuilder?
  
  def resolve: Config = this // TODO - should only exist on ConfigBuilder
  
  override def hashCode: Int = (root, origin, fallback).hashCode

  override def equals (obj: Any): Boolean = obj match {
    case c: Config => (c.root, c.origin, c.fallback).equals((root, origin, fallback))
    case _ => false
  }
  
}

object Config {
  
  type Result[T] = Either[ConfigError, T]
  
  val empty: Config = new Config(ObjectValue(Nil), Origin.root)
  
}