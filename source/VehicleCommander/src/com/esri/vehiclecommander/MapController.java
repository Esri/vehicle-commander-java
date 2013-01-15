package com.esri.vehiclecommander;

import com.esri.client.local.ArcGISLocalDynamicMapServiceLayer;
import com.esri.client.local.LayerDetails;
import com.esri.client.local.LocalFeatureService;
import com.esri.client.local.LocalServer;
import com.esri.client.local.LocalServerStatus;
import com.esri.client.local.LocalServiceStartCompleteEvent;
import com.esri.client.local.LocalServiceStartCompleteListener;
import com.esri.client.local.ServerLifetimeEvent;
import com.esri.client.local.ServerLifetimeListener;
import com.esri.core.geometry.MgrsConversionMode;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.tasks.ags.identify.IdentifyParameters;
import com.esri.core.tasks.ags.identify.IdentifyResult;
import com.esri.core.tasks.ags.identify.IdentifyTask;
import com.esri.map.ArcGISDynamicMapServiceLayer;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.Grid;
import com.esri.map.JMap;
import com.esri.map.Layer;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.MapOverlay;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class for interacting with the JMap map control, for convenience and abstraction.
 * UI code should not go in the MapController class.
 */
public class MapController {

    /**
     * Directions used when panning with MapController.pan(PanDirection).
     */
    public enum PanDirection {
        UP, DOWN, LEFT, RIGHT
    }
    
    /**
     * Constant representing a mouse click event, to be used with the trackAsync
     * method.
     * @see #trackAsync(com.esri.vehiclecommander.MapOverlayListener, short)
     */
    public static final short EVENT_MOUSE_CLICKED = 1;

    /**
     * Constant representing a mouse drag event, to be used with the trackAsync
     * method.
     * @see #trackAsync(com.esri.vehiclecommander.MapOverlayListener, short)
     */
    public static final short EVENT_MOUSE_DRAGGED = 2;

    /**
     * Constant representing a mouse press event, to be used with the trackAsync
     * method.
     * @see #trackAsync(com.esri.vehiclecommander.MapOverlayListener, short)
     */
    public static final short EVENT_MOUSE_PRESSED = 4;

    /**
     * Constant representing a mouse drag event, to be used with the trackAsync
     * method.
     * @see #trackAsync(com.esri.vehiclecommander.MapOverlayListener, short)
     */
    public static final short EVENT_MOUSE_RELEASED = 8;

    private final JMap map;
    private final List<Layer> overlayLayers = new ArrayList<Layer>();
    private final List<MapControllerListener> listeners = new ArrayList<MapControllerListener>();
    private final HashSet<CallbackListener<IdentifyResult[]>> identifyListeners = new HashSet<CallbackListener<IdentifyResult[]>>();
    private final ArrayList<IdentifiedItem> allResults = new ArrayList<IdentifiedItem>();
    /**
     * Maps each result to the layer from which it came.
     */
    private final Map<IdentifiedItem, Layer> resultToLayer = new HashMap<IdentifiedItem, Layer>();
    /**
     * Maps layers--probably map service layers--to corresponding feature layers,
     * where individual feature layers are identified by their IDs.
     */
    private final Map<Layer, Map<Integer, ArcGISFeatureLayer>> layerToFeatureLayer = new HashMap<Layer, Map<Integer, ArcGISFeatureLayer>>();
    private final IdentifyListener identifyListener;
    
    private AdvancedSymbolController symbolController;
    private MapOverlay trackOverlay = null;

    /**
     * Constructs a MapController with a JMap
     * @param map The JMap to be controlled by this MapController
     * @param identifyListener the application's identify listener.
     */
    public MapController(
            JMap map,
            IdentifyListener identifyListener,
            AppConfigController appConfig) {
        map.addMapEventListener(new MapEventListenerAdapter() {

            @Override
            public void mapReady(MapEvent event) {
                fireMapReady();
            }

        });
        this.map = map;
        setGridVisible(appConfig.isShowMgrsGrid());
        setGridType(Grid.GridType.MGRS);
        this.identifyListener = identifyListener;
        activateIdentify();
    }

