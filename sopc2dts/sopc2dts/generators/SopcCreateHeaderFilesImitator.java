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
package sopc2dts.generators;

import java.util.Vector;

import sopc2dts.lib.Parameter;
import sopc2dts.lib.AvalonSystem;
import sopc2dts.lib.BoardInfo;
import sopc2dts.lib.Connection;
import sopc2dts.lib.components.BasicComponent;
import sopc2dts.lib.components.Interface;

public class SopcCreateHeaderFilesImitator extends AbstractSopcGenerator {
	static String lameCopyright;
	Vector<BasicComponent> vHandled = new Vector<BasicComponent>();
	public SopcCreateHeaderFilesImitator(AvalonSystem s) {
		super(s);
		lameCopyright = "/*\n" +
				" * This file was not generated by the swinfo2header utility.\n" +
				" */\n" + copyRightNotice;
	}

	@Override
	public String getExtension() {
		return "h";
	}

	protected String dumpComponent(BasicComponent comp, String pov)
	{
		String res = "";
		String prefix = "";
		if(!comp.getInstanceName().equalsIgnoreCase(pov))
		{
			prefix = definenify(comp.getInstanceName()) + '_';
		}
		res += "/*\n" +
				" * Macros for module '" + comp.getInstanceName() + "', class '" + comp.getScd().getClassName() + "'.\n";
		if(prefix.length()>0)
		{
			res += " * The macros are prefixed with '" + prefix + "'.\n" +
					" * The prefix is the slave descriptor.\n";
		} else {
			res += " * The macros have no prefix.\n";
		}
		res += " */\n";
		if(!comp.getInstanceName().equalsIgnoreCase(pov))
		{
			res += "#define " + prefix + "COMPONENT_TYPE " + comp.getScd().getClassName() + "\n" +
					"#define " + prefix +"COMPONENT_NAME " + comp.getInstanceName() + "\n";
		}
		for(Parameter ass : comp.getParams())
		{
			if(ass.getName().toUpperCase().startsWith("EMBEDDEDSW.CMACRO."))
			{
				res += "#define " + prefix + definenify(ass.getName().substring(18)) + ' ';
				if(ass.getValue()!=null) 
				{
					res += ass.getValue();
				}
				res += "\n";
			}
		}
		return res += "\n";
	}
	public String dumpChildren(BasicComponent master, String pov)
	{
		String res = "";
		for(Interface intf : master.getInterfaces())
		{
			if(intf.isMemoryMaster())
			{
				for(Connection conn : intf.getConnections())
				{
					if(!vHandled.contains(conn.getSlaveInterface()))
					{
						if(!conn.getSlaveModule().getScd().getGroup().equals("bridge"))
						{
							res += dumpComponent(conn.getSlaveModule(), pov);
						} else {
							res += dumpChildren(conn.getSlaveModule(), pov);
						}
					}
				}
			}
		}
		return res;
	}
	@Override
	public String getOutput(BoardInfo bi) {
		String res = null;
		vHandled.clear();
		if(bi.getPov()==null)
		{
			for(BasicComponent comp : sys.getSystemComponents())
			{
				if(!comp.getScd().getGroup().equals("bridge"))
				{
					if(res==null)
					{
						res = "#ifndef _ALTERA_CPU_H_\n" +
							"#define _ALTERA_CPU_H_\n\n" +
							lameCopyright;
					}
					res += dumpComponent(comp, bi.getPov());
				}
			}
			if(res!=null)
			{
				res += "\n#endif //_ALTERA_CPU_H_\n";
			}
		} else {
			BasicComponent povComp = sys.getComponentByName(bi.getPov());
			if(povComp!=null)
			{
				res = "#ifndef _ALTERA_" + definenify(bi.getPov()) + "_H_\n" +
				"#define _ALTERA_" + definenify(bi.getPov()) + "_H_\n\n" +
				lameCopyright + "\n/*\n" +
						" * This file contains macros for module '" + bi.getPov() + "' and devices\n" +
						" * connected to the following masters:\n";
				for(Interface intf : povComp.getInterfaces())
				{
					if(intf.isMemoryMaster())
					{
						res += " *   " + intf.getName() + "\n";
					}
				}
				res += " * \n" +
						" * Do not include this header file and another header file created for a\n" +
						" * different module or master group at the same time.\n" +
						" * Doing so may result in duplicate macro names.\n" +
						" * Instead, use the system header file which has macros with unique names.\n" +
						" */\n" +
						"\n" + dumpComponent(povComp, bi.getPov())
						+ dumpChildren(povComp, bi.getPov());
				res += "\n#endif /* _ALTERA_" + definenify(bi.getPov()) + "_H_ */\n";

			}
		}
		return res;
	}
}
