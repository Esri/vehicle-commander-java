package com.esri.vehiclecommander;

/**
 * A listener for certain changes to the application configuration.
 */
public interface AppConfigListener {

    /**
     * Called when the "decorated" preference changes. A decorated application is
     * one with a title bar that can be moved, resized, maximized, etc.
     * @param isDecorated true if the preference was changed to decorated, or false
     *                    if the preference was changed to undecorated.
     */
    public void decoratedChanged(boolean isDecorated);

}