    /**
     * Adds a MapControllerListener to this MapController.
     * @param listener the listener to add.
     */
    public void addListener(MapControllerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a MapControllerListener from this MapController. This method has
     * no effect if this MapController does not have a reference to the specified
     * listener.
     * @param listener the listener to remove.
     */
    public void removeListener(MapControllerListener listener) {
        listeners.remove(listener);
    }

    private void fireLayersChanged(final boolean isOverlay) {
        for (final MapControllerListener listener : listeners) {
            listener.layersChanged(isOverlay);
        }
    }

    private void fireMapReady() {
        for (MapControllerListener listener : listeners) {
            listener.mapReady();
        }
    }

    /**
     * Zooms the map in on the current center point.
     */
    public void zoomIn() {
        map.zoom(0.5);
    }

    /**
     * Zooms the map out, focused on the current center point.
     */
    public void zoomOut() {
        map.zoom(2);
    }

    /**
     * Adds a number of degrees to the map's current rotation.
     * @param degrees the number of degrees to add to the map's current rotation.
     */
    public void rotate(double degrees) {
        double rotation = map.getRotation() + degrees;
        rotation = Utilities.fixAngleDegrees(rotation, -180, 180);
        map.setRotation(rotation);
    }

    /**
     * Sets the map's rotation, in degrees.
     * @param degrees the new map rotation.
     */
    public void setRotation(double degrees) {
        map.setRotation(Utilities.fixAngleDegrees(degrees, -180, 180));
    }

    /**
     * Gets the map's rotation, in degrees.
     */
    public double getRotation() {
        return map.getRotation();
    }

    /**
     * Adds a layer to the map.
     * @param layer the layer to add.
     * @param isOverlay true if the layer is an overlay that can be turned on and
     *                  off, and false otherwise.
     */
    public void addLayer(Layer layer, boolean isOverlay) {
        synchronized (map) {
            if (0 == map.getLayers().size()) {
                addLayer(0, layer, isOverlay);
            } else {
                //Don't add it on top of graphics layers at the top
                for (int i = map.getLayers().size() - 1; i >= 0; i--) {
                    if (!(map.getLayers().get(i) instanceof GraphicsLayer)) {
                        addLayer(i + 1, layer, isOverlay);
                        i = 0;
                    }
                }
            }
        }
        fireLayersChanged(isOverlay);
    }

    /**
     * Adds a layer to the map at a certain index in the layer list.
     * @param layerIndex the index in the layer list.
     * @param layer the layer to add.
     * @param isOverlay true if the layer is an overlay that can be turned on and
     *                  off, and false otherwise.
     */
    public void addLayer(final int layerIndex, Layer layer, boolean isOverlay) {
        map.getLayers().add(layerIndex, layer);
        if (isOverlay) {
            for (int mapIndex = 0, listIndex = 0; mapIndex < map.getLayers().size(); mapIndex++) {
                if (layerIndex == mapIndex || overlayLayers.size() <= listIndex) {
                    overlayLayers.add(listIndex, layer);
                    mapIndex = map.getLayers().size();
                } else {
                    if (overlayLayers.get(listIndex) == map.getLayers().get(mapIndex)) {
                        listIndex++;
                    }
                }
            }
        }
        fireLayersChanged(isOverlay);
    }

    /**
     * Adds a Collection of layers to the map.
     * @param layers the Collection of layers to add.
     * @param isOverlay true if the layers are overlays that can be turned on and
     *                  off, and false otherwise.
     */
    public void addLayers(Collection<Layer> layers, boolean isOverlay) {
        synchronized (map) {
            map.getLayers().addAll(layers);
        }
        if (isOverlay) {
            overlayLayers.addAll(layers);
        }
        fireLayersChanged(isOverlay);
    }
    
    /**
     * Moves the layer to a new location in the layer list. If the layer is not already
     * in the map, nothing happens.
     * @param layer the layer to move.
     * @param newIndex the new index of the layer. This index will be adjusted to
     *                 be at least 0 (bottom layer) and at most the number of layers
     *                 (top layer), so you could pass Integer.MAX_VALUE to move
     *                 the layer to the top of the map.
     */
    public void moveLayer(Layer layer, int newIndex) {
        if (newIndex < 0) {
            newIndex = 0;
        }
        synchronized (map) {
            LayerList layers = map.getLayers();
            if (layers.remove(layer)) {
                if (newIndex > layers.size()) {
                    newIndex = layers.size();
                }
                layers.add(newIndex, layer);
            }
        }
    }

    /**
     * Removes a layer from the map if it is present.
     * @param layer the layer to remove.
     * @return the removed layer's former index, or -1 if the layer was not present.
     */
    public int removeLayer(Layer layer) {
        int layerIndex = -1;
        synchronized (map) {
            layerIndex = map.getLayers().indexOf(layer);
            map.getLayers().remove(layer);
        }
        boolean isOverlay = overlayLayers.remove(layer);
        fireLayersChanged(isOverlay);
        layerToFeatureLayer.remove(layer);
        
        return layerIndex;
    }

    /**
     * Removes all layers from the map.
     */
    public void removeAllLayers() {
        synchronized (map) {
            map.getLayers().clear();
        }
        overlayLayers.clear();
        resultToLayer.clear();
        layerToFeatureLayer.clear();
    }

    /**
     * Returns a list of overlay layers in the map.
     * @return a list of overlay layers in the map.
     */
    public List<Layer> getOverlayLayers() {
        return overlayLayers;
    }

    /**
     * Returns true if the map contains the specified layer and false otherwise.
     * @param layer the layer to check.
     * @return true if the map contains the specified layer and false otherwise.
     */
    public boolean hasLayer(Layer layer) {
        return map.getLayers().contains(layer);
    }

    /**
     * Zooms the map to the given center point and scale.
     * @param scale the new map scale, expressed as the denominator of the actual scale.
     *              For example, if you pass 250000 as the scale, the new map scale
     *              will be 1:250,000.
     * @param centerPoint The new center point of the map, in the map's spatial reference
     */
    public void zoomToScale(final double scale, final Point centerPoint) {
        if (map.isReady()) {
            map.zoomToScale(scale, centerPoint);
        } else {
            map.addMapEventListener(new MapEventListenerAdapter() {

                @Override
                public void mapReady(MapEvent event) {
                    map.zoomToScale(scale, centerPoint);
                    map.removeMapEventListener(this);
                }

            });
        }
    }

    /**
     * Pans the map in the specified direction.
     * @param direction the direction in which to pan the map.
     */
    public void pan(PanDirection direction) {
        double diff;
        int newScreenX = map.getWidth() / 2;
        int newScreenY = map.getHeight() / 2;
        switch (direction) {
            case UP:
            case DOWN: {
                diff = .25 * map.getHeight();
                switch (direction) {
                    case UP: {
                        newScreenY -= diff;
                        break;
                    }

                    case DOWN: {
                        newScreenY += diff;
                        break;
                    }
                }
                break;
            }

            case LEFT:
            case RIGHT: {
                diff = .25 * map.getWidth();
                switch (direction) {
                    case LEFT: {
                        newScreenX -= diff;
                        break;
                    }

                    case RIGHT: {
                        newScreenX += diff;
                        break;
                    }
                }
                break;
            }
        }
        map.panTo(map.toMapPoint(newScreenX, newScreenY));
    }

    /**
     * Pans the map to a new center point.
     * @param newCenter the map's new center point.
     */
    public void panTo(Point newCenter) {
        map.panTo(newCenter);
    }
    
    /**
     * Pans the map to a new center point, if a valid MGRS string is provided.
     * @param newCenterMgrs the map's new center point, as an MGRS string.
     * @return if the string was valid, the point to which the map was panned; null otherwise
     */
    public Point panTo(String newCenterMgrs) {
        newCenterMgrs = Utilities.convertToValidMgrs(newCenterMgrs, map.getExtent().getCenter(),
                map.getSpatialReference());
        if (null != newCenterMgrs) {
            Point pt = fromMilitaryGrid(new String[] {newCenterMgrs})[0];
            if (null != pt) {
                panTo(pt);
                return pt;
            } else {
                Logger.getLogger(getClass().getName()).log(Level.WARNING,
                        "MGRS string ''{0}'' could not be converted to a point", newCenterMgrs);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Track MapOverlay events, most often used to allow the user to interact with
     * the map control and get the user's mouse events. The provided MapOverlayListener
     * receives an event for each MapOverlay event specified in the mask. To stop
     * tracking points, call cancelTrackAsync.
     * @param listener the listener that receives click events.
     * @param eventsMask A bitmask equal to the sum of EVENT_ constants corresponding
     *                   to the events you want to track. For example, to track just
     *                   mouse clicks, pass EVENT_MOUSE_CLICKED. To track mouse
     *                   clicks and mouse drags, pass EVENT_MOUSE_CLICKED + EVENT_MOUSE_DRAGGED.
     */
    public void trackAsync(final MapOverlayListener listener, final short eventsMask) {
        if (null != trackOverlay) {
            map.removeMapOverlay(trackOverlay);
        }
        trackOverlay = new MapOverlay() {

            private static final long serialVersionUID = 7396150864102685407L;

            @Override
            public void onMouseClicked(MouseEvent event) {
                if (0 != (eventsMask & EVENT_MOUSE_CLICKED)) {
                    listener.mouseClicked(event);
                } else {
                    super.onMouseClicked(event);
                }
            }

            @Override
            public void onMouseDragged(MouseEvent event) {
                if (0 != (eventsMask & EVENT_MOUSE_DRAGGED)) {
                    listener.mouseDragged(event);
                } else {
                    super.onMouseDragged(event);
                }
            }

            @Override
            public void onMousePressed(MouseEvent event) {
                if (0 != (eventsMask & EVENT_MOUSE_PRESSED)) {
                    listener.mousePressed(event);
                } else {
                    super.onMousePressed(event);
                }
            }

            @Override
            public void onMouseReleased(MouseEvent event) {
                if (0 != (eventsMask & EVENT_MOUSE_RELEASED)) {
                    listener.mouseReleased(event);
                } else {
                    super.onMouseReleased(event);
                }
            }

        };
        map.addMapOverlay(trackOverlay);
        trackOverlay.setActive(true);
    }

    /**
     * Stops tracking MapOverlay events. Call trackAsync first, and when you no
     * longer need events, call this method.
     */
    public void cancelTrackAsync() {
        if (null != trackOverlay) {
            trackOverlay.setActive(false);
            map.removeMapOverlay(trackOverlay);
        }
        activateIdentify();
    }

    private void activateIdentify() {
        trackAsync(new MapOverlayAdapter() {

            @Override
            public void mouseClicked(MouseEvent event) {
                synchronized (identifyListeners) {
                    identifyListeners.removeAll(identifyListeners);
                }
                synchronized (allResults) {
                    allResults.clear();
                    resultToLayer.clear();
                }
                final Point mapPoint = toMapPoint(event.getX(), event.getY());
                IdentifyResultList results = symbolController.identify(event.getX(), event.getY(), 5);
                for (int i = 0; i < results.size(); i++) {
                    IdentifiedItem result = results.get(i);
                    synchronized (allResults) {
                        allResults.add(result);
                        resultToLayer.put(result, results.getLayer(result));
                    }
                }
                
                List<Layer> overlayLayers = getOverlayLayers();
                for (final Layer layer : overlayLayers) {
                    if (null != layer.getUrl()) {
                        IdentifyTask task = new IdentifyTask(layer.getUrl());
                        IdentifyParameters params = new IdentifyParameters();
                        params.setSpatialReference(map.getSpatialReference());
                        params.setGeometry(mapPoint);
                        params.setMapExtent(map.getExtent());
                        params.setMapWidth(map.getWidth());
                        params.setMapHeight(map.getHeight());
                        //TODO there might be a better way to get the DPI
                        params.setDPI(96);
                        CallbackListener<IdentifyResult[]> identifyListener = new CallbackListener<IdentifyResult[]>() {

                            public void onCallback(IdentifyResult[] results) {
                                synchronized (identifyListeners) {
                                    if (identifyListeners.contains(this)) {
                                        synchronized (results) {
                                            for (IdentifyResult result : results) {
                                                /**
                                                 * Feature results have a non-null value, and
                                                 * raster results have a null value. For now,
                                                 * only allow feature results by checking for
                                                 * null.
                                                 */
                                                if (null != result.getValue()) {
                                                    IdentifiedItem item = new IdentifiedItem(
                                                            result.getGeometry(),
                                                            result.getLayerId(),
                                                            result.getAttributes(),
                                                            result.getValue());
                                                    allResults.add(item);
                                                    resultToLayer.put(item, layer);
                                                }
                                            }
                                        }

                                        removeListenerAndCheckResult();
                                    }
                                }
                            }

                            public void onError(Throwable e) {
                                Logger.getLogger(MapController.class.getName()).log(Level.WARNING, null, e);
                                removeListenerAndCheckResult();
                            }

                            private void removeListenerAndCheckResult() {
                                synchronized (identifyListeners) {
                                    identifyListeners.remove(this);
                                    if (identifyListeners.isEmpty()) {
                                        fireIdentifyComplete(mapPoint);
                                    }
                                }
                            }

                        };
                        synchronized (identifyListeners) {
                            identifyListeners.add(identifyListener);
                            task.executeAsync(params, identifyListener);
                        }
                    }
                }
                synchronized (identifyListeners) {
                    if (identifyListeners.isEmpty()) {
                        fireIdentifyComplete(mapPoint);
                    }
                }
            }

        }, EVENT_MOUSE_CLICKED);
    }
    
    private void fireIdentifyComplete(Point mapPoint) {
        identifyListener.identifyComplete(
                mapPoint,
                allResults.toArray(new IdentifiedItem[allResults.size()]),
                resultToLayer
                );
    }

    /**
     * Converts the screen coordinates to map coordinates.
     * @param screenX the screen X coordinate in pixels. The upper left corner of
     *                the JMap component is 0,0.
     * @param screenY the screen Y coordinate in pixels. The upper left corner of
     *                the JMap component is 0,0.
     * @return the point in map coordinates.
     */
    public Point toMapPoint(int screenX, int screenY) {
        return map.toMapPoint(screenX, screenY);
    }

    /**
     * Converts an array of map points to MGRS strings.
     * @param points the points to convert to MGRS strings.
     * @return an array of MGRS strings corresponding to the input points.
     */
    public String[] toMilitaryGrid(Point[] points) {
        SpatialReference sr = map.getSpatialReference();
        if (null == sr) {
            //Assume Web Mercator (3857)
            sr = SpatialReference.create(3857);
        }

        return sr.toMilitaryGrid(MgrsConversionMode.mgrsAutomatic, 5, false, true, points);
    }
    
    /**
     * Converts an array of MGRS points to map points.
     * @param mgrsStrings the MGRS strings to convert to map points.
     * @return an array of map points in the coordinate system of the map.
     */
    public Point[] fromMilitaryGrid(String[] mgrsStrings) {
        SpatialReference sr = map.getSpatialReference();
        if (null == sr) {
            //Assume Web Mercator (3857)
            sr = SpatialReference.create(3857);
        }
        return sr.fromMilitaryGrid(mgrsStrings, MgrsConversionMode.mgrsAutomatic);
    }

    /**
     * Returns the map's spatial reference.
     * @return the map's spatial reference.
     */
    public SpatialReference getSpatialReference() {
        return map.getSpatialReference();
    }

    /**
     * Opens a map package and adds it to the map. The map package should be a .mpk
     * file that was created in ArcGIS 10.1 or newer and was enabled for ArcGIS
     * Runtime.
     * @param mpkFile the .mpk file to open and add to the map as a layer.
     */
    public void openMapPackage(final File mpkFile) {
        final LocalServer localServer = LocalServer.getInstance();
        startLocalServer(new ServerLifetimeListener() {

            public void serverLifetimeInitialized(ServerLifetimeEvent e) {
                if (LocalServerStatus.INITIALIZED == localServer.getStatus()) {                    
                    //A dummy layer to display "Loading..." in the TOC
                    final GraphicsLayer dummyLayer = new GraphicsLayer();
                    
                    //The feature service that will provide the real map layer and a feature layer
                    final LocalFeatureService featureService = new LocalFeatureService(mpkFile.getAbsolutePath());
                    featureService.addLocalServiceStartCompleteListener(new LocalServiceStartCompleteListener() {

                        public void localServiceStartComplete(LocalServiceStartCompleteEvent e) {
                            //Create map layer and add to the map.
                            ArcGISDynamicMapServiceLayer mapLayer;
                            if (null == featureService.getUrlMapService()) {
                                mapLayer = new ArcGISLocalDynamicMapServiceLayer(mpkFile.getAbsolutePath());
                            } else {
                                mapLayer = new ArcGISDynamicMapServiceLayer(featureService.getUrlMapService());
                            }
                            addLayer(mapLayer, true);
                            removeLayer(dummyLayer);
                            
                            //Create corresponding feature layer, but don't add it to the map.
                            for (LayerDetails featureLayerDetails : featureService.getFeatureLayers()) {
                                ArcGISFeatureLayer featureLayer = new ArcGISFeatureLayer(featureLayerDetails.getUrl());
                                featureLayer.initializeAsync();
                                saveFeatureLayer(mapLayer, featureLayerDetails.getId(), featureLayer);
                            }
                        }
                    });
                    
                    //Add the dummy layer to get "Loading..." into the TOC
                    dummyLayer.setName("Loading...");
                    addLayer(dummyLayer, true);
                    
                    //Start the feature service, which will call the listener above
                    featureService.startAsync();
                }
            }

            public void serverLifetimeShutdown(ServerLifetimeEvent e) {

            }
        });
    }
    
    /**
     * Maps a layer to a corresponding feature layer, which will enable attachments
     * to be retrieved for the layer. A layer-featureLayerId pair uniquely identifies
     * a feature layer.
     * @param layer the layer whose attachments need to be retrieved.
     * @param featureLayerId the feature layer's ID.
     * @param featureLayer the feature layer that has the attachments.
     */
    public void saveFeatureLayer(Layer layer, int featureLayerId, ArcGISFeatureLayer featureLayer) {
        Map<Integer, ArcGISFeatureLayer> map = layerToFeatureLayer.get(layer);
        if (null == map) {
            map = new HashMap<Integer, ArcGISFeatureLayer>();
            layerToFeatureLayer.put(layer, map);
        }
        map.put(featureLayerId, featureLayer);
    }
    
    /**
     * Returns the feature layer associated with the specified layer (if any). For
     * example, a map layer may have an associated feature layer, and the feature
     * layer can be used to get attachments for identified features.
     * @param layer the layer whose associated feature layer is to be returned.
     * @para featureLayerId the ID of the feature layer to return.
     * @return the feature layer associated with the specified layer (if any).
     */
    public ArcGISFeatureLayer getFeatureLayer(Layer layer, int featureLayerId) {
        Map<Integer, ArcGISFeatureLayer> featureLayers = layerToFeatureLayer.get(layer);
        if (null == featureLayers) {
            return null;
        } else {
            return featureLayers.get(featureLayerId);
        }
    }

    private void startLocalServer(ServerLifetimeListener... listeners) {
        final LocalServer localServer = LocalServer.getInstance();

        for (ServerLifetimeListener listener : listeners) {
            localServer.addServerLifetimeListener(listener);
        }
        if (localServer.isInitialized()) {
            for (ServerLifetimeListener listener : listeners) {
                listener.serverLifetimeInitialized(new ServerLifetimeEvent(localServer, ServerLifetimeEvent.SERVERLIFETIME_INITIALIZED));
            }
        } else {
            localServer.initializeAsync();
        }
    }

    /**
     * Sets the map's animation duration in seconds.
     * @param seconds the map's animation duration in seconds.
     */
    public void setAnimationDuration(float seconds) {
        map.setAnimationDuration(seconds);
    }

    /**
     * Returns the map's animation duration in seconds.
     * @return the map's animation duration in seconds.
     */
    public float getAnimationDuration() {
        return map.getAnimationDuration();
    }
    
    /**
     * Sets the type of grid to display on the map.
     * @param gridType the type of grid to display on the map.
     */
    public final void setGridType(Grid.GridType gridType) {
        map.getGrid().setType(gridType);
    }
    
    /**
     * Gets the type of grid to display on the map.
     * @return the type of grid to display on the map.
     */
    public Grid.GridType getGridType() {
        return map.getGrid().getType();
    }
    
    /**
     * Sets the map's grid to be visible or invisible. Note that you must also call setGridType
     * with a value other than None in order to make the grid visible.
     * @param visible true to make the grid visible and false to make the grid invisible.
     */
    public final void setGridVisible(boolean visible) {
        map.getGrid().setVisibility(visible);
    }
    
    /**
     * Gets the visibility of the map's grid. The grid will not actually display
     * if getGridType returns None.
     * @return the visibility of the map's grid.
     */
    public boolean isGridVisible() {
        return map.getGrid().getVisibility();
    }
    
    /**
     * Enables or disables keyboard navigation on the map. Keyboard navigation includes
     * using the keyboard's arrow keys to pan the map.
     * @param enabled true to enable keyboard navigation, and false to disable it.
     */
    public void setKeyboardEnabled(boolean enabled) {
        map.setKeyboardEnabled(enabled);
    }
    
    public void setAdvancedSymbolController(AdvancedSymbolController symbolController) {
        this.symbolController = symbolController;
    }
    
    public JMap getMap() {
        return map;
    }

}
