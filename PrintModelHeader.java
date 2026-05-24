import java.io.ObjectInputStream;
import java.io.FileInputStream;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class PrintModelHeader {
    public static void main(String[] args) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("src/main/resources/ETA_modelo_predictivo.model"));
        Classifier cls = (Classifier) ois.readObject();
        // Weka model files usually contain a Classifier array or Classifier + Instances
        // if they are saved with the header.
        // Actually, Classifier alone doesn't contain the header. But Bagging might
        // contain it, or the pipeline.
        // Let's just try to read the Instances if it's there.
        try {
            Instances header = (Instances) ois.readObject();
            System.out.println("HEADER:");
            System.out.println(header.toString());
        } catch (Exception e) {
            System.out.println("No Instances header found in model file.");
        }
    }
}
