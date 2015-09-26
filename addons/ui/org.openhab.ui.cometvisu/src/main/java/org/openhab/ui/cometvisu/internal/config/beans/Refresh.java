//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.17 at 05:50:37 PM CEST 
//

package org.openhab.ui.cometvisu.internal.config.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for refresh complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="refresh">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="layout" type="{}layout" minOccurs="0"/>
 *         &lt;element name="label" type="{}label" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{}value use="required""/>
 *       &lt;attribute ref="{}mapping"/>
 *       &lt;attribute ref="{}styling"/>
 *       &lt;attribute ref="{}align"/>
 *       &lt;attribute ref="{}flavour"/>
 *       &lt;attribute ref="{}bind_click_to_widget"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "refresh", propOrder = { "layout", "label" })
public class Refresh {

    protected Layout layout;
    protected Label label;
    @XmlAttribute(name = "value", required = true)
    protected String value;
    @XmlAttribute(name = "mapping")
    protected String mapping;
    @XmlAttribute(name = "styling")
    protected String styling;
    @XmlAttribute(name = "align")
    protected String align;
    @XmlAttribute(name = "flavour")
    protected String flavour;
    @XmlAttribute(name = "bind_click_to_widget")
    protected Boolean bindClickToWidget;

    /**
     * Gets the value of the layout property.
     * 
     * @return
     *         possible object is
     *         {@link Layout }
     * 
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Sets the value of the layout property.
     * 
     * @param value
     *            allowed object is
     *            {@link Layout }
     * 
     */
    public void setLayout(Layout value) {
        this.layout = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *         possible object is
     *         {@link Label }
     * 
     */
    public Label getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *            allowed object is
     *            {@link Label }
     * 
     */
    public void setLabel(Label value) {
        this.label = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the mapping property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getMapping() {
        return mapping;
    }

    /**
     * Sets the value of the mapping property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setMapping(String value) {
        this.mapping = value;
    }

    /**
     * Gets the value of the styling property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getStyling() {
        return styling;
    }

    /**
     * Sets the value of the styling property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setStyling(String value) {
        this.styling = value;
    }

    /**
     * Gets the value of the align property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getAlign() {
        return align;
    }

    /**
     * Sets the value of the align property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setAlign(String value) {
        this.align = value;
    }

    /**
     * Gets the value of the flavour property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getFlavour() {
        return flavour;
    }

    /**
     * Sets the value of the flavour property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setFlavour(String value) {
        this.flavour = value;
    }

    /**
     * Gets the value of the bindClickToWidget property.
     * 
     * @return
     *         possible object is
     *         {@link Boolean }
     * 
     */
    public Boolean isBindClickToWidget() {
        return bindClickToWidget;
    }

    /**
     * Sets the value of the bindClickToWidget property.
     * 
     * @param value
     *            allowed object is
     *            {@link Boolean }
     * 
     */
    public void setBindClickToWidget(Boolean value) {
        this.bindClickToWidget = value;
    }

}
