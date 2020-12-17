/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author audreyazura
 */
public class GeneratorManager implements Runnable
{
    private final BigDecimal m_vth;
    private final DrawingSurface m_canvas;
    private final int m_nElectron;
    
    public GeneratorManager (DrawingSurface p_canvas, int p_nElectron, BigDecimal p_temperature)
    {
        m_canvas = p_canvas;
        m_nElectron = p_nElectron;
        m_vth = formatBigDecimal((PhysicsTools.KB.multiply(p_temperature).divide(PhysicsTools.ME, MathContext.DECIMAL128)).sqrt(MathContext.DECIMAL128));
    }
    
    @Override
    public void run()
    {
        BigDecimal sampleXSize = formatBigDecimal((new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier()));
        BigDecimal sampleYSize = formatBigDecimal((new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier()));
        
        List<Electron> electronList = new ArrayList<>();
        
//        PcgRSFast randomGenerator = new PcgRSFast();
//        
//        for (int i = 0 ; i < m_nElectron ; i += 1)
//        {
//            BigDecimal x = formatBigDecimal((new BigDecimal(randomGenerator.nextDouble())).multiply(canvasXSize));
//            BigDecimal y = formatBigDecimal((new BigDecimal(randomGenerator.nextDouble())).multiply(canvasYSize));
//            
//            BigDecimal v_x = formatBigDecimal((new BigDecimal(randomGenerator.nextGaussian())).multiply(vth));
//            BigDecimal v_y = formatBigDecimal((new BigDecimal(randomGenerator.nextGaussian())).multiply(vth));
//            
//            electronList.add(new Electron(x, y, v_x, v_y));
//        }
        
        electronList.add(new Electron(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000"), new BigDecimal("100")));
        
        for (Electron curentElectron: electronList)
        {
            while(true)
            {
//                m_canvas.reset();
//                System.out.println(curentElectron);
                curentElectron.stepInTime(new BigDecimal("1e-15"), sampleXSize, sampleYSize);
                m_canvas.drawAbsorberObject(curentElectron, sampleXSize, sampleYSize, BigDecimal.valueOf(5));
            }
        }
    }
    
    private BigDecimal formatBigDecimal(BigDecimal p_toFormat)
    {
        return p_toFormat.stripTrailingZeros();
    }
}
