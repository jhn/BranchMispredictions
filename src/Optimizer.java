import java.util.List;
import java.util.concurrent.Callable;

public class Optimizer implements Callable<String> {

    private final List<Double> selectivities;

    public Optimizer(List<Double> selectivities) {
        this.selectivities = selectivities;
    }

    public String process() {
        return "Process me! :-(";
    }

    @Override
    public String call() throws Exception {
        return process();
    }
}
