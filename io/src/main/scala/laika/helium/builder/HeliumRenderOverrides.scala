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

package laika.helium.builder

import laika.ast.RelativePath.CurrentDocument
import laika.ast._
import laika.helium.config.{AnchorPlacement, HeliumIcon}
import laika.render.{FOFormatter, HTMLFormatter}

/**
  * @author Jens Halm
  */
private[helium] object HeliumRenderOverrides {

  case class Tabs (tabs: Seq[Tab], options: Options = NoOpt) extends Block {
    type Self = Tabs
    def withOptions(options: Options): Tabs = copy(options = options)
  }
  case class Tab (name: String, label: String, options: Options = NoOpt) extends Span {
    type Self = Tab
    def withOptions(options: Options): Tab = copy(options = options)
  }
  case class TabContent (name: String, content: Seq[Block], options: Options = NoOpt) extends Block {
    type Self = TabContent
    def withOptions(options: Options): TabContent = copy(options = options)
  }

  def renderChoices (fmt:HTMLFormatter, name: String, choices: Seq[Choice], options: Options): String = {
    choices match {
      case Nil => ""
      case first +: rest =>
        val tabs = Tabs(Tab(first.name, first.label, Style.active) +: rest.map(c => Tab(c.name, c.label)))
        val content = TabContent(first.name, first.content, Style.active) +: rest.map(c => TabContent(c.name, c.content))
        fmt.indentedElement("div", options + Styles("tab-container"), tabs +: content, "data-tab-group" -> name)
    }
  }
  
  def icon (opt: Options): Option[Icon] = (opt.styles - "callout").headOption match {
    case Some("warning") => Some(HeliumIcon.warning)
    case Some("error") => Some(HeliumIcon.error)
    case Some("info") => Some(HeliumIcon.info)
    case _ => None
  }

  def forHTML (anchorPlacement: AnchorPlacement): PartialFunction[(HTMLFormatter, Element), String] = {
    case (fmt, Header(level, content, opt)) =>
      def link (style: String) = opt.id.map(id => SpanLink(Seq(HeliumIcon.link), InternalTarget(CurrentDocument(id)), options = Styles("anchor-link", style)))
      val linkedContent = anchorPlacement match {
        case AnchorPlacement.None => content
        case AnchorPlacement.Left => link("left").toSeq ++ content
        case AnchorPlacement.Right => content ++ link("right").toSeq
      }
      fmt.newLine + fmt.element("h"+level.toString, opt, linkedContent)
    case (fmt, BlockSequence(content, opt)) if opt.styles.contains("callout") =>
      fmt.indentedElement("div", opt, icon(opt).toSeq ++ content)
    case (fmt, Selection(name, choices, opt)) => renderChoices(fmt, name, choices, opt)

    case (fmt, i: Icon) if i.hasStyle("api-link")    => fmt.newLine + SVGIcons.apiIcon + fmt.newLine
    case (fmt, i: Icon) if i.hasStyle("source-link") => fmt.newLine + SVGIcons.githubIcon + fmt.newLine
      
    case (fmt, tabs: Tabs)      => fmt.indentedElement("ul", Styles("tab-group"), tabs.tabs)
    case (fmt, tab: TabContent) => fmt.indentedElement("div", Styles("tab-content") + tab.options, tab.content, "data-choice-name" -> tab.name)
    case (fmt, tab: Tab)        => 
      val link = SpanLink(Seq(Text(tab.label)), InternalTarget(CurrentDocument()))
      fmt.element("li", Styles("tab") + tab.options, Seq(link), "data-choice-name" -> tab.name)
  }
  
  def forPDF: PartialFunction[(FOFormatter, Element), String] = {
    case (fmt, b @ BlockSequence(content, opt)) if opt.styles.contains("callout") =>
      fmt.blockContainer(b, SpanSequence(icon(opt).toSeq, Styles("icon")) +: content)
  }

  def forEPUB: PartialFunction[(HTMLFormatter, Element), String] = {
    case (fmt, BlockSequence(content, opt)) if opt.styles.contains("callout") =>
      fmt.indentedElement("div", opt, icon(opt).toSeq ++ content)
  }
  
}
