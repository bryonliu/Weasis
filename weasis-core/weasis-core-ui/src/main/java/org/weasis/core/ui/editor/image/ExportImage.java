/*******************************************************************************
 * Copyright (c) 2016 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.core.ui.editor.image;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.model.ViewModel;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.ImageOpNode;
import org.weasis.core.api.image.SimpleOpManager;
import org.weasis.core.api.image.util.ImageLayer;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.util.FontTools;
import org.weasis.core.ui.model.layer.LayerAnnotation;

public class ExportImage<E extends ImageElement> extends DefaultView2d<E> {
    private static final long serialVersionUID = 1149562889654679335L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportImage.class);

    private final ViewCanvas<E> view2d;
    private Graphics2D currentG2d;
    private double imagePrintingResolution = 1.0;

    public ExportImage(ViewCanvas<E> view2d) {
        super(view2d.getEventManager(), null);
        this.view2d = view2d;
        // Pixel iterator is necessary for updating label of measurements
        this.imageLayer.setBuildIterator(true);
        // Remove OpEventListener to avoid reseting some parameters when setting the series
        this.imageLayer.removeEventListener(imageLayer.getDisplayOpManager());
        setFont(FontTools.getFont8());
        this.infoLayer = view2d.getInfoLayer().getLayerCopy(this);
        infoLayer.setVisible(view2d.getInfoLayer().getVisible());
        infoLayer.setShowBottomScale(false);
        // For exporting view, remove Pixel value, Preloading bar, Key Object
        infoLayer.setDisplayPreferencesValue(LayerAnnotation.PIXEL, false);
        infoLayer.setDisplayPreferencesValue(LayerAnnotation.PRELOADING_BAR, false);

        // Copy image operations from view2d
        SimpleOpManager operations = imageLayer.getDisplayOpManager();
        for (ImageOpNode op : view2d.getImageLayer().getDisplayOpManager().getOperations()) {
            operations.addImageOperationAction(op.copy());
        }
        // Copy the current values of image operations
        view2d.copyActionWState(actionsInView);

        setPreferredSize(new Dimension(1024, 1024));
        ViewModel model = view2d.getViewModel();
        Rectangle2D canvas = new Rectangle2D.Double(view2d.modelToViewLength(model.getModelOffsetX()),
            view2d.modelToViewLength(model.getModelOffsetY()), view2d.getJComponent().getWidth(),
            view2d.getJComponent().getHeight());
        Rectangle2D mArea = view2d.getViewModel().getModelArea();
        Rectangle2D viewFullImg = new Rectangle2D.Double(0, 0, view2d.modelToViewLength(mArea.getWidth()),
            view2d.modelToViewLength(mArea.getHeight()));
        Rectangle2D.intersect(canvas, viewFullImg, viewFullImg);
        actionsInView.put("origin.image.bound", viewFullImg); //$NON-NLS-1$
        actionsInView.put("origin.zoom", view2d.getActionValue(ActionW.ZOOM.cmd())); //$NON-NLS-1$
        Point2D p = new Point2D.Double(
            view2d.viewToModelX(viewFullImg.getX() - canvas.getX() + (viewFullImg.getWidth() - 1) * 0.5),
            view2d.viewToModelY(viewFullImg.getY() - canvas.getY() + (viewFullImg.getHeight() - 1) * 0.5));
        actionsInView.put("origin.center", p); //$NON-NLS-1$
        // Do not use setSeries() because the view will be reset
        this.series = view2d.getSeries();
        setImage(view2d.getImage());
    }

    public double getImagePrintingResolution() {
        return imagePrintingResolution;
    }

    public void setImagePrintingResolution(double imagePrintingResolution) {
        this.imagePrintingResolution = imagePrintingResolution;
    }

    @Override
    public void disposeView() {
        disableMouseAndKeyListener();
        removeFocusListener(this);
        ToolTipManager.sharedInstance().unregisterComponent(this);
        imageLayer.removeLayerChangeListener(this);
        // Unregister listener in GraphicsPane
        graphicManager.removeChangeListener(layerModelHandler);
        graphicManager.removeGraphicChangeHandler(graphicsChangeHandler);
        setViewModel(null);
    }

    @Override
    public Graphics getGraphics() {
        if (currentG2d != null) {
            return currentG2d;
        }
        return super.getGraphics();
    }

    @Override
    public void paintComponent(Graphics g) {
        if (g instanceof Graphics2D) {
            draw((Graphics2D) g);
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        currentG2d = g2d;
        Stroke oldStroke = g2d.getStroke();
        Paint oldColor = g2d.getPaint();
        double viewScale = getViewModel().getViewScale();
        double offsetX = getViewModel().getModelOffsetX() * viewScale;
        double offsetY = getViewModel().getModelOffsetY() * viewScale;
        // Paint the visible area
        g2d.translate(-offsetX, -offsetY);
        // Set font size according to the view size
        g2d.setFont(getLayerFont());
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Set label box size and spaces between items

        graphicManager.updateLabels(Boolean.TRUE, this);

        if (g2d.getClass().getName().contains("print")) { //$NON-NLS-1$
            imageLayer.drawImageForPrinter(g2d, imagePrintingResolution);
        } else {
            imageLayer.drawImage(g2d);
        }

        drawLayers(g2d, affineTransform, inverseTransform);
        g2d.translate(offsetX, offsetY);
        if (infoLayer != null) {
            infoLayer.paint(g2d);
        }
        g2d.setPaint(oldColor);
        g2d.setStroke(oldStroke);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);

        // Reset label box size and spaces between items
        graphicManager.updateLabels(Boolean.TRUE, view2d);

        currentG2d = null;
    }

    @Override
    public void handleLayerChanged(ImageLayer<E> layer) {
        // Do nothing
    }

    @Override
    public void enableMouseAndKeyListener(MouseActions mouseActions) {
        // Do nothing
    }
}
