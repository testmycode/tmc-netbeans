/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data.serialization.cresultparser;

/**
 *
 * @author rase
 */
public class CTestSuite {
    private String name;
    private String points;
    
    public CTestSuite(String name) {
        this.name = name;
    }
    
    public CTestSuite(String name, String points) {
        this(name);
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
}
