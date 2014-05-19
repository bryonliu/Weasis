package org.weasis.dicom.viewer2d;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.dcm4che3.data.Attributes;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.gui.util.ActionState;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.Filter;
import org.weasis.core.api.gui.util.JMVUtils;
import org.weasis.core.api.gui.util.SliderChangeListener;
import org.weasis.core.api.gui.util.SliderCineListener;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.ui.editor.image.DefaultView2d;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.codec.KOSpecialElement;
import org.weasis.dicom.codec.utils.DicomMediaUtils;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.LoadDicomObjects;

public final class KOManager {

    public static List<Object> getKOElementListWithNone(DefaultView2d<DicomImageElement> currentView) {

        Collection<KOSpecialElement> koElements =
            currentView != null ? DicomModel.getKoSpecialElements(currentView.getSeries()) : null;

        int koElementNb = (koElements == null) ? 0 : koElements.size();

        List<Object> koElementListWithNone = new ArrayList<Object>(koElementNb + 1);
        koElementListWithNone.add(ActionState.NONE);

        if (koElementNb > 0) {
            koElementListWithNone.addAll(koElements);
        }
        return koElementListWithNone;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Test if current sopInstanceUID is referenced in the selected KEY_OBJECT of the given currentView. If not, search
     * if there is a more suitable new KEY_OBJECT element. Ask the user if needed.
     */

    private static KOSpecialElement getValidKOSelection(final DefaultView2d<DicomImageElement> view2d) {

        KOSpecialElement currentSelectedKO = getCurrentKOSelection(view2d);
        DicomImageElement currentImage = view2d.getImage();

        KOSpecialElement newKOSelection = null;
        Attributes newDicomKO = null;

        if (currentSelectedKO == null) {

            KOSpecialElement validKOSelection = findValidKOSelection(view2d);

            if (validKOSelection != null) {

                String message = "No KeyObject is selected but at least one is available.\n";
                Object[] options = { "Switch to a valid KeyObject Selection", "Create a new one" };

                int response =
                    JOptionPane.showOptionDialog(view2d, message, "Key Object Selection", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                if (response == 0) {
                    newKOSelection = validKOSelection;
                } else if (response == 1) {
                    newDicomKO = createNewDicomKeyObject(currentImage, view2d);
                } else if (response == JOptionPane.CLOSED_OPTION) {
                    return null;
                }
            } else {
                newDicomKO = createNewDicomKeyObject(currentImage, view2d);
            }

        } else {
            if (currentSelectedKO.getMediaReader().isEditableDicom()) {

                String studyInstanceUID = (String) currentImage.getTagValue(TagW.StudyInstanceUID);

                if (currentSelectedKO.isEmpty()
                    || currentSelectedKO.containsStudyInstanceUIDReference(studyInstanceUID)) {

                    newKOSelection = currentSelectedKO;
                } else {

                    String message = "Be aware that selected KO doesn't have any reference on the current study.\n";
                    Object[] options = { "Use it anyway", "Create a new KeyObject" };

                    int response =
                        JOptionPane.showOptionDialog(view2d, message, "Key Object Selection",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                    if (response == 0) {
                        newKOSelection = currentSelectedKO;
                    } else if (response == 1) {
                        newDicomKO = createNewDicomKeyObject(currentImage, view2d);
                    } else if (response == JOptionPane.CLOSED_OPTION) {
                        return null;
                    }
                }

            } else {

                String message = "Be aware that selected KO is Read Only.\n";
                Object[] options = { "Create a new KeyObject from a copy", "Create a new KeyObject" };

                int response =
                    JOptionPane.showOptionDialog(view2d, message, "Key Object Selection", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                if (response == 0) {
                    newDicomKO = createNewDicomKeyObject(currentSelectedKO, view2d);
                } else if (response == 1) {
                    newDicomKO = createNewDicomKeyObject(currentImage, view2d);
                } else if (response == JOptionPane.CLOSED_OPTION) {
                    return null;
                }
            }
        }

        if (newDicomKO != null) {
            newKOSelection = loadDicomObject(view2d.getSeries(), newDicomKO);
        }

        return newKOSelection;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static KOSpecialElement loadDicomObject(MediaSeries<DicomImageElement> dicomSeries, Attributes newDicomKO) {

        DicomModel dicomModel = (DicomModel) dicomSeries.getTagValue(TagW.ExplorerModel);

        new LoadDicomObjects(dicomModel, newDicomKO).addSelectionAndnotify(); // must be executed in the EDT

        for (KOSpecialElement koElement : DicomModel.getKoSpecialElements(dicomSeries)) {
            if (koElement.getMediaReader().getDicomObject().equals(newDicomKO)) {
                return koElement;
            }
        }

        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Attributes createNewDicomKeyObject(MediaElement<?> dicomMediaElement, Component parentComponent) {

        if (dicomMediaElement == null || (dicomMediaElement.getMediaReader() instanceof DicomMediaIO) == false) {
            return null;
        }

        Attributes dicomSourceAttribute = ((DicomMediaIO) dicomMediaElement.getMediaReader()).getDicomObject();

        String message = "Set a description for the new KeyObject Selection";
        String defautDescription = "new KO selection";

        String description =
            (String) JOptionPane.showInputDialog(parentComponent, message, "Key Object Selection",
                JOptionPane.INFORMATION_MESSAGE, null, null, defautDescription);

        // description==null means the user canceled the input
        if (StringUtil.hasText(description)) {
            return DicomMediaUtils.createDicomKeyObject(dicomSourceAttribute, description, null);
        }

        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get an editable Dicom KeyObject Selection suitable to handle current Dicom Image. A valid object should either
     * reference the studyInstanceUID of the current Dicom Image or simply be empty ...
     */

    public static KOSpecialElement findValidKOSelection(final DefaultView2d<DicomImageElement> view2d) {

        MediaSeries<DicomImageElement> dicomSeries = view2d.getSeries();
        DicomImageElement currentImage = view2d.getImage();

        String currentStudyInstanceUID = (String) currentImage.getTagValue(TagW.StudyInstanceUID);
        Collection<KOSpecialElement> koElementsWithReferencedSeriesInstanceUID =
            DicomModel.getKoSpecialElements(dicomSeries);

        if (koElementsWithReferencedSeriesInstanceUID != null) {

            for (KOSpecialElement koElement : koElementsWithReferencedSeriesInstanceUID) {
                if (koElement.getMediaReader().isEditableDicom()) {
                    if (koElement.containsStudyInstanceUIDReference(currentStudyInstanceUID)) {
                        return koElement;
                    }
                }
            }

            for (KOSpecialElement koElement : koElementsWithReferencedSeriesInstanceUID) {
                if (koElement.getMediaReader().isEditableDicom()) {
                    if (koElement.isEmpty()) {
                        return koElement;
                    }
                }
            }
        }
        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static KOSpecialElement getCurrentKOSelection(final DefaultView2d<DicomImageElement> view2d) {

        Object actionValue = view2d.getActionValue(ActionW.KO_SELECTION.cmd());
        if (actionValue instanceof KOSpecialElement) {
            return (KOSpecialElement) actionValue;
        }

        return null;
    }

    public static Boolean getCurrentKOToogleState(final DefaultView2d<DicomImageElement> view2d) {

        Object actionValue = view2d.getActionValue(ActionW.KO_TOOGLE_STATE.cmd());
        if (actionValue instanceof Boolean) {
            return (Boolean) actionValue;
        }

        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean setKeyObjectReference(boolean selectedState, final DefaultView2d<DicomImageElement> view2d) {

        KOSpecialElement validKOSelection = getValidKOSelection(view2d);

        if (validKOSelection == null) {
            return false; // canceled
        }

        KOSpecialElement currentSelectedKO = KOManager.getCurrentKOSelection(view2d);
        boolean hasKeyObjectSelectionChanged = validKOSelection != currentSelectedKO;

        if (hasKeyObjectSelectionChanged) {
            if (view2d instanceof View2d) {
                // ((View2d) view2d).setKeyObjectSelection(validKOSelection);
                ((View2d) view2d).updateKOButtonVisibleState();
            }
        } else {
            DicomImageElement currentImage = view2d.getImage();
            boolean hasKeyObjectReferenceChanged = validKOSelection.setKeyObjectReference(selectedState, currentImage);

            if (hasKeyObjectReferenceChanged) {
                DicomModel dicomModel = (DicomModel) view2d.getSeries().getTagValue(TagW.ExplorerModel);
                // Fire an event since any view in any View2dContainner may have its KO selected state changed
                dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Update, view2d, null,
                    validKOSelection));
            }
        }

        return hasKeyObjectSelectionChanged;
    }

    public static void updateKOFilter(DefaultView2d<DicomImageElement> view2D, Object selectedKO, boolean onlyNewestKO,
        Boolean enableFilter) {
        if (view2D instanceof View2d) {
            Filter<DicomImageElement> sopInstanceUIDFilter = null;

            if (selectedKO == null) {
                selectedKO = view2D.getActionValue(ActionW.KO_SELECTION.cmd());
            } else {
                if (onlyNewestKO) {
                    Object lastKO = view2D.getActionValue(ActionW.KO_SELECTION.cmd());
                    if (lastKO instanceof KOSpecialElement && selectedKO instanceof KOSpecialElement) {
                        int val =
                            KOSpecialElement.ORDER_BY_DATE.compare((KOSpecialElement) lastKO,
                                (KOSpecialElement) selectedKO);
                        if (val < 0) {
                            return;
                        }
                    }
                }
                view2D.setActionsInView(ActionW.KO_SELECTION.cmd(), selectedKO);
            }
            if (enableFilter == null) {
                enableFilter = JMVUtils.getNULLtoFalse(view2D.getActionValue(ActionW.KO_FILTER.cmd()));
            } else {
                view2D.setActionsInView(ActionW.KO_FILTER.cmd(), enableFilter);
            }

            if (enableFilter) {
                sopInstanceUIDFilter =
                    (selectedKO instanceof KOSpecialElement) ? ((KOSpecialElement) selectedKO)
                        .getSOPInstanceUIDFilter() : null;
            }
            view2D.setActionsInView(ActionW.FILTERED_SERIES.cmd(), sopInstanceUIDFilter);

            DicomSeries dicomSeries = (DicomSeries) view2D.getSeries();
            DicomImageElement currentImg = view2D.getImage();

            if (currentImg != null && dicomSeries != null) {
                /*
                 * The getFrameIndex() returns a valid index for the current image displayed according to the current
                 * FILTERED_SERIES and the current SortComparator
                 */
                int newImageIndex = view2D.getFrameIndex();
                if (enableFilter && selectedKO instanceof KOSpecialElement) {
                    if (newImageIndex < 0) {

                        if (dicomSeries.size(sopInstanceUIDFilter) > 0) {
                            double[] val = (double[]) currentImg.getTagValue(TagW.SlicePosition);
                            double location = val[0] + val[1] + val[2];
                            Double offset = (Double) view2D.getActionValue(ActionW.STACK_OFFSET.cmd());
                            if (offset != null) {
                                location += offset;
                            }
                            newImageIndex =
                                dicomSeries.getNearestImageIndex(location, view2D.getTileOffset(),
                                    sopInstanceUIDFilter, view2D.getCurrentSortComparator());
                        } else {
                            // If there is no more image in KO series filtered then disable the KO_FILTER
                            sopInstanceUIDFilter = null;
                            view2D.setActionsInView(ActionW.KO_FILTER.cmd(), false);
                            view2D.setActionsInView(ActionW.FILTERED_SERIES.cmd(), sopInstanceUIDFilter);
                            newImageIndex = view2D.getFrameIndex();
                        }
                    }
                }

                DefaultView2d<DicomImageElement> selectedPane = view2D.getEventManager().getSelectedViewPane();
                if (selectedPane == view2D) {
                    /*
                     * Update the sliceAction action according to nearest image when the filter hides the image of the
                     * previous state.
                     */
                    ActionState seqAction = view2D.getEventManager().getAction(ActionW.SCROLL_SERIES);
                    if (seqAction instanceof SliderCineListener) {
                        ((SliderChangeListener) seqAction).setMinMaxValue(1, dicomSeries.size(sopInstanceUIDFilter),
                            newImageIndex + 1);
                    }
                } else {
                    DicomImageElement newImage =
                        dicomSeries.getMedia(newImageIndex, sopInstanceUIDFilter, view2D.getCurrentSortComparator());
                    if (newImage != null && !newImage.isImageAvailable()) {
                        newImage.getImage();
                    }
                    ((View2d) view2D).setImage(newImage);
                }
                ((View2d) view2D).updateKOButtonVisibleState();
            }
        }
    }

}
