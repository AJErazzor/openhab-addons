//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.03.20 at 05:23:12 PM CET 
//

package org.openhab.ui.cometvisu.internal.config.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openhab.ui.cometvisu.internal.config.AdapterCDATA;

/**
 * {@inheritDoc}
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "status", propOrder = { "value" })
public class CDataStatus extends Status {

    @XmlJavaTypeAdapter(AdapterCDATA.class)
    protected String value;

}
