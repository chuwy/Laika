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

package laika.io.runtime

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits._
import laika.io.binary
import laika.io.model.RenderedTreeRoot
import laika.io.text._

/** Internal runtime for transform operations, for text and binary output as well
  * as parallel and sequential execution. 
  *
  *  @author Jens Halm
  */
object TransformerRuntime {

  /** Process the specified transform operation for a single input document and 
    * a character output format.
    */
  def run[F[_]: Async: Runtime] (op: SequentialTransformer.Op[F]): F[String] = for {
    doc <- SequentialParser.Op(op.transformer.parser, op.input).parse
    res <- SequentialRenderer.Op(op.transformer.renderer, doc.content, doc.path, op.output).render
  } yield res

  /** Process the specified transform operation for an entire input tree and 
    * a character output format.
    */
  def run[F[_]: Async: Runtime] (op: ParallelTransformer.Op[F]): F[RenderedTreeRoot[F]] = for {
    tree <- ParallelParser.Op(NonEmptyList.of(op.transformer.parser), op.input).parse
    res  <- ParallelRenderer.Op(op.transformer.renderer, tree.root, op.output, tree.staticDocuments).render
  } yield res

  /** Process the specified transform operation for a single input document and 
    * a binary output format.
    */
  def run[F[_]: Async: Runtime] (op: binary.SequentialTransformer.Op[F]): F[Unit] = for {
    doc <- SequentialParser.Op(op.transformer.markupParser, op.input).parse
    res <- binary.SequentialRenderer.Op(op.transformer.renderer, doc.content, doc.path, op.output).render
  } yield res

  /** Process the specified transform operation for an entire input tree and 
    * a binary output format.
    */
  def run[F[_]: Async: Runtime] (op: binary.ParallelTransformer.Op[F]): F[Unit] = for {
    tree <- ParallelParser.Op(NonEmptyList.of(op.transformer.markupParser), op.input).parse
    res  <- binary.ParallelRenderer.Op[F](op.transformer.renderer, tree.root, op.output, tree.staticDocuments).render
  } yield res

}