/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author audreyazura
 */
public class ElectronMover implements Runnable
{
    private final BigDecimal m_sampleXSize;
    private final BigDecimal m_sampleYSize;
    private final BigDecimal m_timeStep;    
    private final List<Electron> m_electronList;
    private final List<QuantumDot> m_QDList;
    private final PcgRSFast m_randomGenerator;
    
    public ElectronMover (BigDecimal p_sampleXMax, BigDecimal p_sampleYMax, BigDecimal p_timeStep, List<Electron> p_electronToTreat, List<QuantumDot> p_sampleQDs)
    {
        m_sampleXSize = p_sampleXMax;
        m_sampleYSize = p_sampleYMax;
        m_timeStep = p_timeStep;
        m_electronList = new ArrayList(p_electronToTreat);
        m_QDList = new ArrayList(p_sampleQDs);
        m_randomGenerator = new PcgRSFast();
    }
    
    public boolean allRecombined()
    {
        boolean finished = true;
        
        for (Electron currentElectron: m_electronList)
        {
            finished &= currentElectron.isRecombined();
        }
        
        return finished;
    }
    
    public ArrayList<Electron> getElectronList()
    {
        return new ArrayList(m_electronList);
    }
    
    @Override
    public void run()
    {
        for (Electron curentElectron: m_electronList)
        {
            curentElectron.stepInTime(m_timeStep, m_sampleXSize, m_sampleYSize, m_QDList, m_randomGenerator);
        }
    }
}
