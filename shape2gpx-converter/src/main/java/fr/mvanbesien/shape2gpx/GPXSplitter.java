package fr.mvanbesien.shape2gpx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

public class GPXSplitter {

	public static void main(String[] args) throws Exception {
		File inputFile = new File("C:/Users/Maxence/Desktop/Shapes/communes-20150101-100m-shp/output.xml");
		File outputDir = new File("C:/Users/Maxence/Desktop/Shapes/communes-20150101-100m-shp/output");
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		Map<String, Element> outputFiles = new HashMap<>();
		FileInputStream fileInputStream = new FileInputStream(inputFile);
		InputSource is = new InputSource(fileInputStream);
		Element rootElement = new SAXBuilder().build(is).getRootElement();

		for (Object o1 : rootElement.getChildren("trk")) {
			Element trk = (Element) o1;
			trk.removeAttribute("wikipedia");
			trk.removeAttribute("surf_m2");
			Set<String> zones = new HashSet<>();
			for (Object o2 : trk.getChildren("trkseg")) {
				Element trkseg = (Element) o2;
				for (Object o3 : trkseg.getChildren("trkpt")) {
					Element trkpt = (Element) o3;
					Float lon = Float.parseFloat(trkpt.getAttributeValue("lon"));
					Float lat = Float.parseFloat(trkpt.getAttributeValue("lat"));
					zones.add(computeZoneIdentifier(lon, lat));
				}
			}
			for (String zone : zones) {
				Element element = outputFiles.get(zone);
				if (element == null) {
					element = new Element("gpx");
					new Document().addContent(element);
					outputFiles.put(zone, element);
				}
				element.addContent((Element) trk.clone());
			}
		}
		for (Entry<String, Element> pair : outputFiles.entrySet()) {
			String fileName = outputDir.getPath() + "/" + pair.getKey() + ".gpx";
			FileOutputStream fileOutputStream = new FileOutputStream(new File(fileName));
			Format prettyFormat = Format.getPrettyFormat();
			prettyFormat.setEncoding("ISO-8859-1");
			new XMLOutputter(prettyFormat).output(pair.getValue().getDocument(), fileOutputStream);
			fileOutputStream.close();
		}
	}

	private static String computeZoneIdentifier(float longitude, float latitude) {
		int longitudeZoneId = (int) ((longitude + 180) * 3);
		int latitudeZoneId = (int) ((latitude + 90) * 3);
		return String.format("z%04d%04d", (int) longitudeZoneId, latitudeZoneId);
	}

}
