/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author audreyazura
 */
public interface ImageBuffer
{
    public void logElectrons(List<Electron> p_listToDraw);
    
    public void logTime(BigDecimal p_time);
}
