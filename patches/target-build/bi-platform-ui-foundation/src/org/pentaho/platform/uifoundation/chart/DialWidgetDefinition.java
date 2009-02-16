/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License, version 2 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2005 - 2008 Pentaho Corporation.  All rights reserved. 
 * 
 * @created Aug 15, 2005
 * @author James Dixon
 *
 */

package org.pentaho.platform.uifoundation.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.data.Range;
import org.jfree.ui.RectangleEdge;
import org.pentaho.commons.connection.DataUtilities;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.commons.connection.PentahoDataTransmuter;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.uifoundation.messages.Messages;
import org.pentaho.platform.util.messages.LocaleHelper;

/**
 * This class represents the definition of a dashboard dial. It holds:
 * <ul>
 * <li>The value to be displayed on the dial</li>
 * <li>Minimum value of the dial</li>
 * <li>Maximum value of the dial</li>
 * <li>A list of intervals with the dial. Each interval specifies a minimum,
 * maximum and information about how the interval should be painted.</li>
 * <li>Painting information
 * <ul>
 * <li>Background paint</li>
 * <li>Dial paint</li>
 * <li>Needle paint</li>
 * </ul>
 * </ul>
 *
 * <p/> This class does not generate an image of the dial, it just defines the
 * properties of the dial. <p/> Dial definitions are stored in xml documents in
 * the solution folders with *.dial.xml extensions. These definition files store
 * XML representations of all the settings here. Typically the value to be
 * displayed is provided at runtime by a query or business rule, but the value
 * can also read from the definition file. <p/> The definitions are read by
 * org.pentaho.core.ui.component.DashboardWidgetComponent objects, which create
 * instances of this object and set the properties defined here. <p/> The
 * DashboardWidgetComponent objects pass this, now populated, object to
 *
 * The dial image is generated by
 * {@link org.pentaho.core.ui.component.JFreeChartEngine} <p/>
 *
 * Example Dial <br/> <img src="doc-files/DialWidgetDefinition-1.png">
 */
public class DialWidgetDefinition extends WidgetDefinition implements ChartDefinition {

  private static final long serialVersionUID = 2232742163326878608L;

  private final ArrayList intervals = new ArrayList();

  private final RectangleEdge titlePosition = RectangleEdge.TOP;

  private Paint chartBackgroundPaint = Color.WHITE;

  private Paint plotBackgroundPaint = Color.GRAY;

  private Paint needlePaint = Color.blue;

  private DialShape dialShape = DialShape.CHORD;

  private Font titleFont;

  private final List subTitles = new ArrayList();

  private boolean rangeLimited;

  private int tickSize = 5;

  private Paint tickPaint = Color.blue;

  private Paint valuePaint = Color.BLUE;

  private Font valueFont;

  private String units;

  private Font legendFont = null;

  private boolean legendBorderVisible = true;

  private Node attributes = null;
  
  private Float backgroundAlpha;
  
  private Float foregroundAlpha;

  //    private IPentahoSession session;

  public DialWidgetDefinition(final double value, final double minimum, final double maximum, final boolean rangeLimited) {
    super(value, minimum, maximum);
    this.rangeLimited = rangeLimited;
  }

  /**
   * TODO PROBLEM HERE! If you use this constructor, the XML schema for the chart attributes is different than if you use the constructor with the arguments 
   * public DialWidgetDefinition( Document document, double value, int width, int height, IPentahoSession session). This constructor expects the 
   * chart attribute nodes to be children of the <chart-attributes> node, whereas the latter constructor expects the attributes to be children of a <dial> node. 
   * This does not help us with our parity situation, and should be deprecated and reconciled. 
   *  
   * @param data
   * @param byRow
   * @param chartAttributes
   * @param width
   * @param height
   * @param session
   */
  public DialWidgetDefinition(final IPentahoResultSet data, final boolean byRow, final Node chartAttributes,
      final int width, final int height, final IPentahoSession session) {
    this(0.0, Double.MIN_VALUE, Double.MAX_VALUE, false);

    attributes = chartAttributes;

    if (data != null) {
      if (byRow) {
        setDataByRow(data);
      } else {
        setDataByColumn(data);
      }
    }

    //set legend font
    setLegendFont(chartAttributes.selectSingleNode(ChartDefinition.LEGEND_FONT_NODE_NAME));

    // set legend border visible
    setLegendBorderVisible(chartAttributes.selectSingleNode(ChartDefinition.DISPLAY_LEGEND_BORDER_NODE_NAME));
    
    // set the alfa layers
    setBackgroundAlpha(chartAttributes.selectSingleNode(BACKGROUND_ALPHA_NODE_NAME));

    setForegroundAlpha(chartAttributes.selectSingleNode(FOREGROUND_ALPHA_NODE_NAME));


    DialWidgetDefinition.createDial(this, chartAttributes, width, height, session);
  }

