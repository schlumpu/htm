package nl.malienkolders.htm.viewer.jmonkey.lib.util

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import scala.collection.mutable.Map

class TextImage(text: String, align: Align, htmFont: HtmFont, color: Color, size: (Int, Int)) extends PaintableImage(size, true) {

  def this(text: String, align: Align, color: Color, size: (Int, Int)) =
    this(text, align, Dialog, color, size)

  def this(text: String, color: Color, size: (Int, Int)) =
    this(text, AlignCenter, Dialog, color, size)

  val background = new Color(0, 0, 0, 0)

  def paint(g: Graphics2D) {
    val fontRaw = htmFont.load()
    val textToDraw = if (fontRaw.getFontName().toLowerCase().contains("copperplate")) TextImage.copperplateSafe(text) else text
    val fontMetrics = TextImage.calculateFontSize(fontRaw, size._2, g)
    val font = htmFont.load().deriveFont(Font.PLAIN, fontMetrics._1)
    g.setBackground(background)
    g.clearRect(0, 0, getWidth(), getHeight())
    g.setFont(font)
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setColor(color)
    var textX = align match {
      case AlignLeft => 0
      case AlignCenter => (getWidth() - g.getFontMetrics(g.getFont()).stringWidth(textToDraw)) / 2
      case AlignRight => getWidth() - g.getFontMetrics(g.getFont()).stringWidth(textToDraw)
    }
    g.drawString(textToDraw, textX, getHeight() - fontMetrics._3)
  }
}

object TextImage {
  val heightToFontSize = Map[String, (Int, Int, Int)]()

  def calculateFontSize(fontRaw: Font, height: Int, g: Graphics2D) = {
    val key = fontRaw.getFontName() + "__" + height
    if (heightToFontSize.contains(key)) {
      heightToFontSize(key)
    } else {
      def getFontSize(fontHeight: Int): (Int, Int, Int) = {
        val font = fontRaw.deriveFont(Font.PLAIN, fontHeight)
        val metrics = g.getFontMetrics(font)
        if (metrics.getAscent() + metrics.getDescent() <= height)
          (fontHeight, metrics.getAscent(), metrics.getDescent())
        else
          getFontSize(fontHeight - 1)
      }
      val fontSize = getFontSize(height)
      heightToFontSize.put(key, fontSize)
      fontSize
    }
  }
  private val copperplateReplacements = Map(
    'ą' -> 'a',
    'Ł' -> 'L',
    'ł' -> 'l',
    'ń' -> 'n',
    'č' -> 'c')
  def copperplateSafe(s: String) = {
    var res = s
    for ((i, o) <- copperplateReplacements)
      res = res.replace(i, o)
    res
  }
}