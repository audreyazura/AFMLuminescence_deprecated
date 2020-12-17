/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.Electron;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 *
 * @author audreyazura
 */
public class PainterManager implements Runnable
{
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    private final DrawingBuffer m_thingToDraw;
    private final GraphicsContext m_painter;
    
    public PainterManager (BigDecimal p_scaleX, BigDecimal p_scaleY, DrawingBuffer p_buffer, GraphicsContext p_graphicContext)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
        m_thingToDraw = p_buffer;
        m_painter = p_graphicContext;
    }
    
    public void run()
    {
        while(true)
        {
//            long startingTime = System.nanoTime();
            ArrayList<Electron> electronsToDraw = m_thingToDraw.download();
//            System.out.println("Time passed: " + (System.nanoTime() - startingTime) / 1000000000 + "s.\n");
            
            if (electronsToDraw.size() > 0)
            {
                m_painter.setFill(Color.RED);
                double radius = 5;
                for(Electron currentElectron: electronsToDraw)
                {
                    double xDrawing = (currentElectron.getX().multiply(m_scaleX)).doubleValue() - radius;
                    double yDrawing = (currentElectron.getY().multiply(m_scaleY)).doubleValue() - radius;
                    double diameter = radius * 2;
                    m_painter.fillOval(xDrawing, yDrawing, diameter, diameter);
                }
                m_painter.setFill(Color.TRANSPARENT);
            }
        }
    }
}