  /**
   * TODO: PROBLEM HERE! See the note on the constructor above. 
   * 
   * @param document
   * @param value
   * @param width
   * @param height
   * @param session
   */
  public DialWidgetDefinition(final Document document, final double value, final int width, final int height,
      final IPentahoSession session) {
    this(value, Double.MIN_VALUE, Double.MAX_VALUE, false);

    // get the dial node from the document
    attributes = document.selectSingleNode("//dial"); //$NON-NLS-1$

    deriveMinMax(value);

    // create the dial definition object
    DialWidgetDefinition.createDial(this, attributes, width, height, session);
  }

  public static Log getLogger() {
    return LogFactory.getLog(DialWidgetDefinition.class);
  }

  /*
   * public ThermometerWidgetDefinition createThermometer( Document doc ) { //
   * TODO implement this to return a ThermometerWidgetDefinition object return
   * null; }
   */
  /**
   * Create a dial definition object from an XML document
   *
   * @param doc
   *            definition XML document
   * @return Dial definition object
   */
  public static void createDial(final DialWidgetDefinition widgetDefinition, final Node dialNode, final int width,
      final int height, final IPentahoSession session) {

    Node node = dialNode.selectSingleNode("units"); //$NON-NLS-1$
    if (node != null) {
      String units = node.getText();
      widgetDefinition.setUnits(units);
    }

    // set the background Paint
    Paint paint = JFreeChartEngine.getPaint(dialNode.selectSingleNode("background-color")); //$NON-NLS-1$
    if (paint == null) {
      Element backgroundNode = (Element) dialNode.selectSingleNode("chart-background"); //$NON-NLS-1$
      if (backgroundNode != null) {
        String backgroundType = backgroundNode.attributeValue("type"); //$NON-NLS-1$
        if ("texture".equals(backgroundType)) { //$NON-NLS-1$
          paint = JFreeChartEngine.getTexturePaint(backgroundNode, width, height, session);
        } else if ("gradient".equals(backgroundType)) { //$NON-NLS-1$
          paint = JFreeChartEngine.getGradientPaint(backgroundNode, width, height);
        }
      }
    } else {
      // log a deprecation warning for background-color ...
      DialWidgetDefinition.getLogger().warn(
          Messages.getString("CHART.WARN_DEPRECATED_PROPERTY", "background-color", "chart-background"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$    
      DialWidgetDefinition.getLogger().warn(
          Messages.getString("CHART.WARN_PROPERTY_WILL_NOT_VALIDATE", "background-color"));//$NON-NLS-1$ //$NON-NLS-2$     
    }

    if (paint != null) {
      widgetDefinition.setChartBackgroundPaint(paint);
    }

    // set the dial background Paint
    paint = JFreeChartEngine.getPaint(dialNode.selectSingleNode("plot-background-color")); //$NON-NLS-1$
    if (paint == null) {
      Element backgroundNode = (Element) dialNode.selectSingleNode("plot-background"); //$NON-NLS-1$
      if (backgroundNode != null) {
        String backgroundType = backgroundNode.attributeValue("type"); //$NON-NLS-1$
        if ("texture".equals(backgroundType)) { //$NON-NLS-1$
          paint = JFreeChartEngine.getTexturePaint(backgroundNode, width, height, session);
        } else if ("gradient".equals(backgroundType)) { //$NON-NLS-1$
          paint = JFreeChartEngine.getGradientPaint(backgroundNode, width, height);
        }
      }
    } else {
      // log a deprecation warning for plot-background-color ...
      DialWidgetDefinition.getLogger().warn(
          Messages.getString("CHART.WARN_DEPRECATED_PROPERTY", "plot-background-color", "plot-background"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$    
      DialWidgetDefinition.getLogger().warn(
          Messages.getString("CHART.WARN_PROPERTY_WILL_NOT_VALIDATE", "plot-background-color"));//$NON-NLS-1$ //$NON-NLS-2$     
    }

    if (paint != null) {
      widgetDefinition.setPlotBackgroundPaint(paint);
    }

    // set the needle Paint
    paint = JFreeChartEngine.getPaint(dialNode.selectSingleNode("needle-color")); //$NON-NLS-1$
    if (paint != null) {
      widgetDefinition.setNeedlePaint(paint);
    }

    // set the tick Paint
    paint = JFreeChartEngine.getPaint(dialNode.selectSingleNode("tick-color")); //$NON-NLS-1$
    if (paint != null) {
      widgetDefinition.setTickPaint(paint);
    }

    Node tmpNode = dialNode.selectSingleNode("tick-interval"); //$NON-NLS-1$
    if (tmpNode != null) {
      widgetDefinition.setTickSize(Integer.parseInt(dialNode.selectSingleNode("tick-interval").getText())); //$NON-NLS-1$
    }

    // set the value Paint
    paint = JFreeChartEngine.getPaint(dialNode.selectSingleNode("value-color")); //$NON-NLS-1$
    if (paint != null) {
      widgetDefinition.setValuePaint(paint);
    }

    // TODO get this from the XML document
    widgetDefinition.setDialShape(DialShape.CHORD);

    Node titleFontNode = dialNode.selectSingleNode("title-font"); //$NON-NLS-1$
    if (titleFontNode != null) {
      Node fontNode = titleFontNode.selectSingleNode("font");
       if(fontNode != null) {
       String titleFontStr = fontNode.getText().trim();
       if (!"".equals(titleFontStr)) { //$NON-NLS-1$
    	 Node titleFontSizeNode = titleFontNode.selectSingleNode("size");
    	 int size = titleFontSizeNode != null ? Integer.parseInt(titleFontSizeNode.getText()) : 12;
         widgetDefinition.setTitleFont(new Font(titleFontStr, Font.BOLD, size));
       }
      }
    }

    Node valueFontNode = dialNode.selectSingleNode("domain-tick-font"); //$NON-NLS-1$
    if (valueFontNode != null) {
      Node fontNode = titleFontNode.selectSingleNode("font");
      if(fontNode != null) {
       String fontStr = fontNode.getText().trim();
       if (!"".equals(fontStr)) { //$NON-NLS-1$
    	 Node valueFontSizeNode = valueFontNode.selectSingleNode("size");
    	 int size = valueFontSizeNode != null ? Integer.parseInt(valueFontSizeNode.getText()) : 12;
         widgetDefinition.setValueFont(new Font(fontStr, Font.BOLD, size));
       }
      }
    }

    // set any intervals that are defined in the document

    // A list of interval nodes should not be allowed to exist as a child of the main XML element (for XML schema to 
    // be well constructed and validate the XML . 
    // We have deprecated <interval> as a child of the main node , and now require an <intervals> parent node 
    // under which <intervals> can exist. 

    List intervals = dialNode.selectNodes("interval"); //$NON-NLS-1$

    if ((intervals == null) || (intervals.isEmpty())) {
      Node intervalsNode = dialNode.selectSingleNode("intervals"); //$NON-NLS-1$
      if (intervalsNode != null) {
        intervals = intervalsNode.selectNodes("interval"); //$NON-NLS-1$
      }
    } else {
      // log a deprecation warning for this property...
      DialWidgetDefinition.getLogger().warn(Messages.getString("CHART.WARN_DEPRECATED_CHILD", "interval", "intervals"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$    
      DialWidgetDefinition.getLogger().warn(Messages.getString("CHART.WARN_PROPERTY_WILL_NOT_VALIDATE", "interval"));//$NON-NLS-1$ //$NON-NLS-2$     
    }

    if (intervals != null) {

      Iterator intervalIterator = intervals.iterator();
      while (intervalIterator.hasNext()) {
        // get the interval node
        Node intervalNode = (Node) intervalIterator.next();

        // get the interval name
        String label = intervalNode.selectSingleNode("label").getText(); //$NON-NLS-1$

        // get the range of the interval
        double minimum = Double.parseDouble(intervalNode.selectSingleNode("minimum").getText()); //$NON-NLS-1$
        double maximum = Double.parseDouble(intervalNode.selectSingleNode("maximum").getText()); //$NON-NLS-1$
        Range range = new Range(minimum, maximum);

        Paint backgroundPaint = JFreeChartEngine.getPaint(intervalNode.selectSingleNode("color")); //$NON-NLS-1$
        if (backgroundPaint == null) {
          Element backgroundNode = (Element) intervalNode.selectSingleNode("interval-background"); //$NON-NLS-1$
          if (backgroundNode != null) {
            String backgroundType = backgroundNode.attributeValue("type"); //$NON-NLS-1$
            if ("texture".equals(backgroundType)) { //$NON-NLS-1$
              backgroundPaint = JFreeChartEngine.getTexturePaint(backgroundNode, width, height, session);
            } else if ("gradient".equals(backgroundType)) { //$NON-NLS-1$
              backgroundPaint = JFreeChartEngine.getGradientPaint(backgroundNode, width, height);
            }
          }
        }

        // get the text color of the interval
        String textColor = intervalNode.selectSingleNode("text-color").getText(); //$NON-NLS-1$
        Stroke outlineStroke;
        if (intervalNode.selectSingleNode("stroke-width") != null) { //$NON-NLS-1$
          outlineStroke = new BasicStroke(Float.parseFloat(intervalNode.selectSingleNode("stroke-width").getText())); //$NON-NLS-1$
        } else {
          outlineStroke = new BasicStroke();
        }
        Paint outlinePaint = JFreeChartEngine.getPaint(textColor);

        // create the interval object
        MeterInterval interval = new MeterInterval(label, range, outlinePaint, outlineStroke, backgroundPaint);

        // add the interval to the widget
        widgetDefinition.addInterval(interval);
      }
    }

    // get the chart subtitles

    // A list of <subtitle> nodes should not be allowed to exist as a child of the main XML element (for XML schema to 
    // be well constructed and validate the XML . 
    // We have deprecated <subtitle> as a child of the main node , and now require a <subtitles> parent node 
    // under which <subtitle> can exist. 

    List subtitles = dialNode.selectNodes(ChartDefinition.SUBTITLE_NODE_NAME);

    if ((subtitles == null) || (subtitles.isEmpty())) {
      Node subTitlesNode = dialNode.selectSingleNode(ChartDefinition.SUBTITLES_NODE_NAME);
      if (subTitlesNode != null) {
        subtitles = subTitlesNode.selectNodes(ChartDefinition.SUBTITLE_NODE_NAME);
      }
    } else {
      // log a deprecation warning for this property...
      DialWidgetDefinition.getLogger().warn(
          Messages.getString(
              "CHART.WARN_DEPRECATED_CHILD", ChartDefinition.SUBTITLE_NODE_NAME, ChartDefinition.SUBTITLES_NODE_NAME));//$NON-NLS-1$ 
      DialWidgetDefinition.getLogger().warn(
          Messages.getString("CHART.WARN_PROPERTY_WILL_NOT_VALIDATE", ChartDefinition.SUBTITLE_NODE_NAME));//$NON-NLS-1$  
    }

    if (subtitles != null) {
      widgetDefinition.addSubTitles(subtitles);
    }

  }

  public void setUnits(final String units) {
    this.units = units;
  }

  public String getUnits() {
    return units;
  }

  private void setDataByColumn(final IPentahoResultSet data) {
    setDataByRow(PentahoDataTransmuter.pivot(data));
  }

  private void setDataByRow(final IPentahoResultSet data) {

    if (data == null) {
      noDataMessage = Messages.getString("CHART.USER_NO_DATA_AVAILABLE"); //$NON-NLS-1$
      return;
    }

    Object[] rowData = data.next();

    List<Number> numericRowData = DataUtilities.toNumbers(rowData, LocaleHelper.getNumberFormat(), LocaleHelper
        .getCurrencyFormat());

    double newValue = numericRowData.get(0).doubleValue();

    // Do we have a value, minimum and maximum? 
    if (rowData.length >= 3) {

      this.setMinimum(numericRowData.get(1).doubleValue());
      this.setMaximum(numericRowData.get(2).doubleValue());

    } else {

      deriveMinMax(newValue);

    }

    this.setValue(newValue);

  }

  public void deriveMinMax(final double value) {

    double min = 0;
    double max = 100;
    Node node = attributes.selectSingleNode("range-limited"); //$NON-NLS-1$
    rangeLimited = (node == null) || ("true".equalsIgnoreCase(node.getText())); //$NON-NLS-1$

    if (!rangeLimited) {
    } else {
      max = 0.1;
      double absValue = Math.abs(value);
      // based on the current value, to to select some sensible min and
      // max values
      while (max < absValue) {
        max *= 2;
        if (max < absValue) {
          min *= 2.5;
          max *= 2.5;
        }
        if (max < absValue) {
          min *= 2;
          max *= 2;
        }
      }
      if (value > 0) {
        min = 0;
      } else {
        min = -max;
      }
      setMaximum(max);
      setMinimum(min);
    }
  }

  /**
   * Add an interval (MeterInterval) to the dial definition. The interval
   * defines a range and how it should be painted. <p/> The dial images here
   * have three intervals. The lowest interval has a minimum of 0 and a
   * maximum of 30. <p/> Intervals have a color. In this image the lowest
   * interval color is set to red. <br/> <img
   * src="doc-files/DialWidgetDefinition-5.png"> <p/> Intervals have a text
   * color. In this image the lowest interval text color is set to red. This
   * affects the outer rim, the interval value text <br/> <img
   * src="doc-files/DialWidgetDefinition-6.png">
   *
   * @param interval
   *            A MeterInterval that defines an interval (range) on the dial
   */
  public void addInterval(final MeterInterval interval) {
    intervals.add(interval);
    Range range = interval.getRange();
    double min = range.getLowerBound();
    double max = range.getUpperBound();
    if (rangeLimited && (intervals.size() == 1)) {
      setMinimum(min);
      setMaximum(max);
    } else {
      if (min < getMinimum()) {
        setMinimum(min);
      }
      if (max > getMaximum()) {
        setMaximum(max);
      }
    }
  }

  /**
   * Sets the value to be displayed on the dial image
   *
   * @param value
   *            The value to be displayed
   */

  public void setValue(final double value) {
    setValue(new Double(value));
    if (rangeLimited) {
      if (value < getMinimum()) {
        setValue(getMinimum());
      } else if (value > getMaximum()) {
        setValue(getMaximum());
      }
    } else {
      if (value < getMinimum()) {
        setMinimum(value);
      } else if (value > getMaximum()) {
        setMaximum(value);
      }
    }
  }

  /**
   * Return the java.awt.Paint object to be used to paint the backound of the
   * dial.
   *
   * @return The Paint to be used
   */
  public Paint getPlotBackgroundPaint() {
    return plotBackgroundPaint;
  }

  /**
   * Return the java.awt.Paint object to be used to paint the backound of the
   * dial. <p/> In this image the background paint has been set to red <br/>
   * <img src="doc-files/DialWidgetDefinition-2.png">
   *
   * @return The Paint to used for the background of the image
   */
  public void setPlotBackgroundPaint(final Paint plotBackgroundPaint) {
    this.plotBackgroundPaint = plotBackgroundPaint;
  }

  /**
   * Return the java.awt.Paint used to paint the needle of the dial image
   *
   * @return The Paint to use for the needle of this dial
   */
  public Paint getNeedlePaint() {
    return needlePaint;
  }

  /**
   * Sets the java.awt.Paint object to be used to paint the needle of the dial
   * image. <p/> In this image the needle paint has been set to red. <br/>
   * <img src="doc-files/DialWidgetDefinition-4.png">
   *
   * @param needlePaint
   *            The Paint to use for ths needle of this dial
   */
  public void setNeedlePaint(final Paint needlePaint) {
    this.needlePaint = needlePaint;
  }

  /**
   * Return the shape to be used for the dial.
   *
   * @return DialShape The DialShape for this dial
   */
  public DialShape getDialShape() {
    return dialShape;
  }

  /**
   * Return the java.awt.Font to be used to display the dial title
   *
   * @return Font The Font for the title of this dial
   */
  public Font getTitleFont() {
    if (titleFont != null) {
      return titleFont;
    } else {
      return new Font("sans-serif", Font.PLAIN, 14); //$NON-NLS-1$
    }
  }

  public void setTitleFont(final Font tFont) {
    titleFont = tFont;

  }

  /**
   * Sets the shape to be used for the dial. This affects the area of dial
   * outside the range that the needle covers. <table>
   * <tr>
   * <td><center>CIRCLE</center></td>
   * <td><center>CHORD</center></td>
   * <td><center>PIE</center></td>
   * </tr>
   * <tr>
   * <td><img src="doc-files/DialWidgetDefinition-3.png"></td>
   * <td><img src="doc-files/DialWidgetDefinition-8.png"></td>
   * <td><img src="doc-files/DialWidgetDefinition-9.png"></td>
   * </tr>
   * </table>
   *
   * @param dialShape
   *            The shape for this dial
   */
  public void setDialShape(final DialShape dialShape) {
    this.dialShape = dialShape;
  }

  /**
   * Return a list of the intervals for the dial. Each object in the list is a
   * MeterInterval object.
   *
   * @return List The list of MeterInterval objects for this dial
   */

  public List getIntervals() {
    return intervals;
  }

  public Paint[] getPaintSequence() {
    return null;
  }

  public Image getPlotBackgroundImage() {
    return null;
  }

  public List getSubtitles() {
    return subTitles;
  }

  public void addSubTitles(final List subTitleNodes) {
    if (subTitleNodes != null) {
      Iterator iter = subTitleNodes.iterator();
      while (iter.hasNext()) {
        addSubTitle(((Node) iter.next()).getText());
      }
    }
  }

  public void addSubTitle(final String subTitle) {
    subTitles.add(subTitle);
  }

  public Paint getChartBackgroundPaint() {
    // TODO Auto-generated method stub
    return chartBackgroundPaint;
  }

  public Image getChartBackgroundImage() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isBorderVisible() {
    // TODO Auto-generated method stub
    return false;
  }

  public Paint getBorderPaint() {
    // TODO Auto-generated method stub
    return null;
  }

  public RectangleEdge getTitlePosition() {
    return titlePosition;
  }

  /**
   * @param chartBackgroundPaint
   *            The chartBackgroundPaint to set.
   */
  public void setChartBackgroundPaint(final Paint chartBackgroundPaint) {
    this.chartBackgroundPaint = chartBackgroundPaint;
  }

  public int getHeight() {
    // TODO Auto-generated method stub
    return 200;
  }

  public int getWidth() {
    // TODO Auto-generated method stub
    return 200;
  }

  public String getTitle() {
    return null;
  }

  public boolean isLegendIncluded() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isThreeD() {
    // TODO Auto-generated method stub
    return false;
  }

  public Paint getValuePaint() {
    return valuePaint;
  }

  public Paint getTickPaint() {
    return tickPaint;
  }

  public int getTickSize() {
    return tickSize;
  }

  public void setValuePaint(final Paint valuePaint) {
    this.valuePaint = valuePaint;
  }

  public void setTickPaint(final Paint tickPaint) {
    this.tickPaint = tickPaint;
  }

  public void setTickSize(final int tickSize) {
    this.tickSize = tickSize;
  }

  @Override
  public Font getValueFont() {
    return valueFont;
  }

  public void setValueFont(final Font valueFont) {
    this.valueFont = valueFont;
  }

  public boolean isDisplayLabels() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Return the java.awt.Font to be used to display the legend items
   *
   * @return Font The font for the legend items
   */
  public Font getLegendFont() {
    // TODO Auto-generated method stub
    return legendFont;
  }

  /**
   * Set java.awt.Font to be used to display the legend items
   *
   * @param Font The java.awt.Font for the legend items
   */
  public void setLegendFont(final Font legendFont) {
    this.legendFont = legendFont;
  }

  public void setLegendFont(final Node legendFontNode) {
    Font font = JFreeChartEngine.getFont(legendFontNode);
    if (font != null) {
      setLegendFont(font);
    }
  }

  public void setLegendBorderVisible(final Node legendBorderVisibleNode) {
    if (legendBorderVisibleNode != null) {
      boolean legBorderVisible = (new Boolean(legendBorderVisibleNode.getText())).booleanValue();
      setLegendBorderVisible(legBorderVisible);
    }
  }

  /**
   * @param boolean legendBorderVisible
   *        Set the visibility of the legend border.
   */
  public void setLegendBorderVisible(final boolean legendBorderVisible) {
    this.legendBorderVisible = legendBorderVisible;
  }

  /**
   * Return the boolen that states if the legend border is visible
   *
   * @return boolean Is the legend border visible
   */
  public boolean isLegendBorderVisible() {
    return legendBorderVisible;
  }
  
  public Float getBackgroundAlpha() {
		return backgroundAlpha;
	}

	public void setBackgroundAlpha(Node backgroundAlphaNode) {
		if (backgroundAlphaNode != null) {
			Float backgroundAlphaValue = new Float(backgroundAlphaNode.getText());
			this.backgroundAlpha = backgroundAlphaValue;
		}

	}

	public Float getForegroundAlpha() {
		return foregroundAlpha;
	}

	public void setForegroundAlpha(Node foregroundAlphaNode) {
		if (foregroundAlphaNode != null) {
			Float foregroundAlphaValue = new Float(foregroundAlphaNode.getText());
			this.foregroundAlpha = foregroundAlphaValue;
		}

	}

  
  

}