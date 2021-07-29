/*
 * Copyright (C) 2021 audreyazura
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
package afmluminescence.executionmanager;

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import afmluminescence.luminescencegenerator.QuantumDot;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author audreyazura
 */
public class ImageInterpretator implements ImageBuffer
{
    private final GUIUpdater m_gui;
    
    public ImageInterpretator (GUIUpdater p_gui)
    {
        m_gui = p_gui;
    }
    
    /**
     * Format the data to send them to the GUI
     * @param p_electronsToDraw the list of electrons
     * @param p_qdsToDraw the list of QDs
     * @param p_time the time passed in the simulation, in nanoseconds
     */
    @Override
    public void logObjects(List<Electron> p_electronsToDraw, List<QuantumDot> p_qdsToDraw, BigDecimal p_time)
    {
        int numberRecombinedElectron = 0;
        
        for (Electron electron: p_electronsToDraw)
        {
            if (electron.isRecombined())
            {
                numberRecombinedElectron += 1;
            }
        }
        
        String timeUnit = p_time.toPlainString() + " ns";
        String recombinedRatio = numberRecombinedElectron + "/" + p_electronsToDraw.size();
        
        m_gui.updateProgress((double) numberRecombinedElectron / p_electronsToDraw.size(), timeUnit, recombinedRatio);
    }
}
