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
import javafx.scene.text.Font;
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
    private GraphicsContext m_timePainter;
    
    private void drawAnimated()
    {
        m_canvasPainter.clearRect(0, 0, m_xWidth.doubleValue(), m_yWidth.doubleValue());
        ArrayList<ObjectToDraw> electrons = m_buffer.downloadElectron();
        String time = m_buffer.getTimePassed();
            
        if (electrons.size() > 0)
        {
            m_canvasPainter.setFill(Color.RED);
            for(ObjectToDraw electronToDraw: electrons)
            {
                double xDrawing = (electronToDraw.getX().multiply(m_xWidth.divide(m_sampleXSize, MathContext.DECIMAL128))).doubleValue() - electronToDraw.getRadius();
                double yDrawing = (electronToDraw.getY().multiply(m_yWidth.divide(m_sampleYSize, MathContext.DECIMAL128))).doubleValue() - electronToDraw.getRadius();
                double diameter = electronToDraw.getRadius() * 2;
                m_canvasPainter.fillOval(xDrawing, yDrawing, diameter, diameter);
            }
            m_canvasPainter.setFill(Color.TRANSPARENT);
        }
        
        m_timePainter.clearRect(120, 0, 100, 30);
        m_timePainter.setFill(Color.BLACK);
        m_timePainter.fillRect(115, 0, 100, 30);
        m_timePainter.setFill(Color.WHITE);
        m_timePainter.fillText(time + " fs", 120, 20);
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
        stage.setResizable(false);
        
        m_xWidth = new BigDecimal("1000");
        m_yWidth = new BigDecimal("1000");
        Canvas animationCanvas = new Canvas(m_xWidth.doubleValue(), m_yWidth.doubleValue());
        m_canvasPainter = animationCanvas.getGraphicsContext2D();
        
        Canvas timeCanvas = new Canvas(250, 30);
        m_timePainter = timeCanvas.getGraphicsContext2D();
        m_timePainter.setFill(Color.BLACK);
        m_timePainter.fillRect(0, 0, 120, 30);
        m_timePainter.setFill(Color.WHITE);
        m_timePainter.setFont(new Font("Source Sans Pro", 20));
        m_timePainter.fillText("Time passed: ", 5, 20);
        
        Group canvasRegion = new Group(animationCanvas, timeCanvas);
        Scene currentScene = new Scene(canvasRegion);
        
        stage.setScene(currentScene);
        stage.setTitle("Electron circulating");
        stage.show();
        
        ImageBuffer buffer = new DrawingBuffer();
        m_buffer = (DrawingBuffer) buffer;
        
        m_sampleXSize = (new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        m_sampleYSize = (new BigDecimal(2)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        GeneratorManager luminescenceGenerator = new GeneratorManager(buffer, 1000, new BigDecimal("300"), m_sampleXSize, m_sampleYSize);
        (new Thread(luminescenceGenerator)).start();
        
        Timeline animation = new Timeline(
            new KeyFrame(
                    Duration.seconds(0),
                    event -> drawAnimated()
                        ),
            new KeyFrame(Duration.millis(30))
        );
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
    }
}
