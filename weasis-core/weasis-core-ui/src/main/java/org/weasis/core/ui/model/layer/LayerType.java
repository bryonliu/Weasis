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
package org.weasis.core.ui.model.layer;

import org.weasis.core.ui.Messages;

public enum LayerType {
    IMAGE(10, Messages.getString("LayerType.img"), Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE), //$NON-NLS-1$

    CROSSLINES(20, Messages.getString("LayerType.crosslines"), Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE), //$NON-NLS-1$

    IMAGE_ANNOTATION(10_000, Messages.getString("LayerType.img_anont"), Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE), //$NON-NLS-1$

    ANNOTATION(31, Messages.getString("LayerType.annot"), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE), //$NON-NLS-1$

    DRAW(40, Messages.getString("LayerType.drawings"), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE), //$NON-NLS-1$

    MEASURE(50, Messages.getString("LayerType.measurements"), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE), //$NON-NLS-1$

    TEMP_DRAW(60, "Drawings [Temp]", Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE), //$NON-NLS-1$

    ACQUIRE(70, "Dicomizer [Temp]", Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE), //$NON-NLS-1$

    BLOB(80, Messages.getString("LayerType.obj"), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), //$NON-NLS-1$

    POINTS(90, Messages.getString("LayerType.pts"), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), //$NON-NLS-1$

    DICOM_SR(100, "DICOM SR", Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE), //$NON-NLS-1$
    
    DICOM_PR(100, "DICOM PR", Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE); //$NON-NLS-1$

    private final Integer level;
    private final Boolean visible;
    private final Boolean locked;
    private final Boolean serializable;
    private final Boolean selectable;
    private final String defaultName;

    private LayerType(Integer level, String defaultName, Boolean visible, Boolean locked, Boolean serializable,
        Boolean selectable) {
        this.level = level;
        this.visible = visible;
        this.locked = locked;
        this.serializable = serializable;
        this.defaultName = defaultName;
        this.selectable = selectable;
    }

    public Integer getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return defaultName;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public Boolean getVisible() {
        return visible;
    }

    public Boolean getLocked() {
        return locked;
    }

    public Boolean getSerializable() {
        return serializable;
    }

    public Boolean getSelectable() {
        return selectable;
    }

}
