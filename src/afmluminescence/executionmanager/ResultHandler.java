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
import afmluminescence.luminescencegenerator.GeneratorManager;
import com.sun.jdi.AbsentInformationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author audreyazura
 */
public class ResultHandler implements Runnable
{
    private final ExecutionManager m_manager;
    private boolean m_continuousMonitoring = true;
    private GeneratorManager m_simulator;
    
    public ResultHandler ()
    {
        m_manager = null;
        m_simulator = null;
    }
    
    public ResultHandler (ExecutionManager p_manager)
    {
        m_manager = p_manager;
        m_simulator = null;
    }
    
    @Override
    public void run()
    {
        while(m_continuousMonitoring)
        {
            if (m_simulator != null)
            {
                HashMap<Electron, BigDecimal> results = new HashMap<>();
                
                while (results.isEmpty())
                {
                    try
                    {
                        Thread.sleep(500);
                        results = m_simulator.getFinalElectronList();
                    }
                    catch (IllegalStateException ex)
                    {
                        Logger.getLogger(ResultHandler.class.getName()).log(Level.FINER, ex.getMessage(), ex);
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(ResultHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                System.out.println("Simulation finished.");
                
                List<BigDecimal> recombinationTimes = new ArrayList(results.values());
                List<BigDecimal> recombinationEnergies = new ArrayList<>();

                for (Electron el: results.keySet())
                {
                    try
                    {
                        recombinationEnergies.add(el.getRecombinationEnergy());
                    }
                    catch (AbsentInformationException ex)
                    {
                        Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                m_simulator = null;
                m_manager.computeResults(recombinationEnergies, recombinationTimes);
            }
        }
    }
    
    public void initializeTrackedGenerator (GeneratorManager p_simulator)
    {
        m_simulator = p_simulator;
    }
    
    public void stopMonitoring()
    {
        m_continuousMonitoring = false;
    }
}
