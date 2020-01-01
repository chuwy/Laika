/*
 * Copyright 2012-2020 the original author or authors.
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

package laika.io.model

import laika.ast.{Navigatable, Path, SectionInfo, Span, TemplateRoot}
import laika.config.Config

/** A titled, positional element in the tree of rendered documents.
  */
sealed trait RenderContent extends Navigatable {
  def path: Path
  def title: Seq[Span]
}

/** Represents a node of the tree of rendered documents.
  *
  * @param path the full, absolute path of this (virtual) document tree
  * @param title the title of this tree, either obtained from the title document or configuration
  * @param content the rendered documents and subtrees in a recursive structure
  * @param titleDocument the optional title document of this tree   
  */
case class RenderedTree (path: Path,
                         title: Seq[Span],
                         content: Seq[RenderContent],
                         titleDocument: Option[RenderedDocument] = None) extends RenderContent

/** A single rendered document with the content as a plain string in the target format.
  *
  * The title and section info are still represented as an AST, so they be used in any subsequent
  * step that needs to produce navigation structures.
  */
case class RenderedDocument (path: Path, title: Seq[Span], sections: Seq[SectionInfo], content: String) extends RenderContent

/** Represents the root of a tree of rendered documents. In addition to the recursive structure of documents
  * it holds additional items like static or cover documents, which may contribute to the output of a site or an e-book.
  *
  * @param tree the recursive structure of documents, usually obtained from parsing text markup 
  * @param defaultTemplate the default template configured for the output format, which may be used by a post-processor
  * @param config the root configuration of the rendered tree                       
  * @param coverDocument the cover document (usually used with e-book formats like EPUB and PDF)            
  * @param staticDocuments the paths of documents that were neither identified as text markup, config or templates, 
  *                        and will potentially be embedded or copied as is to the final output, depending on the output format
  * @param sourcePaths the paths this document tree has been built from or an empty list if this ast does not originate from the file system
  */
case class RenderedTreeRoot[F[_]] (tree: RenderedTree,
                                   defaultTemplate: TemplateRoot,
                                   config: Config,
                                   coverDocument: Option[RenderedDocument] = None,
                                   staticDocuments: Seq[BinaryInput[F]] = Nil,
                                   sourcePaths: Seq[String] = Nil) {

  /** The title of the tree, either obtained from the title document or configuration 
    */
  val title: Seq[Span] = tree.title

  /** The optional title document of the tree.
    */
  val titleDocument: Option[RenderedDocument] = tree.titleDocument

  /** All documents contained in this tree, fetched recursively, depth-first.
    */
  lazy val allDocuments: Seq[RenderedDocument] = {

    def collect (tree: RenderedTree): Seq[RenderedDocument] = tree.titleDocument.toSeq ++ tree.content.flatMap {
      case doc: RenderedDocument => Seq(doc)
      case sub: RenderedTree     => collect(sub)
    }

    coverDocument.toSeq ++ collect(tree)
  }
}