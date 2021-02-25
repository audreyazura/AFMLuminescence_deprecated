/*
 * Copyright (C) 2020-2021 Alban Lafuente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package afmluminescence.guimanager;

import net.opentsdb.tools.ArgP;

/**
 *
 * @author Alban Lafuente
 */
public class Launcher
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        final ArgP argParser = new ArgP();
        argParser.addOption("--lum", "File containing the luminescence data.");
        argParser.addOption("--QDs", "File containing the quantum dots size and position.");
        argParser.addOption("--help", "The command you just used.");
        
        //parsing the args to get the options passed to the program
        try
	{
	    args = argParser.parse(args);
	}
	catch (IllegalArgumentException e)
	{
	    System.err.println(e.getMessage());
	    System.err.print(argParser.usage());
	    System.exit(1);
	}
        
        if (argParser.has("--help"))
        {
            //just print help message, not continuing execution
            System.out.println(argParser.usage());
        }
        else
        {
            String[] arguments = new String[2];
            
            arguments[0] = argParser.get("--lum", "");
            arguments[1] = argParser.get("--QDs", "");
            
//            arguments[1] = "/home/audreyazura/Documents/Work/Simulation/AFMLuminescence/QDList.csv";
            
            CanvasManager absorberRepresentation = new CanvasManager();
            absorberRepresentation.startVisualizer(arguments);
        }
    }
    
}
