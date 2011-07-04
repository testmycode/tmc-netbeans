/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.utilities.json.updaters;

/**
 *
 * @author knordman
 */
public interface IExerciseListUpdateListener {
    void exerciseListUpdateComplete();
    void exerciseListUpdateFailed(String errorMessage);
}
