/*
 * Copyright (C) 2020-2021 Alban Lafuente
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
package afmluminescence.guimanager;

import afmluminescence.executionmanager.ExecutionManager;
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
 * @author Alban Lafuente
 */
public class GUIManager extends Application
{
    private BigDecimal m_canvasXWidth;
    private BigDecimal m_canvasYWidth;
    private BigDecimal m_sampleXSize;
    private BigDecimal m_sampleYSize;
    private DrawingBuffer m_buffer;
    private GraphicsContext m_canvasPainter;
    private GraphicsContext m_QDPainter;
    private GraphicsContext m_timePainter;
    
    private void drawAnimated()
    {
        m_canvasPainter.clearRect(0, 0, m_canvasXWidth.doubleValue(), m_canvasYWidth.doubleValue());
        m_QDPainter.clearRect(0, 0, m_canvasXWidth.doubleValue(), m_canvasYWidth.doubleValue());
        
        ArrayList<ObjectToDraw> electrons = m_buffer.downloadMoving();
        String time = m_buffer.getTimePassed();
            
        if (electrons.size() > 0)
        {
            for(ObjectToDraw electronToDraw: electrons)
            {
                m_canvasPainter.setFill(electronToDraw.getColor());
                double xDrawing = electronToDraw.getX().doubleValue();
                double yDrawing = electronToDraw.getY().doubleValue();
                double radius = electronToDraw.getRadius();
                m_canvasPainter.fillOval(xDrawing - radius, yDrawing - radius, radius * 2, radius * 2);
            }
            m_canvasPainter.setFill(Color.TRANSPARENT);
        }
        
        for (ObjectToDraw QDToDraw: m_buffer.downloadFixed())
        {
            m_QDPainter.setFill(QDToDraw.getColor());
            double xDrawing = QDToDraw.getX().doubleValue();
            double yDrawing = QDToDraw.getY().doubleValue();
            double diameter = QDToDraw.getRadius() * 2;
            m_QDPainter.fillOval(xDrawing, yDrawing, diameter, diameter);
        }
        
        m_timePainter.clearRect(120, 0, 100, 30);
        m_timePainter.setFill(Color.BLACK);
        m_timePainter.fillRect(115, 0, 100, 30);
        m_timePainter.setFill(Color.WHITE);
        m_timePainter.fillText(time + " ps", 120, 20);
    }
    
    public void startVisualizer(String[] args)
    {
        launch(args);
    }
    
    synchronized public void reset()
    {
        m_canvasPainter.clearRect(0, 0, m_canvasXWidth.doubleValue(), m_canvasYWidth.doubleValue());
    }
    
    @Override
    public void start (Stage stage)
    {
        stage.setResizable(false);
        
        m_canvasXWidth = new BigDecimal("1000");
        m_canvasYWidth = new BigDecimal("1000");
        
        m_sampleXSize = (new BigDecimal(1)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        m_sampleYSize = (new BigDecimal(1)).multiply(PhysicsTools.UnitsPrefix.MICRO.getMultiplier());
        BigDecimal scaleX = m_canvasXWidth.divide(m_sampleXSize, MathContext.DECIMAL128);
        BigDecimal scaleY = m_canvasYWidth.divide(m_sampleYSize, MathContext.DECIMAL128);
        DrawingBuffer buffer = new DrawingBuffer(scaleX, scaleY);
        m_buffer = buffer;
        
        new ExecutionManager(buffer, getParameters().getRaw(), m_sampleXSize, m_sampleYSize, scaleX, scaleY);
        
        Canvas animationCanvas = new Canvas(m_canvasXWidth.doubleValue(), m_canvasYWidth.doubleValue());
        m_canvasPainter = animationCanvas.getGraphicsContext2D();
        
        Canvas timeCanvas = new Canvas(250, 30);
        m_timePainter = timeCanvas.getGraphicsContext2D();
        m_timePainter.setFill(Color.BLACK);
        m_timePainter.fillRect(0, 0, 120, 30);
        m_timePainter.setFill(Color.WHITE);
        m_timePainter.setFont(new Font("Source Sans Pro", 20));
        m_timePainter.fillText("Time passed: ", 5, 20);
        
        Canvas QDCanvas = new Canvas(m_canvasXWidth.doubleValue(), m_canvasYWidth.doubleValue());
        m_QDPainter = QDCanvas.getGraphicsContext2D();
        ArrayList<ObjectToDraw> QDToDraw = new ArrayList<>();
        while (QDToDraw.size() == 0)
        {
           QDToDraw = m_buffer.downloadFixed();
        }
        
        for(ObjectToDraw QD: QDToDraw)
        {
            m_QDPainter.setFill(QD.getColor());
            m_QDPainter.fillOval(QD.getX().doubleValue(), QD.getY().doubleValue(), QD.getRadius() * 2, QD.getRadius() * 2);
        }
        
        Group canvasRegion = new Group(QDCanvas, animationCanvas, timeCanvas);
        Scene currentScene = new Scene(canvasRegion);
        
        stage.setScene(currentScene);
        stage.setTitle("Electron circulating");
        stage.show();
        
        Timeline animation = new Timeline(
            new KeyFrame(
                    Duration.seconds(0),
                    event -> drawAnimated()),
            new KeyFrame(Duration.millis(500))
//            new KeyFrame(Duration.millis(30))
        );
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
    }
}
