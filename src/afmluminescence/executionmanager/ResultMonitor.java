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
import com.github.audreyazura.commonutils.PhysicsTools;
import com.sun.jdi.AbsentInformationException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author audreyazura
 */
public class ResultMonitor implements Runnable
{
    private final ExecutionManager m_manager;
    private GeneratorManager m_simulator;
    private Thread m_monitoredThread;
    
    public ResultMonitor ()
    {
        m_manager = null;
        m_simulator = null;
        m_monitoredThread = null;
    }
    
    public ResultMonitor (ExecutionManager p_manager, GeneratorManager p_simulator, Thread p_toMonitor)
    {
        m_manager = p_manager;
        m_simulator = p_simulator;
        m_monitoredThread = p_toMonitor;
    }
    
    @Override
    public void run()
    {
        if (m_simulator != null && m_monitoredThread != null)
        {
            HashMap<Electron, BigDecimal> results = new HashMap<>();

            try
            {
                m_monitoredThread.join();
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(ResultMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }

            results = m_simulator.getFinalElectronList();
            List<BigDecimal> recombinationTimes = new ArrayList<>();
            List<BigDecimal> recombinationWavelengths = new ArrayList<>();

            for (Electron el: results.keySet())
            {
                try
                {
                    BigDecimal wavelength = PhysicsTools.h.multiply(PhysicsTools.c).divide(el.getRecombinationEnergy(), MathContext.DECIMAL128);
                    wavelength = wavelength.setScale(wavelength.scale() - wavelength.precision() + 4, RoundingMode.HALF_UP);
                    recombinationWavelengths.add(new BigDecimal(wavelength.toString()));
                    recombinationTimes.add(new BigDecimal(results.get(el).toString()));
                }
                catch (AbsentInformationException ex)
                {
                    Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            m_manager.computeResults(recombinationWavelengths, recombinationTimes);
        }
    }
    
    public void initializeTrackedGenerator (GeneratorManager p_simulator, Thread p_toMonitor)
    {
        m_simulator = p_simulator;
        m_monitoredThread = p_toMonitor;
    }
}
