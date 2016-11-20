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
		
		if (args.length < 2) {
			System.out.println("There should be at least two parameters...");
			System.out.println("\t-fileName: path to the file to split into gpx files.");
			System.out.println("\t-id: column name corresponding to the name of the gpx files.");
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

		File outputDir = new File(file.getParentFile().getPath() + "/output/");
		outputDir.mkdirs();

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
		try (FeatureIterator<SimpleFeature> features = collection.features()) {
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				Object thegeom = feature.getDefaultGeometry();
				Object id = feature.getAttribute(args[1]);
				if (id != null && thegeom != null) {
					Document document = new Document();
					Element rootElement = new Element("gpx");
					Collection<Property> properties = feature.getProperties();
					for (Property property : properties) {
						if (property.getValue() != thegeom) {
							rootElement.setAttribute(property.getName().toString(), property.getValue().toString());
						}
					}
					document.addContent(rootElement);

					double minLon = 1000;
					double maxLon = -1000;
					double minLat = 1000;
					double maxLat = -1000;
					Element bounds = new Element("bounds");
					rootElement.addContent(bounds);

					Element trk = new Element("trk");
					rootElement.addContent(trk);

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
								if (coordinate.y > maxLat) {
									maxLat = coordinate.y;
								}
								if (coordinate.y < minLat) {
									minLat = coordinate.y;
								}
								if (coordinate.x > maxLon) {
									maxLon = coordinate.x;
								}
								if (coordinate.x < minLon) {
									minLon = coordinate.x;
								}
							}
						}
						bounds.setAttribute("minlat", String.valueOf(minLat));
						bounds.setAttribute("maxlat", String.valueOf(maxLat));
						bounds.setAttribute("minlon", String.valueOf(minLon));
						bounds.setAttribute("maxlon", String.valueOf(maxLon));
					}
					File arrFile = new File(outputDir.getPath() + "/" + id + ".gpx");
					arrFile.createNewFile();
					FileOutputStream fos = new FileOutputStream(arrFile);
					new XMLOutputter(Format.getPrettyFormat()).output(document, fos);
					fos.close();
					System.out.println("Written in " + arrFile.getPath());
				}

			}
			features.close();
		}
		
	}

}
