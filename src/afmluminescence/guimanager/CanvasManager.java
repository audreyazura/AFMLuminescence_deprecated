/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.AbsorberObject;
import afmluminescence.luminescencegenerator.Electron;
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
import java.util.ArrayList;
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
    private BigDecimal m_sampleXSize;
    private BigDecimal m_sampleYSize;
    private GraphicsContext m_canvasPainter;
    private Stage m_stage;
    
    synchronized public void drawAbsorberObject(AbsorberObject p_objectToDraw, BigDecimal p_xScale, BigDecimal p_yScale, BigDecimal p_radius)
    {
        m_canvasPainter.setFill(Color.RED);
        double xDrawing = ((p_objectToDraw.getX().multiply(m_xWidth.divide(p_xScale, MathContext.DECIMAL128))).subtract(p_radius)).doubleValue();
        double yDrawing = ((p_objectToDraw.getY().multiply(m_yWidth.divide(p_yScale, MathContext.DECIMAL128))).subtract(p_radius)).doubleValue();
        double diameter = p_radius.doubleValue() * 2;
        m_canvasPainter.fillOval(xDrawing, yDrawing, diameter, diameter);
        
        m_canvasPainter.setFill(Color.TRANSPARENT);
    }
    
    public void startVisualizer()
    {
        launch();
    }
    
    synchronized public void reset()
    {
        m_canvasPainter.clearRect(0, 0, m_xWidth.doubleValue(), m_yWidth.doubleValue());
    }
    
    @Override
    public void start (Stage stage)
    {
        m_stage = stage;
        m_stage.setResizable(false);
        
        m_xWidth = new BigDecimal("1000");
        m_yWidth = new BigDecimal("1000");
        Canvas drawingSpace = new Canvas(m_xWidth.doubleValue(), m_yWidth.doubleValue());
        m_canvasPainter = drawingSpace.getGraphicsContext2D();
        m_canvasPainter.setFill(Color.BLUE);
        m_canvasPainter.fillRect(100, 500, 200, 200);
        m_canvasPainter.setFill(Color.TRANSPARENT);
        
        Group canvasRegion = new Group(drawingSpace);
        Scene currentScene = new Scene(canvasRegion);
        
        m_stage.setScene(currentScene);
        m_stage.show();
        
        ImageBuffer buffer = new DrawingBuffer();
        
        m_sampleXSize = (new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        m_sampleYSize = (new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        GeneratorManager luminescenceGenerator = new GeneratorManager(buffer, 1, new BigDecimal("300"), m_sampleXSize, m_sampleYSize);
        (new Thread(luminescenceGenerator)).start();
        
        PainterManager painterStarter = new PainterManager(m_xWidth.divide(m_sampleXSize, MathContext.DECIMAL128), m_yWidth.divide(m_sampleYSize, MathContext.DECIMAL128), (DrawingBuffer) buffer, m_canvasPainter);
        (new Thread(painterStarter)).run();
    }
}
