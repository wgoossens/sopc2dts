/*
sopc2dts - Devicetree generation for Altera systems

Copyright (C) 2011 Walter Goossens <waltergoossens@home.nl>

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package sopc2dts.parsers.sopcinfo;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import sopc2dts.Logger;
import sopc2dts.lib.AvalonSystem.SystemDataType;
import sopc2dts.lib.components.Interface;
import sopc2dts.lib.components.BasicComponent;


public class SopcInfoInterface extends SopcInfoElementWithParams {
	protected SystemDataType type = SystemDataType.CONDUIT;
	protected boolean isMaster;
	Interface bi;

	public SopcInfoInterface(ContentHandler p, XMLReader xr, String iName,
			BasicComponent owner, Attributes atts) {
		super(p, xr, owner);
		setKind(atts.getValue("kind"));
		bi = new Interface(iName, type, isMaster, owner);
		owner.getInterfaces().add(bi);
	}
	boolean setKind(String k)
	{
		boolean valid = false;
		if(k.equalsIgnoreCase("avalon_master") || 
				k.equalsIgnoreCase("avalon_tristate_master"))
		{
			valid = true;
			type = SystemDataType.MEMORY_MAPPED;
			isMaster = true;
		} else if(k.equalsIgnoreCase("avalon_slave") ||
				k.equalsIgnoreCase("avalon_tristate_slave"))
		{
			valid = true;
			type = SystemDataType.MEMORY_MAPPED;
			isMaster = false;
		} else if(k.equalsIgnoreCase("clock_sink"))
		{
			valid = true;
			type = SystemDataType.CLOCK;
			isMaster = false;
		} else if(k.equalsIgnoreCase("clock_source"))
		{
			valid = true;
			type = SystemDataType.CLOCK;
			isMaster = true;
		} else if(k.equalsIgnoreCase("interrupt_receiver"))
		{
			valid = true;
			type = SystemDataType.INTERRUPT;
			isMaster = true;
		} else if(k.equalsIgnoreCase("interrupt_sender"))
		{
			valid = true;
			type = SystemDataType.INTERRUPT;
			isMaster = false;
		} else if(k.equalsIgnoreCase("nios_custom_instruction_master"))
		{
			valid = true;
			type = SystemDataType.CUSTOM_INSTRUCTION;
			isMaster = true;
		} else if(k.equalsIgnoreCase("nios_custom_instruction_slave"))
		{
			valid = true;
			type = SystemDataType.CUSTOM_INSTRUCTION;
			isMaster = false;
		} else if(k.equalsIgnoreCase("avalon_streaming_sink"))
		{
			valid = true;
			type = SystemDataType.STREAMING;
			isMaster = false;
		} else if(k.equalsIgnoreCase("avalon_streaming_source"))
		{
			valid = true;
			type = SystemDataType.STREAMING;
			isMaster = true;
		} else if(k.equalsIgnoreCase("conduit") ||
				k.equalsIgnoreCase("conduit_start") ||
				k.equalsIgnoreCase("conduit_end"))
		{
			valid = true;
			type = SystemDataType.CONDUIT;
			isMaster = false;
		}
		if(!valid) {
			Logger.logln("Unsupported interface kind: " + k);
		}
		return valid;
	}

	public int getAddressableSize()
	{
		int span = 1;
		int stepSize = 1;
		String assVal = getParamValue("addressSpan");
		if(assVal!=null)
		{
			span = Integer.decode(assVal);
		}
		assVal = getParamValue("addressUnits");
		if(assVal!=null)
		{
			if(assVal.equalsIgnoreCase("WORDS"))
			{
				stepSize = 4;
			} else {
				System.out.println("Unsupported AddressUnits: " + assVal + " reverting to bytes");
			}
		}
		return span * stepSize;
	}
	@Override
	public void endElement(String uri, String localName, String qName)
		throws SAXException {
		if(localName.equalsIgnoreCase(getElementName()))
		{
			if(bi.isMemorySlave())
			{
				bi.setInterfaceValue(getAddressableSize());
			} else if(bi.isClockMaster())
			{
				bi.setInterfaceValue(Integer.decode(getParamValue("clockRate")));
			}
		}
		super.endElement(uri, localName, qName);
	}

	@Override
	public String getElementName() {
		return "interface";
	}
}
