package fr.mvanbesien.shape2gpx;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Shape2GPXConverter {

	public static void main(String[] args) throws Exception {

		Document document = new Document();
		Element gpx = new Element("gpx");
		document.addContent(gpx);
		if (args.length < 1) {
			System.out.println("There should be at least one parameter...");
			System.out.println("\t-fileName: path to the file to split into gpx files.");
			return;
		}

		File file = new File(args[0]);
		if (!file.exists()) {
			System.out.println("shape file does not exist. Returning.");
			return;
		}
		Map<String, Object> map = new HashMap<>();
		map.put("url", file.toURI().toURL());

		DataStore dataStore = DataStoreFinder.getDataStore(map);
		String typeName = dataStore.getTypeNames()[0];

		FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
		Filter filter = Filter.INCLUDE;

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
		try (FeatureIterator<SimpleFeature> features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				Object thegeom = feature.getDefaultGeometry();

				if (thegeom != null) {
					Collection<Property> properties = feature.getProperties();
					Element trk = new Element("trk");
					for (Property property : properties) {
						if (property.getValue() != thegeom) {
							trk.setAttribute(property.getName().toString(),
									property.getValue() != null ? property.getValue().toString() : "");
						}
					}
					if (thegeom instanceof MultiPolygon) {
						MultiPolygon multiPolygon = (MultiPolygon) thegeom;
						for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
							Element trkseg = new Element("trkseg");
							trk.addContent(trkseg);
							for (Coordinate coordinate : multiPolygon.getGeometryN(i).getCoordinates()) {
								Element trkpt = new Element("trkpt");
								trkseg.addContent(trkpt);
								trkpt.setAttribute("lon", String.valueOf(coordinate.x));
								trkpt.setAttribute("lat", String.valueOf(coordinate.y));
							}
						}
					}
					gpx.addContent(trk);
				}
			}

			File outputFile = new File(file.getParentFile().getPath() + "/output.xml");
			outputFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(outputFile);
			Format prettyFormat = Format.getPrettyFormat();
			prettyFormat.setEncoding("ISO-8859-1");
			new XMLOutputter(prettyFormat).output(document, fos);
			fos.close();
			System.out.println("Written in " + outputFile.getPath());

			Element item = new Element("file");
			item.setAttribute("path", "/" + outputFile.getName());

			features.close();
		}
	}


}
