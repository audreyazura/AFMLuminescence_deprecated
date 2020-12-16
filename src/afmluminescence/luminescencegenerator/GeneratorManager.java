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

/**
 *
 * @author audreyazura
 */
public class GeneratorManager
{
    public void start(int p_nElectron, BigDecimal p_temperature)
    {
        PcgRSFast randomGenerator = new PcgRSFast();
        BigDecimal vth = formatBigDecimal((PhysicsTools.KB.multiply(p_temperature).divide(PhysicsTools.ME, MathContext.DECIMAL128)).sqrt(MathContext.DECIMAL128));
        
        BigDecimal canvasXSize = formatBigDecimal((new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier()));
        BigDecimal canvasYSize = formatBigDecimal((new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier()));
        
        List<Electron> electronList = new ArrayList<>();
        
        for (int i = 0 ; i < p_nElectron ; i += 1)
        {
            BigDecimal x = formatBigDecimal((new BigDecimal(randomGenerator.nextDouble())).multiply(canvasXSize));
            BigDecimal y = formatBigDecimal((new BigDecimal(randomGenerator.nextDouble())).multiply(canvasYSize));
            
            BigDecimal v_x = formatBigDecimal((new BigDecimal(randomGenerator.nextGaussian())).multiply(vth));
            BigDecimal v_y = formatBigDecimal((new BigDecimal(randomGenerator.nextGaussian())).multiply(vth));
            
            electronList.add(new Electron(x, y, v_x, v_y));
        }
    }
    
    private BigDecimal formatBigDecimal(BigDecimal p_toFormat)
    {
        return p_toFormat.stripTrailingZeros();
    }
}
