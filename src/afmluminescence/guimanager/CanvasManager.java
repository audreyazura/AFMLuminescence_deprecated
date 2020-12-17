/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

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
    private DrawingBuffer m_buffer;
    private GraphicsContext m_canvasPainter;
    private Stage m_stage;
    
    private void drawAbsorberObjects()
    {
        m_canvasPainter.clearRect(0, 0, m_xWidth.doubleValue(), m_yWidth.doubleValue());
        ArrayList<ObjectToDraw> toDraw = m_buffer.download();
            
        if (toDraw.size() > 0)
        {
            m_canvasPainter.setFill(Color.RED);
            double radius = 5;
            for(ObjectToDraw thingToDraw: toDraw)
            {
                double xDrawing = (thingToDraw.getX().multiply(m_xWidth.divide(m_sampleXSize, MathContext.DECIMAL128))).doubleValue() - radius;
                double yDrawing = (thingToDraw.getY().multiply(m_yWidth.divide(m_sampleYSize, MathContext.DECIMAL128))).doubleValue() - radius;
                double diameter = radius * 2;
                m_canvasPainter.fillOval(xDrawing, yDrawing, diameter, diameter);
            }
            m_canvasPainter.setFill(Color.TRANSPARENT);
        }
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
        
        Group canvasRegion = new Group(drawingSpace);
        Scene currentScene = new Scene(canvasRegion);
        
        m_stage.setScene(currentScene);
        m_stage.show();
        
        ImageBuffer buffer = new DrawingBuffer();
        m_buffer = (DrawingBuffer) buffer;
        
        m_sampleXSize = (new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        m_sampleYSize = (new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        GeneratorManager luminescenceGenerator = new GeneratorManager(buffer, 150, new BigDecimal("300"), m_sampleXSize, m_sampleYSize);
        (new Thread(luminescenceGenerator)).start();
        
        Timeline animation = new Timeline(
            new KeyFrame(
                    Duration.seconds(0),
                    event -> drawAbsorberObjects()
                        ),
            new KeyFrame(Duration.millis(1))
        );
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
    }
}
