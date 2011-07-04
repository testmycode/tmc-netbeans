package fi.helsinki.cs.tmc.settings;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 
 * This class is used to store and load settings data in Palikka.
 * NOTE: only String and Integer types are allowed field types because of serialization/deserialization
 * @author jmturpei
 */
public class Settings {

    private String studentID;
    private String hostAddress;
    private String defaultFolder;
    private String selectedCourse;
    private Integer courseListDownloadTimeout;
    private Integer exerciseListDownloadTimeout;
    private Integer exerciseDownloadTimeout;
    private Integer exerciseUploadTimeout;

    /**
     * Stores the settings to disk.
     * @param settings
     * @param out
     * @throws Exception 
     */
    public static void Serialize(Settings settings, OutputStream out) throws Exception {
        if (settings == null) {
            throw new NullPointerException("settings was null at Settings.Serialize");
        }
        if (out == null) {
            throw new NullPointerException("output stream was null at Settings.Serialize");
        }

        try {

            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document dom = docBuilder.newDocument();


            Element root = dom.createElement("Settings");
            dom.appendChild(root);

            Field[] fields = settings.getClass().getDeclaredFields();

            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                Element elem = dom.createElement(fields[i].getName());
                Object value = fields[i].get(settings);


                if (fields[i].getType().equals(Integer.class)) {
                    elem.setAttribute("value", ((Integer) value).toString());
                } else if (fields[i].getType().equals(String.class)) {
                    if (value == null) {
                        value = "";
                    }
                    elem.setAttribute("value", (String) value);
                } else {
                    throw new Exception("unknown field type. Only String and Integer fields are allowed");
                }

                root.appendChild(elem);
            }



            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(dom);
            transformer.transform(source, result);
            String xmlString = sw.toString();

            PrintWriter writer = new PrintWriter(out);

            writer.println(xmlString);
            writer.close();
            out.close();


        } catch (Exception e) {
            throw new Exception("Failed to serialize settings. " + e.getMessage());
        }

    }

    /**
     * Loads settings from a given InputStream
     * @param xmlFile
     * @return The created Settings instance
     * @throws Exception 
     */
    public static Settings Deserialize(InputStream xmlFile) throws Exception {
        if (xmlFile == null) {
            throw new NullPointerException("xml file inputstream was null at Settings.Deserialize");
        }


        Settings settings = new Settings();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom;

        try {
            dom = db.parse(xmlFile);
        } catch (Exception e) {
            throw new Exception("couldn't parse xml settings. Invalid xml file");
        }


        Element root = dom.getDocumentElement();
        NodeList nodes = root.getChildNodes();


        Field[] fields = settings.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
        }



        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String elementName = elem.getNodeName();
                String elementValue = elem.getAttribute("value");


                for (int j = 0; j < fields.length; j++) {
                    if (fields[j].getName().equals(elementName)) {

                        if (fields[j].getType().equals(Integer.class)) {
                            fields[j].set(settings, Integer.parseInt(elementValue));
                        } else if (fields[j].getType().equals(String.class)) {
                            if (elementValue == null) {
                                elementValue = "";
                            }
                            fields[j].set(settings, elementValue);
                        } else {
                            throw new Exception("unknown field type. Only String and Integer fields are allowed");
                        }

                    }
                }
            }
        }

        xmlFile.close();

        return settings;
    }

    /**
     * Checks if the settings are valid.
     * @return TRUE if the settings are valid and FALSE if they are not.
     */
    public boolean isValid() {
        if (studentID == null || defaultFolder == null || hostAddress == null) {
            return false;
        } else if (!(new File(defaultFolder)).exists()) {
            return false;
        }
        if (studentID.length() == 0) {
            return false;
        }

        try {
            URL checkURL = new URL(hostAddress);
        } catch (MalformedURLException e) {
            return false;
        }

        return true;
    }

    /**
     * @return the studentID
     */
    public String getStudentID() {
        return studentID;
    }

    /**
     * @param studentID the studentID to set
     */
    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    /**
     * @return the hostAddress
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param hostAddress the hostAddress to set
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * @return the defaultFolder
     */
    public String getDefaultFolder() {
        return defaultFolder;
    }

    /**
     * @param defaultFolder the defaultFolder to set
     */
    public void setDefaultFolder(String defaultFolder) {
        this.defaultFolder = defaultFolder;
    }

    /**
     * @return the selectedCourse
     */
    public String getSelectedCourse() {
        return selectedCourse;
    }

    /**
     * @param selectedCourse the selectedCourse to set
     */
    public void setSelectedCourse(String selectedCourse) {
        this.selectedCourse = selectedCourse;
    }

    /**
     * @return the courseListDownloadTimeout
     */
    public Integer getCourseListDownloadTimeout() {
        return courseListDownloadTimeout;
    }

    /**
     * @param courseListDownloadTimeout the courseListDownloadTimeout to set
     */
    public void setCourseListDownloadTimeout(Integer courseListDownloadTimeout) {
        this.courseListDownloadTimeout = courseListDownloadTimeout;
    }

    /**
     * @return the exerciseListDownloadTimeout
     */
    public Integer getExerciseListDownloadTimeout() {
        return exerciseListDownloadTimeout;
    }

    /**
     * @param exerciseListDownloadTimeout the exerciseListDownloadTimeout to set
     */
    public void setExerciseListDownloadTimeout(Integer exerciseListDownloadTimeout) {
        this.exerciseListDownloadTimeout = exerciseListDownloadTimeout;
    }

    /**
     * @return the exerciseDownloadTimeout
     */
    public Integer getExerciseDownloadTimeout() {
        return exerciseDownloadTimeout;
    }

    /**
     * @param exerciseDownloadTimeout the exerciseDownloadTimeout to set
     */
    public void setExerciseDownloadTimeout(Integer exerciseDownloadTimeout) {
        this.exerciseDownloadTimeout = exerciseDownloadTimeout;
    }

    /**
     * @return the exerciseUploadTimeout
     */
    public Integer getExerciseUploadTimeout() {
        return exerciseUploadTimeout;
    }

    /**
     * @param exerciseUploadTimeout the exerciseUploadTimeout to set
     */
    public void setExerciseUploadTimeout(Integer exerciseUploadTimeout) {
        this.exerciseUploadTimeout = exerciseUploadTimeout;
    }
}
