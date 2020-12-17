/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.AbsorberObject;
import afmluminescence.luminescencegenerator.GeneratorManager;
import com.github.audreyazura.commonutils.PhysicsTools;
import java.math.BigDecimal;
import java.math.MathContext;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import afmluminescence.luminescencegenerator.ImageBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author audreyazura
 */
public class CanvasManager extends Application
{
    private BigDecimal m_xWidth;
    private BigDecimal m_yWidth;
    private GraphicsContext m_drawingBuffer;
    private Stage m_stage;
    
    synchronized public void drawAbsorberObject(AbsorberObject p_objectToDraw, BigDecimal p_xScale, BigDecimal p_yScale, BigDecimal p_radius)
    {
        m_drawingBuffer.setFill(Color.RED);
        double xDrawing = ((p_objectToDraw.getX().multiply(m_xWidth.divide(p_xScale, MathContext.DECIMAL128))).subtract(p_radius)).doubleValue();
        double yDrawing = ((p_objectToDraw.getY().multiply(m_yWidth.divide(p_yScale, MathContext.DECIMAL128))).subtract(p_radius)).doubleValue();
        double diameter = p_radius.doubleValue() * 2;
        m_drawingBuffer.fillOval(xDrawing, yDrawing, diameter, diameter);
        
        m_drawingBuffer.setFill(Color.TRANSPARENT);
    }
    
    private void draw(DrawingBuffer p_readBuffer)
    {
        while(true)
        {
            p_readBuffer.download();
        }
    }
    
    public void startVisualizer()
    {
        launch();
    }
    
    synchronized public void reset()
    {
        m_drawingBuffer.clearRect(0, 0, m_xWidth.doubleValue(), m_yWidth.doubleValue());
    }
    
    @Override
    public void start (Stage stage)
    {
        m_stage = stage;
        m_stage.setResizable(false);
        
        m_xWidth = new BigDecimal("1000");
        m_yWidth = new BigDecimal("1000");
        Canvas drawingSpace = new Canvas(m_xWidth.doubleValue(), m_yWidth.doubleValue());
        m_drawingBuffer = drawingSpace.getGraphicsContext2D();
        m_drawingBuffer.save();
        
        Group canvasRegion = new Group(drawingSpace);
        Scene currentScene = new Scene(canvasRegion);
        
        m_stage.setScene(currentScene);
        m_stage.show();
        
        ImageBuffer buffer = new DrawingBuffer();
        GeneratorManager luminescenceGenerator = new GeneratorManager(buffer, 1, new BigDecimal("300"));
        (new Thread(luminescenceGenerator)).start();
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(CanvasManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        draw((DrawingBuffer) buffer);
    }
}
