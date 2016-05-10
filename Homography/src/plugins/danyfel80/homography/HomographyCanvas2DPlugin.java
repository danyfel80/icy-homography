package plugins.danyfel80.homography;

import icy.canvas.IcyCanvas;
import icy.gui.viewer.Viewer;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginCanvas;

public class HomographyCanvas2DPlugin extends Plugin implements PluginCanvas {

	@Override
	public String getCanvasClassName() {
		return HomographyCanvas2D.class.getName();
	}

	@Override
	public IcyCanvas createCanvas(Viewer viewer) {
		return new HomographyCanvas2D(viewer);
	}

}
